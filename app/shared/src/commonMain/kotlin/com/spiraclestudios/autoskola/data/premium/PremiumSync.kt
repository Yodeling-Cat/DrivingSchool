package com.spiraclestudios.autoskola.data.premium

import com.russhwolf.settings.Settings
import com.spiraclestudios.autoskola.premium.PremiumStatus
import com.spiraclestudios.autoskola.premium.PurchaseSource
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * What was last known about what the user owns, kept in [Settings] so the first frame after a restart
 * is right without waiting for a store.
 *
 * The store stays the source of truth ([PremiumSync] refreshes it at every launch and resume); this is
 * only what to believe until it answers - and when it cannot be reached, which is why it exists.
 */
internal object PremiumCache {
    private const val KEY = "premium_status"

    /** The status stored last, or `Free` for a user the store has never confirmed anything for. */
    fun read(settings: Settings): PremiumStatus {
        val stored = settings.getStringOrNull(KEY) ?: return PremiumStatus.Free
        val (kind, source) = stored.split(':', limit = 2).let { it[0] to it.getOrNull(1) }
        val purchaseSource = PurchaseSource.entries.firstOrNull { it.name == source } ?: PurchaseSource.Unknown
        return when (kind) {
            "Premium" -> PremiumStatus.Premium(purchaseSource)

            "AdsRemoved" -> PremiumStatus.AdsRemoved(purchaseSource)

            // Anything else, including a kind a later version wrote and this one does not know: nothing
            // is granted on a value that cannot be read.
            else -> PremiumStatus.Free
        }
    }

    fun write(settings: Settings, status: PremiumStatus) {
        when (status) {
            PremiumStatus.Free -> settings.remove(KEY)
            is PremiumStatus.Premium -> settings.putString(KEY, "Premium:${status.source.name}")
            is PremiumStatus.AdsRemoved -> settings.putString(KEY, "AdsRemoved:${status.source.name}")
        }
    }
}

/**
 * Asks the store what the user owns and tells [PremiumRepository], when it has an answer to give, and
 * buys "remove ads" through it.
 *
 * Three rules, each there because the mistake it prevents is a person who paid seeing what they paid
 * to stop:
 *
 * 1. **A store that does not answer changes nothing.** [PurchaseQuery.Unavailable] is not `Free`, and a
 *    question that takes longer than [timeoutMillis] counts as not having been answered.
 * 2. **An answer is cached** ([PremiumCache]), so the next start, offline or not, begins from it.
 * 3. **A store that says "nothing" is believed**, because that is how a refund takes a purchase back.
 *
 * Calls queue rather than overlap: [refresh] waits its turn, so a caller that needs the answer before it
 * does something - the ads, which must not start for somebody who has paid - can wait for it.
 */
class PremiumSync(
    private val billing: BillingDataSource,
    private val premium: PremiumRepository,
    private val settings: Settings,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) {
    private val turn = Mutex()

    suspend fun refresh() = turn.withLock {
        val answer = withTimeoutOrNull(timeoutMillis) { billing.query() } ?: PurchaseQuery.Unavailable
        if (answer is PurchaseQuery.Answered) record(answer.status)
    }

    /** What the store asks for "remove ads", or `null` when it does not answer in time or sells nothing here. */
    suspend fun removeAdsOffer(): ProductOffer? = withTimeoutOrNull(timeoutMillis) { billing.removeAdsOffer() }

    /**
     * Buys "remove ads" through the store and, once it is paid for, takes the ads away at once and
     * caches it, so the purchase does not wait for the next refresh to count.
     *
     * Runs to the end even when the caller is canceled - a screen closed while the user is still in
     * the store's sheet - because the user may be paying, and a purchase that is paid for and not
     * recorded is exactly the mistake this class is here to prevent. The store's sheet is not inside
     * the lock, so a refresh on resume (which the sheet closing causes) does not wait on the user.
     */
    suspend fun buyRemoveAds(): PurchaseResult = withContext(NonCancellable) {
        val result = billing.buyRemoveAds()
        when (result) {
            is PurchaseResult.Purchased -> turn.withLock { record(result.status) }

            // Owned already, on this account: what it owns is the store's to say.
            PurchaseResult.AlreadyOwned -> refresh()

            PurchaseResult.Pending, PurchaseResult.Canceled, PurchaseResult.Failed -> Unit
        }
        result
    }

    private fun record(status: PremiumStatus) {
        PremiumCache.write(settings, status)
        premium.update(status)
    }

    companion object {
        /** Long enough for a slow phone to reach Play, short enough that nothing waits on a store that is not there. */
        const val DEFAULT_TIMEOUT_MILLIS = 10_000L
    }
}

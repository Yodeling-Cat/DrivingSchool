package com.spiraclestudios.autoskola.data.ads

import com.russhwolf.settings.Settings
import com.spiraclestudios.autoskola.data.preferences.ConsentAge
import com.spiraclestudios.autoskola.data.preferences.Preferences
import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository
import com.spiraclestudios.autoskola.data.premium.PremiumRepository
import com.spiraclestudios.autoskola.premium.PremiumFeature
import com.spiraclestudios.autoskola.premium.PremiumRules
import com.spiraclestudios.autoskola.premium.PremiumStatus
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Whether an ad may be asked for at all, and when one is due.
 *
 * Three questions, and every request goes through all three in this order:
 *
 * 1. **Consent** ([PreferencesRepository]) - the user has agreed to ads. Off until they say so.
 * 2. **The entitlement** ([PremiumRepository]) - nobody who has paid for the ads to stop gets one.
 * 3. **The frequency** ([AdRules]) - every tenth practice test, and never more often than that.
 *
 * Consent and the entitlement are read at the moment of each request rather than remembered from
 * when something was loaded, which is why [showTestOfferAd] throws away a preloaded ad it turns out
 * it may no longer show. A purchase arrives mid-session, and what was bought is the absence of
 * exactly that.
 */
class AdRepository(
    private val dataSource: AdDataSource,
    private val preferences: PreferencesRepository,
    private val premium: PremiumRepository,
    private val settings: Settings,
) {

    /**
     * The practice-test count the offer was last served at, or `null` if it never was. What the
     * count alone cannot say: a user who watched the ad and whose test was never recorded is still
     * at the count that asked, and would be asked again for an ad they have already sat through.
     */
    private var servedAt: Int? = settings.getIntOrNull(KEY_OFFER_SERVED_AT)

    private val writes = Mutex()

    /**
     * Whether the app may request an ad, as it changes.
     *
     * What a banner placement follows: both of the answers it depends on change while a screen is
     * open - consent can be withdrawn in Settings, and Premium can be bought - and a placement drawn
     * from a snapshot would keep showing an ad the user had just switched off.
     */
    val adsAllowed: Flow<Boolean> =
        combine(preferences.preferences, premium.entitlement) { preferences, entitlement ->
            allowed(preferences, entitlement)
        }.distinctUntilChanged()

    /**
     * The same answer, read once: what the request paths below ask before asking the SDK anything.
     *
     * Deliberately separate from [adsAllowed] rather than `adsAllowed.value`, because this is the
     * question a caller that is about to act needs answered now, and the flow is the one a screen
     * drawing itself needs to follow.
     */
    fun mayRequestAds(): Boolean = allowed(preferences.preferences.value, premium.entitlement.value)

    /**
     * The one place the four conditions are put together: the target can serve ads, the user has
     * agreed to them, their age answer lets them have agreed at all, and nobody has paid for them to
     * stop. The age answer is asked here as well as at the switches, because a value stored before the
     * app asked about age must not be able to send anything on its own ([ConsentAge.effective]).
     */
    private fun allowed(preferences: Preferences, entitlement: PremiumStatus): Boolean {
        val agreed = ConsentAge.effective(
            preferences.adsEnabled,
            preferences.overConsentAge,
            preferences.parentalConsent,
        )
        val paid = PremiumRules.isUnlocked(PremiumFeature.NoAds, entitlement)
        return dataSource.available && agreed && !paid
    }

    init {
        // The SDK has to be told before it is asked for anything, and a banner asks it directly
        // without coming through this class, so this cannot wait for the first request.
        pushAgeGate()
    }

    /**
     * Tells the ad SDK whether this user is under the age of consent, which Google requires in place
     * before a request is made for such a user. This class is the only thing that knows the answer, so
     * it is pushed from here: once when the repository is built, before every request, and from the
     * screens that change the answer, which is what keeps a banner right.
     */
    fun pushAgeGate() {
        dataSource.setUnderAgeOfConsent(ConsentAge.underAgeOfConsent(preferences.preferences.value.overConsentAge))
    }

    /**
     * Whether the offer is due before the next test: every tenth one, unless it has already been
     * served at this very count.
     */
    fun shouldOfferTest(practiceTestsStarted: Int): Boolean =
        mayRequestAds() && AdRules.offerDue(practiceTestsStarted) && servedAt != practiceTestsStarted

    /**
     * Asks for the offer's ad ahead of the user tapping, so the dialog's button does not wait on a
     * load. *When* to call this is a screen's decision; whether it does anything is not.
     */
    suspend fun preloadTestOffer() {
        pushAgeGate()
        if (mayRequestAds()) dataSource.preload(AdSlot.TestOfferInterstitial)
    }

    /**
     * Shows the offer's ad.
     *
     * The offer is spent - and the user let through - whether an ad turned up or not, because what
     * the user experiences is the same either way: they waited, and now they are on their way. Not
     * calling this at all is what dismissing the dialog means, and that leaves the offer due, so
     * tapping again asks again.
     */
    suspend fun showTestOfferAd(practiceTestsStarted: Int): AdResult {
        pushAgeGate()
        if (!mayRequestAds()) {
            dataSource.discard()
            return AdResult.Unavailable
        }
        // A second tap while the first ad is up, or a count that has already been served.
        if (!shouldOfferTest(practiceTestsStarted)) return AdResult.NotDue

        val result = dataSource.show(AdSlot.TestOfferInterstitial)
        if (result == AdResult.Shown || result == AdResult.NoFill) markServed(practiceTestsStarted)
        return result
    }

    /**
     * Shows the ad once a result screen is done with, unless the user's next tap is the gated one
     * (see [AdRules.closingAdYields]) - one fullscreen ad per trip through the app, and the gate is
     * the one worth spending, because it is the one that can be bought out of.
     */
    suspend fun showClosingAd(practiceTestsStarted: Int): AdResult {
        pushAgeGate()
        if (!mayRequestAds()) {
            dataSource.discard()
            return AdResult.Unavailable
        }
        if (AdRules.closingAdYields(practiceTestsStarted)) return AdResult.NotDue
        return dataSource.show(AdSlot.ResultsInterstitial)
    }

    /**
     * Records that the offer has been served at [practiceTestsStarted]. Written to the end even if
     * the screen that asked for the ad goes away, and one at a time: a marker that did not survive
     * would mean asking a user to watch the same ad twice.
     */
    private suspend fun markServed(practiceTestsStarted: Int) = withContext(NonCancellable) {
        writes.withLock {
            settings.putInt(KEY_OFFER_SERVED_AT, practiceTestsStarted)
            servedAt = practiceTestsStarted
        }
    }

    private companion object {
        const val KEY_OFFER_SERVED_AT = "ads_offer_served_at"
    }
}

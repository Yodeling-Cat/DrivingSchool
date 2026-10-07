package com.spiraclestudios.autoskola.data.premium

import com.spiraclestudios.autoskola.premium.PremiumStatus

/**
 * The id of the one-time "remove ads" product that Google Play already sells for this app.
 *
 * It exists because the app sold it before it had ads of its own to remove, and anyone who bought it
 * paid for exactly what the product says: the ads do not come back (`plans/premium.md`, "Someone has
 * already bought remove ads"). The id is the store's, so it cannot be renamed here.
 */
const val PRODUCT_REMOVE_ADS = "remove_ads"

/**
 * The store, asked what the user already owns.
 *
 * **Restore only, for now.** There is no purchase here and no price: the app asks, and a person who
 * bought the old "remove ads" product is recognized. Selling something new is the rest of
 * `plans/premium.md`, and it is a second method on this interface rather than a different one.
 *
 * One implementation per platform behind [platformBillingDataSource]. What to do with the answer, and
 * when not to believe it, is [PremiumSync]'s; this is the store.
 */
interface BillingDataSource {

    /**
     * Asks the store what this user owns. Suspends while it connects, which can take a while on a
     * slow phone, so a caller that must not wait forever puts a time limit on it ([PremiumSync] does).
     */
    suspend fun query(): PurchaseQuery
}

/** What a question to the store came back with. */
sealed interface PurchaseQuery {

    /**
     * The store answered, and [status] is what it said the user owns - including `Free`, which is an
     * answer: a refund takes a purchase back by arriving as one.
     */
    data class Answered(val status: PremiumStatus) : PurchaseQuery

    /**
     * The store could not be asked, or did not answer: no connection, no Play Store on the device, a
     * build the store does not know, a target with no store at all.
     *
     * **This is not `Free`.** Believing it would take a purchase away from somebody whose phone is in a
     * tunnel, which is the one mistake this slice must not make, so it changes nothing and whatever
     * was last known stands.
     */
    data object Unavailable : PurchaseQuery
}

/** A target with no store to ask: it never answers, so nothing is ever granted or taken away. */
class UnavailableBillingDataSource : BillingDataSource {
    override suspend fun query(): PurchaseQuery = PurchaseQuery.Unavailable
}

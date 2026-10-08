package com.spiraclestudios.autoskola.data.premium

import com.spiraclestudios.autoskola.premium.PremiumStatus

/**
 * The id of the one-time "remove ads" product that Google Play sells for this app, from Settings. The
 * id is the store's, so it cannot be renamed here.
 *
 * The old app sold the same thing as `remove_ads`. It is not recognized: nobody still uses that
 * purchase (decided 2026-10-08, `plans/premium.md`).
 */
const val PRODUCT_REMOVE_ADS = "remove_all_ads"

/** Whether a purchase of [products] takes the ads away. */
fun removesAds(products: List<String>): Boolean = PRODUCT_REMOVE_ADS in products

/**
 * The store: what the user already owns, and the one thing it sells, "remove ads" ([PRODUCT_REMOVE_ADS]).
 *
 * Premium is the rest of `plans/premium.md`, and it is more methods on this interface rather than a
 * different one.
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

    /**
     * What the store asks for "remove ads" from this user, or `null` when it does not sell it here: no
     * store, no connection, or a product the store does not offer this account. Suspends like [query].
     */
    suspend fun removeAdsOffer(): ProductOffer?

    /**
     * Opens the store's own purchase sheet for "remove ads" and suspends until the user is done with it,
     * which can be minutes. A completed purchase is acknowledged by the store's implementation before
     * this returns, because Play refunds one that is not within three days.
     */
    suspend fun buyRemoveAds(): PurchaseResult
}

/**
 * A product as the store sells it to this user. [price] is the store's own string, in the user's
 * currency and format ("0,99 €"), so no price is ever written in the app (`plans/premium.md`, "Price").
 */
data class ProductOffer(val price: String)

/** How a purchase ended. */
sealed interface PurchaseResult {

    /** Paid for and acknowledged: [status] is what the user owns now. */
    data class Purchased(val status: PremiumStatus) : PurchaseResult

    /**
     * Started but not paid yet - cash at a shop, a slow bank. Nothing is granted: the store reports the
     * purchase as completed once it is, and the next refresh picks it up.
     */
    data object Pending : PurchaseResult

    /**
     * The store will not sell it again because this account owns it already - bought on another phone
     * and not seen here yet. Not an error: the store is asked what is owned, and that is believed.
     */
    data object AlreadyOwned : PurchaseResult

    /** The user closed the sheet. Nothing to say about it. */
    data object Canceled : PurchaseResult

    /** The store refused or could not be reached. Nothing was bought, and the user can try again. */
    data object Failed : PurchaseResult
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

/**
 * A target with no store to ask: it never answers, so nothing is ever granted or taken away, and it
 * sells nothing, so no screen offers what cannot be bought.
 */
class UnavailableBillingDataSource : BillingDataSource {
    override suspend fun query(): PurchaseQuery = PurchaseQuery.Unavailable

    override suspend fun removeAdsOffer(): ProductOffer? = null

    override suspend fun buyRemoveAds(): PurchaseResult = PurchaseResult.Failed
}

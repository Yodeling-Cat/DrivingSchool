package com.spiraclestudios.autoskola.data.premium

import com.spiraclestudios.autoskola.premium.PremiumStatus
import com.spiraclestudios.autoskola.premium.PurchaseSource
import kotlinx.coroutines.delay

/**
 * A store a test can script: what it answers the next time it is asked, how long it takes, and a
 * record of how often it was asked.
 *
 * It starts by answering "nothing bought", which is what a user who has never paid gets from the real
 * one. A test about a store that cannot be reached says [PurchaseQuery.Unavailable].
 *
 * It sells "remove ads" at [offer] and a purchase ends in [purchase]: bought, unless a test says the
 * user canceled, the payment is pending or the store failed.
 */
class FakeBillingDataSource(
    var answer: PurchaseQuery = PurchaseQuery.Answered(PremiumStatus.Free),
    /** How long the store takes to answer, in the test's virtual time. */
    var delayMillis: Long = 0,
    var offer: ProductOffer? = ProductOffer(price = "€0.99"),
    var purchase: PurchaseResult = PurchaseResult.Purchased(PremiumStatus.AdsRemoved(PurchaseSource.Play)),
    /** How long the user spends in the store's purchase sheet, in the test's virtual time. */
    var purchaseDelayMillis: Long = 0,
) : BillingDataSource {

    /** How many times the store was asked. */
    var queries = 0
        private set

    /** How many purchases were started. */
    var purchases = 0
        private set

    override suspend fun query(): PurchaseQuery {
        queries++
        if (delayMillis > 0) delay(delayMillis)
        return answer
    }

    override suspend fun removeAdsOffer(): ProductOffer? {
        if (delayMillis > 0) delay(delayMillis)
        return offer
    }

    override suspend fun buyRemoveAds(): PurchaseResult {
        purchases++
        if (purchaseDelayMillis > 0) delay(purchaseDelayMillis)
        return purchase
    }
}

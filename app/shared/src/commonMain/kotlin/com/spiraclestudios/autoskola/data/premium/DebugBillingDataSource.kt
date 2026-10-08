package com.spiraclestudios.autoskola.data.premium

import com.spiraclestudios.autoskola.premium.PremiumStatus
import com.spiraclestudios.autoskola.premium.PurchaseSource

/**
 * A pretend store for debug builds, so the "remove ads" row in Settings can be looked at in both of
 * its states. Play sells nothing to a build that did not come through it, so without this a `.dev`
 * build never shows the row at all.
 *
 * It always has "remove ads" for sale, and a tap "buys" it at once - no sheet, no payment. What was
 * bought lives only in this object, so every launch starts unbought: the launch refresh hears
 * "nothing" and takes back what the cache remembered from last time, the same way a refund would.
 *
 * The status it grants comes from [PurchaseSource.Unknown], which is what that source is for: a
 * grant no store made.
 */
class DebugBillingDataSource : BillingDataSource {

    private var status: PremiumStatus = PremiumStatus.Free

    override suspend fun query(): PurchaseQuery = PurchaseQuery.Answered(status)

    override suspend fun removeAdsOffer(): ProductOffer = ProductOffer(price = "€0.99")

    override suspend fun buyRemoveAds(): PurchaseResult {
        status = PremiumStatus.AdsRemoved(PurchaseSource.Unknown)
        return PurchaseResult.Purchased(status)
    }
}

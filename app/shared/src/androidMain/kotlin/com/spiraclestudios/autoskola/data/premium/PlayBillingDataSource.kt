package com.spiraclestudios.autoskola.data.premium

import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.spiraclestudios.autoskola.currentActivity
import com.spiraclestudios.autoskola.premium.PremiumStatus
import com.spiraclestudios.autoskola.premium.PurchaseSource
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Google Play Billing: what this user has bought, and the purchase of "remove ads" ([PRODUCT_REMOVE_ADS]),
 * the one-time product the Play listing sells.
 *
 * **Every completed purchase is acknowledged**, whichever way it arrives: from the sheet this opened, or
 * found by [query] - a pending payment that completed while the app was closed, or a purchase made
 * outside the app with a promo code. Play refunds a purchase that is not acknowledged within three days,
 * so the user would lose what they paid for without being told. The purchases from the old app were
 * acknowledged long ago, and acknowledging is a no-op Play skips for those.
 *
 * It cannot be tried from a debug build installed straight onto a phone: Play only knows builds that
 * came through it, so there the question comes back empty and nothing is for sale. It has to be tried
 * from an internal test track with a license tester account (`plans/premium.md`).
 */
class PlayBillingDataSource(context: Context) : BillingDataSource {

    /**
     * The purchase sheet that is open and waiting for Play to say how it ended, if one is. Play answers
     * on the main thread, through [onPurchasesUpdated].
     */
    @Volatile
    private var waiting: CancellableContinuation<PurchaseResult>? = null

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener { result, purchases -> onPurchasesUpdated(result, purchases.orEmpty()) }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    override suspend fun query(): PurchaseQuery {
        if (!connect()) return PurchaseQuery.Unavailable
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        val purchases = suspendCancellableCoroutine<List<Purchase>?> { continuation ->
            client.queryPurchasesAsync(params) { result, purchases ->
                if (!continuation.isActive) return@queryPurchasesAsync
                // Not OK is not "owns nothing": the store did not answer.
                continuation.resume(purchases.takeIf { result.responseCode == BillingClient.BillingResponseCode.OK })
            }
        } ?: return PurchaseQuery.Unavailable
        // A purchase that completed while nobody was watching is acknowledged here, or Play refunds it.
        purchases.filter { it.isRemoveAds() }.forEach { acknowledge(it) }
        return PurchaseQuery.Answered(statusOf(purchases))
    }

    override suspend fun removeAdsOffer(): ProductOffer? {
        val details = productDetails() ?: return null
        return offerOf(details)?.formattedPrice?.let(::ProductOffer)
    }

    override suspend fun buyRemoveAds(): PurchaseResult {
        val details = productDetails() ?: return PurchaseResult.Failed
        // Play's sheet is drawn over an activity, so there is nothing to buy from while none is in front.
        val activity = currentActivity() ?: return PurchaseResult.Failed
        val product = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            // A product with more than one purchase option has to be told which; the first is the one on sale.
            .apply { offerOf(details)?.offerToken?.let(::setOfferToken) }
            .build()
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(product)).build()
        val result = suspendCancellableCoroutine { continuation ->
            waiting = continuation
            continuation.invokeOnCancellation { if (waiting === continuation) waiting = null }
            val launched = client.launchBillingFlow(activity, params)
            if (launched.responseCode != BillingClient.BillingResponseCode.OK) {
                // The sheet did not open, so the listener will not hear about it: answer from here.
                waiting = null
                continuation.resume(failureOf(launched))
            }
        }
        return result
    }

    /**
     * Play's answer to the sheet. A purchase can also arrive here without one - a pending payment that
     * completes while the app is open - and it is acknowledged either way; the entitlement follows at
     * the next refresh, which is the next resume.
     */
    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>) {
        val continuation = waiting
        waiting = null
        val bought = purchases.filter { it.isRemoveAds() }
        val outcome = when {
            result.responseCode != BillingClient.BillingResponseCode.OK -> failureOf(result)

            bought.any { it.purchaseState == Purchase.PurchaseState.PURCHASED } ->
                PurchaseResult.Purchased(PremiumStatus.AdsRemoved(PurchaseSource.Play))

            bought.any { it.purchaseState == Purchase.PurchaseState.PENDING } -> PurchaseResult.Pending

            else -> PurchaseResult.Failed
        }
        if (outcome is PurchaseResult.Purchased) {
            // Acknowledged before the sheet's caller is answered, so "bought" is never reported for a
            // purchase Play could still take back. A refusal is retried by the next query.
            client.acknowledgePurchase(acknowledgement(bought.first { it.isPurchased() })) { _ ->
                continuation?.takeIf { it.isActive }?.resume(outcome)
            }
        } else {
            continuation?.takeIf { it.isActive }?.resume(outcome)
        }
    }

    private fun failureOf(result: BillingResult): PurchaseResult = when (result.responseCode) {
        BillingClient.BillingResponseCode.USER_CANCELED -> PurchaseResult.Canceled
        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> PurchaseResult.AlreadyOwned
        else -> PurchaseResult.Failed
    }

    /** Acknowledges [purchase] if it is completed and not acknowledged yet; there is nothing to wait for. */
    private fun acknowledge(purchase: Purchase) {
        if (!purchase.isPurchased() || purchase.isAcknowledged) return
        client.acknowledgePurchase(acknowledgement(purchase)) { _ -> }
    }

    private fun acknowledgement(purchase: Purchase) =
        AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()

    /** "Remove ads" as Play sells it to this user, or `null` when Play does not answer or does not sell it. */
    private suspend fun productDetails(): ProductDetails? {
        if (!connect()) return null
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_REMOVE_ADS)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
        return suspendCancellableCoroutine { continuation ->
            client.queryProductDetailsAsync(params) { result, details ->
                if (!continuation.isActive) return@queryProductDetailsAsync
                continuation.resume(
                    details.productDetailsList
                        .firstOrNull { it.productId == PRODUCT_REMOVE_ADS }
                        .takeIf { result.responseCode == BillingClient.BillingResponseCode.OK },
                )
            }
        }
    }

    /** The purchase option on sale: the listing has one (`legacy-base`), and the first stands in if it gains more. */
    private fun offerOf(details: ProductDetails): ProductDetails.OneTimePurchaseOfferDetails? =
        details.oneTimePurchaseOfferDetailsList?.firstOrNull() ?: details.oneTimePurchaseOfferDetails

    /**
     * What the purchases add up to. Only a purchase that has completed counts - a pending one (a
     * payment the user has not finished, a slow bank) has not been paid for, and the user is not
     * handed what it would buy until it has.
     */
    private fun statusOf(purchases: List<Purchase>): PremiumStatus {
        val bought = purchases.any { it.isRemoveAds() && it.isPurchased() }
        return if (bought) PremiumStatus.AdsRemoved(PurchaseSource.Play) else PremiumStatus.Free
    }

    private fun Purchase.isRemoveAds() = PRODUCT_REMOVE_ADS in products

    private fun Purchase.isPurchased() = purchaseState == Purchase.PurchaseState.PURCHASED

    /** Whether the store connection is up, starting it if it is not. */
    private suspend fun connect(): Boolean {
        if (client.isReady) return true
        return suspendCancellableCoroutine { continuation ->
            client.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (continuation.isActive) {
                            continuation.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                        }
                    }

                    // The client reconnects by itself (`enableAutoServiceReconnection`), and the next
                    // query finds it ready or starts again; there is nothing to do here.
                    override fun onBillingServiceDisconnected() = Unit
                },
            )
        }
    }
}

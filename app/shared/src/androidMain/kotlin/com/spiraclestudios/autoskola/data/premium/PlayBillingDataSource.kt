package com.spiraclestudios.autoskola.data.premium

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
import com.spiraclestudios.autoskola.premium.PremiumStatus
import com.spiraclestudios.autoskola.premium.PurchaseSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Google Play Billing, asked one question: what has this user bought?
 *
 * **There is no purchase flow here**, so no purchase can start and no purchase listener has anything
 * to hear; the listener Play requires is a stub. What this does is recognize the one-time "remove ads"
 * product that was sold before this rewrite ([PRODUCT_REMOVE_ADS]).
 *
 * It never acknowledges a purchase. Play refunds a purchase that is not acknowledged within three
 * days, so anything bought in the old app already was, and acknowledging is the job of the code that
 * sells something.
 *
 * It cannot be tried from a debug build installed straight onto a phone: Play only knows builds that
 * came through it, so there the question comes back empty. It has to be tried from an internal test
 * track with an account that owns the product (`plans/premium.md`).
 */
class PlayBillingDataSource(context: Context) : BillingDataSource {

    private val client: BillingClient = BillingClient.newBuilder(context)
        // Required, and nothing is sold from the app yet, so there is nothing to be told.
        .setListener { _, _ -> }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    override suspend fun query(): PurchaseQuery {
        if (!connect()) return PurchaseQuery.Unavailable
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        return suspendCancellableCoroutine { continuation ->
            client.queryPurchasesAsync(params) { result, purchases ->
                if (!continuation.isActive) return@queryPurchasesAsync
                continuation.resume(
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        PurchaseQuery.Answered(statusOf(purchases))
                    } else {
                        // Not OK is not "owns nothing": the store did not answer.
                        PurchaseQuery.Unavailable
                    },
                )
            }
        }
    }

    /**
     * What the purchases add up to. Only a purchase that has completed counts - a pending one (a
     * payment the user has not finished, a slow bank) has not been paid for, and the user is not
     * handed what it would buy until it has.
     */
    private fun statusOf(purchases: List<Purchase>): PremiumStatus {
        val bought = purchases.any {
            it.purchaseState == Purchase.PurchaseState.PURCHASED && PRODUCT_REMOVE_ADS in it.products
        }
        return if (bought) PremiumStatus.AdsRemoved(PurchaseSource.Play) else PremiumStatus.Free
    }

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

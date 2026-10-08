package com.spiraclestudios.autoskola.data.premium

import com.spiraclestudios.autoskola.premium.PremiumStatus
import com.spiraclestudios.autoskola.premium.PurchaseSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * The pretend store a debug build talks to, so the "remove ads" row can be looked at in both of its
 * states without Play, which sells nothing to a build that did not come through it.
 */
class DebugBillingDataSourceTest {

    private val bought = PremiumStatus.AdsRemoved(PurchaseSource.Unknown)

    @Test
    fun itAlwaysHasRemoveAdsForSale() = runTest {
        assertNotNull(DebugBillingDataSource().removeAdsOffer())
    }

    @Test
    fun nothingIsBoughtAtLaunch() = runTest {
        assertEquals(PurchaseQuery.Answered(PremiumStatus.Free), DebugBillingDataSource().query())
    }

    @Test
    fun aTapBuysItAndTheStoreThenSaysSo() = runTest {
        val store = DebugBillingDataSource()

        assertEquals(PurchaseResult.Purchased(bought), store.buyRemoveAds())

        // What a resume's refresh hears, so the purchase is not taken back while the app is running.
        assertEquals(PurchaseQuery.Answered(bought), store.query())
    }

    @Test
    fun theNextLaunchStartsUnboughtAgain() = runTest {
        DebugBillingDataSource().buyRemoveAds()

        // Nothing is kept: a new process builds a new store, and the launch refresh hears "nothing".
        assertEquals(PurchaseQuery.Answered(PremiumStatus.Free), DebugBillingDataSource().query())
    }

    @Test
    fun theWholeFlowThroughTheSyncShowsBothStates() = runTest {
        val settings = com.russhwolf.settings.MapSettings()
        val premium = PremiumRepository(PremiumCache.read(settings))
        val sync = PremiumSync(DebugBillingDataSource(), premium, settings)

        sync.refresh()
        assertEquals(PremiumStatus.Free, premium.entitlement.value)

        sync.buyRemoveAds()
        assertEquals(bought, premium.entitlement.value)

        // Relaunched: the cache says bought for a moment, and the launch refresh puts it back to Free.
        val relaunched = PremiumRepository(PremiumCache.read(settings))
        PremiumSync(DebugBillingDataSource(), relaunched, settings).refresh()
        assertEquals(PremiumStatus.Free, relaunched.entitlement.value)
    }
}

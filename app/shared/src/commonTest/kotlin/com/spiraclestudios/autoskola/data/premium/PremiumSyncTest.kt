package com.spiraclestudios.autoskola.data.premium

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import com.spiraclestudios.autoskola.premium.PremiumFeature
import com.spiraclestudios.autoskola.premium.PremiumStatus
import com.spiraclestudios.autoskola.premium.PurchaseSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What the app believes the user owns, and above all when it does not believe the store. Every test
 * here is about the same mistake: somebody who paid for the ads to stop seeing them anyway.
 */
class PremiumSyncTest {

    private val adsRemoved = PremiumStatus.AdsRemoved(PurchaseSource.Play)

    private class Fixture(
        val billing: FakeBillingDataSource = FakeBillingDataSource(),
        val settings: Settings = MapSettings(),
        timeoutMillis: Long = 1_000,
    ) {
        val premium = PremiumRepository(PremiumCache.read(settings))
        val sync = PremiumSync(billing, premium, settings, timeoutMillis)

        /** The app restarted: same storage, fresh repository, and the store not asked yet. */
        fun restarted() = Fixture(FakeBillingDataSource(PurchaseQuery.Unavailable), settings)
    }

    @Test
    fun aUserWhoBoughtRemoveAdsHasTheAdsTakenAway() = runTest {
        val fixture = Fixture(FakeBillingDataSource(PurchaseQuery.Answered(adsRemoved)))
        assertEquals(PremiumStatus.Free, fixture.premium.entitlement.value)

        fixture.sync.refresh()

        assertEquals(adsRemoved, fixture.premium.entitlement.value)
        assertTrue(fixture.premium.isUnlocked(PremiumFeature.NoAds))
        // And nothing else: they paid for the ads to go, not for what Premium adds.
        assertFalse(fixture.premium.isUnlocked(PremiumFeature.AllLicenseGroups))
        assertFalse(fixture.premium.isUnlocked(PremiumFeature.LearnMaterial))
    }

    @Test
    fun theAnswerIsRememberedSoTheNextStartIsRightBeforeTheStoreIsAsked() = runTest {
        val fixture = Fixture(FakeBillingDataSource(PurchaseQuery.Answered(adsRemoved)))
        fixture.sync.refresh()

        val restarted = fixture.restarted()

        // The first frame already knows, with no store in sight.
        assertEquals(adsRemoved, restarted.premium.entitlement.value)
    }

    @Test
    fun aStoreThatCannotBeReachedTakesNothingAway() = runTest {
        val fixture = Fixture(FakeBillingDataSource(PurchaseQuery.Answered(adsRemoved)))
        fixture.sync.refresh()

        // On a phone in a tunnel, or with no Play Store: not "owns nothing".
        val offline = fixture.restarted()
        offline.sync.refresh()

        assertEquals(adsRemoved, offline.premium.entitlement.value)
    }

    @Test
    fun aStoreThatTakesTooLongCountsAsNotHavingAnswered() = runTest {
        val billing = FakeBillingDataSource(PurchaseQuery.Answered(PremiumStatus.Free), delayMillis = 60_000)
        val settings = MapSettings()
        PremiumCache.write(settings, adsRemoved)
        val fixture = Fixture(billing, settings, timeoutMillis = 1_000)

        fixture.sync.refresh()

        // Believing a "nothing" that arrived a minute late, or never, would take the purchase away.
        assertEquals(adsRemoved, fixture.premium.entitlement.value)
    }

    @Test
    fun aRefundTakesThePurchaseBackBecauseTheStoreSaysNothingIsOwnedAnymore() = runTest {
        val fixture = Fixture(FakeBillingDataSource(PurchaseQuery.Answered(adsRemoved)))
        fixture.sync.refresh()

        fixture.billing.answer = PurchaseQuery.Answered(PremiumStatus.Free)
        fixture.sync.refresh()

        assertEquals(PremiumStatus.Free, fixture.premium.entitlement.value)
        // And the next start does not bring it back from the cache.
        assertEquals(PremiumStatus.Free, fixture.restarted().premium.entitlement.value)
    }

    @Test
    fun aUserWhoNeverBoughtAnythingStaysFree() = runTest {
        val fixture = Fixture()

        fixture.sync.refresh()

        assertEquals(PremiumStatus.Free, fixture.premium.entitlement.value)
        assertFalse(fixture.premium.isUnlocked(PremiumFeature.NoAds))
    }

    @Test
    fun aCachedValueThatCannotBeReadGrantsNothing() = runTest {
        // A kind a later version wrote, or a damaged value: nothing is granted on what cannot be read.
        val settings = MapSettings("premium_status" to "Platinum:Play")

        assertEquals(PremiumStatus.Free, PremiumCache.read(settings))
    }

    @Test
    fun aCachedSourceThatIsNotKnownIsStillTheSamePurchase() = runTest {
        val settings = MapSettings("premium_status" to "AdsRemoved:Carrier")

        assertEquals(PremiumStatus.AdsRemoved(PurchaseSource.Unknown), PremiumCache.read(settings))
    }

    @Test
    fun everyKindOfStatusSurvivesAWriteAndARead() = runTest {
        val statuses = listOf(
            PremiumStatus.Free,
            PremiumStatus.Premium(PurchaseSource.Play),
            PremiumStatus.Premium(PurchaseSource.AppStore),
            PremiumStatus.AdsRemoved(PurchaseSource.Play),
        )
        for (status in statuses) {
            val settings = MapSettings()
            PremiumCache.write(settings, status)
            assertEquals(status, PremiumCache.read(settings), "$status")
        }
    }

    @Test
    fun theTargetsWithNoStoreNeverGrantOrTakeAnything() = runTest {
        val fixture = Fixture(FakeBillingDataSource(PurchaseQuery.Unavailable))

        fixture.sync.refresh()

        assertEquals(PremiumStatus.Free, fixture.premium.entitlement.value)
        assertEquals(PurchaseQuery.Unavailable, UnavailableBillingDataSource().query())
    }
}

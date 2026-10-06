package com.spiraclestudios.autoskola.data.ads

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository
import com.spiraclestudios.autoskola.data.premium.PremiumRepository
import com.spiraclestudios.autoskola.premium.PremiumStatus
import com.spiraclestudios.autoskola.premium.PurchaseSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The gate on asking for an ad at all: consent first, then the entitlement, then the frequency.
 *
 * All of it is settled here rather than on a device, because these are the rules whose mistakes show
 * up as a policy problem or a chargeback rather than as a crash: an app that asks before it was
 * allowed to, or that keeps asking a user who has paid to make it stop.
 *
 * Every test drives the real [AdRepository] over [FakeAdDataSource] and a real [PremiumRepository],
 * never the dud flag, so what is verified is the wiring the app will actually run.
 */
class AdRepositoryTest {

    private class Fixture(
        val dataSource: FakeAdDataSource,
        val preferences: PreferencesRepository,
        val entitlement: PremiumRepository,
        val settings: Settings,
    ) {
        val ads = AdRepository(dataSource, preferences, entitlement, settings)

        /** The app restarted: same storage, same store, fresh repository. */
        fun restarted() = AdRepository(dataSource, PreferencesRepository(settings), entitlement, settings)
    }

    private fun fixture(
        available: Boolean = true,
        result: AdResult = AdResult.Shown,
        adsEnabled: Boolean = true,
        premium: PremiumStatus = PremiumStatus.Free,
        settings: Settings = MapSettings(),
        // 16 or older unless a test says otherwise: consent, the entitlement and the frequency are all
        // settled by an adult, and the age rule itself is in `ConsentAgeTest`. The age is written
        // before the ads answer, so a test can build the state a version 2 install arrives in — the
        // switch stored as yes, and no question ever asked.
        overConsentAge: Boolean? = true,
        parentalConsent: Boolean = false,
    ) = Fixture(
        dataSource = FakeAdDataSource(available = available, result = result),
        preferences = PreferencesRepository(settings).apply {
            setParentalConsent(parentalConsent)
            setOverConsentAge(overConsentAge)
            setAdsEnabled(adsEnabled)
        },
        entitlement = PremiumRepository(premium),
        settings = settings,
    )

    // --- consent ------------------------------------------------------------

    @Test
    fun nothingIsAskedForWithoutConsent() = runTest {
        val fixture = fixture(adsEnabled = false)

        assertFalse(fixture.ads.mayRequestAds())
        assertFalse(fixture.ads.shouldOfferTest(9))
        assertEquals(AdResult.Unavailable, fixture.ads.showTestOfferAd(9))
        assertEquals(emptyList(), fixture.dataSource.shown)
    }

    @Test
    fun consentWithdrawnMidSessionStopsTheVeryNextRequest() = runTest {
        val fixture = fixture()

        assertTrue(fixture.ads.mayRequestAds())
        fixture.preferences.setAdsEnabled(false)

        assertEquals(AdResult.Unavailable, fixture.ads.showTestOfferAd(9))
        assertFalse(fixture.ads.shouldOfferTest(19))
        assertEquals(emptyList(), fixture.dataSource.shown)
    }

    @Test
    fun aTargetThatCannotServeAdsIsAskedForNothing() = runTest {
        val fixture = fixture(available = false)

        assertFalse(fixture.ads.mayRequestAds())
        assertEquals(AdResult.Unavailable, fixture.ads.showTestOfferAd(9))
        assertEquals(emptyList(), fixture.dataSource.preloaded)
    }

    // --- entitlement --------------------------------------------------------

    @Test
    fun premiumTurnsTheAdsOffSoTheOfferNeverAppears() = runTest {
        val fixture = fixture(premium = PremiumStatus.Premium(PurchaseSource.Play))

        assertFalse(fixture.ads.mayRequestAds())
        assertFalse(fixture.ads.shouldOfferTest(9))
        assertEquals(AdResult.Unavailable, fixture.ads.showTestOfferAd(9))
        assertEquals(emptyList(), fixture.dataSource.shown)
    }

    @Test
    fun theEntitlementArrivingMidSessionDropsWhatWasAlreadyLoaded() = runTest {
        val fixture = fixture()
        fixture.ads.preloadTestOffer()
        assertEquals(AdSlot.TestOfferInterstitial, fixture.dataSource.loaded)

        // Bought while an ad was sitting loaded and ready. It must not be shown: what was paid for
        // is the absence of exactly this.
        fixture.entitlement.update(PremiumStatus.Premium(PurchaseSource.Play))

        assertEquals(AdResult.Unavailable, fixture.ads.showTestOfferAd(9))
        assertEquals(emptyList(), fixture.dataSource.shown)
        assertNull(fixture.dataSource.loaded)
    }

    @Test
    fun aRefundBringsTheAdsBackWithoutASecondConsent() = runTest {
        val fixture = fixture(premium = PremiumStatus.Premium(PurchaseSource.Play))

        fixture.entitlement.update(PremiumStatus.Free)

        // Consent was given once and never taken back, so "may we ask" follows the entitlement
        // rather than the user having to answer the same question again.
        assertTrue(fixture.ads.mayRequestAds())
        assertTrue(fixture.ads.shouldOfferTest(9))
    }

    // --- frequency ----------------------------------------------------------

    @Test
    fun theOfferIsDueOnlyOnTheWayIntoATenthTest() = runTest {
        val fixture = fixture()

        assertTrue(fixture.ads.shouldOfferTest(9))
        assertFalse(fixture.ads.shouldOfferTest(8))
        assertFalse(fixture.ads.shouldOfferTest(10))
        assertTrue(fixture.ads.shouldOfferTest(19))
    }

    @Test
    fun dismissingTheDialogLeavesTheOfferDue() = runTest {
        val fixture = fixture()

        // Dismissing is the absence of a call: nothing is written, so tapping again asks again.
        assertTrue(fixture.ads.shouldOfferTest(9))
        assertTrue(fixture.restarted().shouldOfferTest(9))
    }

    @Test
    fun watchingTheAdMarksTheOfferSpentSoItIsNotAskedTwice() = runTest {
        val fixture = fixture()

        assertEquals(AdResult.Shown, fixture.ads.showTestOfferAd(9))

        // Spent here...
        assertFalse(fixture.ads.shouldOfferTest(9))
        // ...and still spent after the app is killed, which is what the marker is for: the count has
        // not moved on yet, so without it the user would be asked again for the ad they just watched.
        assertFalse(fixture.restarted().shouldOfferTest(9))
        // The ladder moves on regardless.
        assertTrue(fixture.restarted().shouldOfferTest(19))
    }

    @Test
    fun noFillMarksTheOfferSpentTooAndLetsTheUserThrough() = runTest {
        val fixture = fixture(result = AdResult.NoFill)

        assertEquals(AdResult.NoFill, fixture.ads.showTestOfferAd(9))

        // Nobody was shown an ad, but the user was let through, so the offer is spent rather than
        // offered again the moment they tap. A dialog that cannot be answered is worse than no
        // dialog at all, and the user has already paid for this one with the wait.
        assertFalse(fixture.restarted().shouldOfferTest(9))
    }

    @Test
    fun aSecondTapCannotServeASecondAd() = runTest {
        val fixture = fixture()

        assertEquals(AdResult.Shown, fixture.ads.showTestOfferAd(9))
        assertEquals(AdResult.NotDue, fixture.ads.showTestOfferAd(9))
        assertEquals(listOf(AdSlot.TestOfferInterstitial), fixture.dataSource.shown)
    }

    // --- the closing ad -----------------------------------------------------

    @Test
    fun theClosingAdIsShownAfterAnOrdinaryResult() = runTest {
        val fixture = fixture()

        assertEquals(AdResult.Shown, fixture.ads.showClosingAd(3))
        assertEquals(listOf(AdSlot.ResultsInterstitial), fixture.dataSource.shown)
    }

    @Test
    fun theClosingAdYieldsWhenTheNextTestIsGated() = runTest {
        val fixture = fixture()

        // The ninth test has just been marked, so the user's next tap is the gated one. Two
        // fullscreen ads on one trip through the app is the thing being avoided, and the gate is
        // worth more than the result screen's ad, because it is the one that can be bought out of.
        assertEquals(AdResult.NotDue, fixture.ads.showClosingAd(9))
        assertEquals(AdResult.NotDue, fixture.ads.showClosingAd(19))
        assertEquals(emptyList(), fixture.dataSource.shown)
    }

    @Test
    fun theClosingAdFollowsConsentAndTheEntitlementToo() = runTest {
        assertEquals(AdResult.Unavailable, fixture(adsEnabled = false).ads.showClosingAd(3))
        assertEquals(
            AdResult.Unavailable,
            fixture(premium = PremiumStatus.Premium(PurchaseSource.Play)).ads.showClosingAd(3),
        )
    }

    // --- what a placement reads ---------------------------------------------

    @Test
    fun theAllowedAnswerIsAvailableAsAFlowForThePlacementsToFollow() = runTest {
        val fixture = fixture(adsEnabled = false)

        // What a banner placement reads: the answer as it changes, rather than a snapshot taken when
        // the screen was built. Both of the things it depends on change while the user is looking at
        // the screen - they can withdraw consent in Settings, and they can buy Premium.
        assertEquals(false, fixture.ads.adsAllowed.first())

        fixture.preferences.setAdsEnabled(true)
        assertEquals(true, fixture.ads.adsAllowed.first())

        fixture.entitlement.update(PremiumStatus.Premium(PurchaseSource.Play))
        assertEquals(false, fixture.ads.adsAllowed.first())
    }

    // --- the preload --------------------------------------------------------

    @Test
    fun theOfferAdIsLoadedAheadOfBeingNeededWhenItMayBe() = runTest {
        val fixture = fixture()

        fixture.ads.preloadTestOffer()
        assertEquals(listOf(AdSlot.TestOfferInterstitial), fixture.dataSource.preloaded)

        // And not at all when it may not be asked for. Loading is a request like any other, and the
        // one that reaches the network before a single pixel is drawn.
        val off = fixture(adsEnabled = false)
        off.ads.preloadTestOffer()
        assertEquals(emptyList(), off.dataSource.preloaded)
    }

    // --- the age answer -----------------------------------------------------

    @Test
    fun anUnder16WithoutAnAgreementIsAskedForNothing() = runTest {
        val fixture = fixture(overConsentAge = false)

        // What the switch says is not the question: nobody who cannot agree alone gets a request.
        assertTrue(fixture.preferences.preferences.value.adsEnabled)
        assertFalse(fixture.ads.mayRequestAds())
        assertEquals(AdResult.Unavailable, fixture.ads.showTestOfferAd(9))
        assertEquals(emptyList(), fixture.dataSource.preloaded)
    }

    @Test
    fun anUnansweredQuestionIsAskedForNothingEither() = runTest {
        val fixture = fixture(overConsentAge = null)

        assertFalse(fixture.ads.mayRequestAds())
    }

    @Test
    fun anUnder16WhoseParentOrGuardianAgreedIsAsked() = runTest {
        val fixture = fixture(overConsentAge = false, parentalConsent = true)

        assertTrue(fixture.ads.mayRequestAds())
    }

    @Test
    fun theSdkIsToldWhoIsUnderTheAgeOfConsent() = runTest {
        // Pushed when the repository is built, because a banner asks the SDK directly rather than
        // coming through here.
        assertEquals(listOf(true), fixture(overConsentAge = false).dataSource.underAgeOfConsent)
        assertEquals(listOf(false), fixture(overConsentAge = true).dataSource.underAgeOfConsent)
    }

    @Test
    fun anUnansweredQuestionIsTreatedAsUnderTheAgeOfConsent() = runTest {
        // Conservative on purpose: the flag comes off on an answer the user gave, never by default.
        assertEquals(listOf(true), fixture(overConsentAge = null).dataSource.underAgeOfConsent)
    }
}

package com.spiraclestudios.autoskola.data.ads

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import com.spiraclestudios.autoskola.data.preferences.AgeBand
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
 * The gate on asking for an ad at all - the entitlement, then the frequency - and what the user's
 * answer does instead of gating: it decides whether the ads are personalized, never whether there are
 * any.
 *
 * All of it is settled here rather than on a device, because these are the rules whose mistakes show
 * up as a policy problem or a chargeback rather than as a crash: an app that personalizes before it
 * was allowed to, or that keeps asking a user who has paid to make it stop.
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
        val consent: FakeAdConsent,
    ) {
        val ads = AdRepository(dataSource, preferences, entitlement, settings, consent)

        /** The app restarted: same storage, same store, same consent, fresh repository. */
        fun restarted() = AdRepository(dataSource, PreferencesRepository(settings), entitlement, settings, consent)
    }

    private fun fixture(
        available: Boolean = true,
        result: AdResult = AdResult.Shown,
        personalizedAds: Boolean = true,
        premium: PremiumStatus = PremiumStatus.Free,
        settings: Settings = MapSettings(),
        // Google's form says ads may be requested unless a test is about it saying otherwise.
        canRequestAds: Boolean = true,
        privacyOptionsRequired: Boolean = false,
        answerOnGather: Boolean = canRequestAds,
        // 16 or older unless a test says otherwise: the answer, the entitlement and the frequency are
        // all settled by an adult, and the age rule itself is in `ConsentAgeTest`. The age is written
        // before the answer, so a test can build the state an older install arrives in — the switch
        // stored as yes, and no question ever asked.
        ageBand: AgeBand? = AgeBand.Adult,
        parentalConsent: Boolean = false,
    ) = Fixture(
        dataSource = FakeAdDataSource(available = available, result = result),
        preferences = PreferencesRepository(settings).apply {
            setParentalConsent(parentalConsent)
            setAgeBand(ageBand)
            setPersonalizedAdsEnabled(personalizedAds)
        },
        entitlement = PremiumRepository(premium),
        settings = settings,
        consent = FakeAdConsent(canRequestAds, privacyOptionsRequired, answerOnGather),
    )

    // --- the user's answer --------------------------------------------------

    @Test
    fun adsAreRequestedWhateverTheUserAnsweredAboutPersonalizing() = runTest {
        val fixture = fixture(personalizedAds = false)

        // The answer decides which kind of ad is asked for, so refusing it leaves every request in
        // place: the app is free because of the ads, and a refusal is not a way to switch them off.
        assertTrue(fixture.ads.mayRequestAds())
        assertTrue(fixture.ads.shouldOfferTest(9))
        assertEquals(AdResult.Shown, fixture.ads.showTestOfferAd(9))
        assertEquals(listOf(AdSlot.TestOfferInterstitial), fixture.dataSource.shown)
    }

    @Test
    fun theSdkIsToldToKeepAdsGenericUntilTheUserAgrees() = runTest {
        // Pushed when the repository is built, because a banner asks the SDK directly rather than
        // coming through here.
        assertEquals(listOf(false), fixture(personalizedAds = false).dataSource.personalizedAds)
        assertEquals(listOf(true), fixture(personalizedAds = true).dataSource.personalizedAds)
    }

    @Test
    fun withdrawingThePersonalizationMidSessionReachesTheSdkBeforeTheNextRequest() = runTest {
        val fixture = fixture(personalizedAds = true)

        fixture.preferences.setPersonalizedAdsEnabled(false)
        val result = fixture.ads.showTestOfferAd(9)

        // Still shown - only generic now - and told so before the request was made.
        assertEquals(AdResult.Shown, result)
        assertEquals(false, fixture.dataSource.personalizedAds.last())
    }

    @Test
    fun theAnswerIsPushedAgainWhenAScreenChangesIt() = runTest {
        val fixture = fixture(personalizedAds = false)

        fixture.preferences.setPersonalizedAdsEnabled(true)
        fixture.ads.pushConsent()

        assertEquals(listOf(false, true), fixture.dataSource.personalizedAds)
    }

    @Test
    fun aTargetThatCannotServeAdsIsAskedForNothing() = runTest {
        val fixture = fixture(available = false)

        assertFalse(fixture.ads.mayRequestAds())
        assertEquals(AdResult.Unavailable, fixture.ads.showTestOfferAd(9))
        assertEquals(emptyList(), fixture.dataSource.preloaded)
    }

    // --- Google's consent form ----------------------------------------------

    @Test
    fun nothingIsAskedForUntilTheFormSaysAdsMayBeRequested() = runTest {
        val fixture = fixture(canRequestAds = false)

        // The form is the first condition on every request, whatever else is true: in the EEA nothing
        // is worth requesting without it, and nothing may be.
        assertFalse(fixture.ads.mayRequestAds())
        assertFalse(fixture.ads.shouldOfferTest(9))
        assertEquals(AdResult.Unavailable, fixture.ads.showTestOfferAd(9))
        assertEquals(AdResult.Unavailable, fixture.ads.showClosingAd(3))
        assertEquals(emptyList(), fixture.dataSource.shown)
        assertEquals(emptyList(), fixture.dataSource.preloaded)
    }

    @Test
    fun theFormAnsweringMidSessionIsFollowedByThePlacements() = runTest {
        val fixture = fixture(canRequestAds = false)
        assertEquals(false, fixture.ads.adsAllowed.first())

        fixture.consent.answer(canRequestAds = true)
        assertEquals(true, fixture.ads.adsAllowed.first())

        // And withdrawing from it later takes them away again, which is what the form promises.
        fixture.consent.answer(canRequestAds = false)
        assertEquals(false, fixture.ads.adsAllowed.first())
        assertFalse(fixture.ads.mayRequestAds())
    }

    @Test
    fun preparingAsksTheFormForThisAudienceAndThenStartsTheSdk() = runTest {
        val fixture = fixture(canRequestAds = false, answerOnGather = true, ageBand = AgeBand.From16To17)

        fixture.ads.prepare()

        assertEquals(listOf(AdAudience.Teen), fixture.consent.gathered)
        assertEquals(1, fixture.dataSource.initializations)
        assertTrue(fixture.ads.mayRequestAds())
    }

    @Test
    fun theSdkIsNotStartedWhenTheUserDeclinedTheForm() = runTest {
        val fixture = fixture(canRequestAds = false, answerOnGather = false)

        fixture.ads.prepare()

        assertEquals(listOf(AdAudience.Adult), fixture.consent.gathered)
        // Google wants the SDK started after consent and not before it.
        assertEquals(0, fixture.dataSource.initializations)
        assertFalse(fixture.ads.mayRequestAds())
    }

    @Test
    fun aChildIsAskedForTheFormAsAChild() = runTest {
        // The form itself does not ask a user under the age of consent anything; it only has to be told.
        val fixture = fixture(ageBand = AgeBand.Under16)

        fixture.ads.prepare()

        assertEquals(listOf(AdAudience.Child), fixture.consent.gathered)
    }

    @Test
    fun aUserWhoHasPaidIsNeverAskedAQuestionWithNoConsequence() = runTest {
        val fixture = fixture(canRequestAds = false, premium = PremiumStatus.Premium(PurchaseSource.Play))

        fixture.ads.prepare()

        assertEquals(emptyList(), fixture.consent.gathered)
        assertEquals(0, fixture.dataSource.initializations)
    }

    @Test
    fun aTargetThatCannotServeAdsNeverShowsTheForm() = runTest {
        val fixture = fixture(available = false, canRequestAds = false)

        fixture.ads.prepare()

        assertEquals(emptyList(), fixture.consent.gathered)
        assertEquals(0, fixture.dataSource.initializations)
    }

    @Test
    fun preparingPushesWhatTheSdkNeedsToKnowBeforeAnythingIsAsked() = runTest {
        val fixture = fixture(personalizedAds = true, ageBand = AgeBand.Adult)

        fixture.ads.prepare()

        assertEquals(AdAudience.Adult, fixture.dataSource.audiences.last())
        assertEquals(true, fixture.dataSource.personalizedAds.last())
    }

    @Test
    fun theFormIsAskedAgainWhenTheAgeAnswerChanges() = runTest {
        val fixture = fixture(ageBand = AgeBand.Under16)
        fixture.ads.prepare()

        fixture.preferences.setAgeBand(AgeBand.Adult)
        fixture.ads.refreshConsent()

        // Somebody who has just turned 18 is no longer one to leave unasked.
        assertEquals(listOf(AdAudience.Child, AdAudience.Adult), fixture.consent.gathered)
    }

    @Test
    fun reopeningThePrivacyOptionsRestartsAdsIfTheUserNowAllowsThem() = runTest {
        val fixture = fixture(canRequestAds = false, privacyOptionsRequired = true)
        fixture.consent.answerOnPrivacyOptions = true

        assertTrue(fixture.ads.privacyOptionsRequired.value)
        fixture.ads.showPrivacyOptions()

        assertEquals(1, fixture.consent.privacyOptionsShown)
        assertTrue(fixture.ads.mayRequestAds())
        assertEquals(1, fixture.dataSource.initializations)
    }

    @Test
    fun theWayBackIntoTheFormIsOfferedOnlyWhereTheFormExists() = runTest {
        assertFalse(fixture().ads.privacyOptionsRequired.value)
        assertTrue(fixture(privacyOptionsRequired = true).ads.privacyOptionsRequired.value)
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
    fun aBuyerOfTheOldRemoveAdsProductIsAskedForNothing() = runTest {
        // Bought in the app before it had ads of its own: the product says the ads do not come back.
        val fixture = fixture(premium = PremiumStatus.AdsRemoved(PurchaseSource.Play))

        assertFalse(fixture.ads.mayRequestAds())
        assertFalse(fixture.ads.shouldOfferTest(9))
        assertEquals(AdResult.Unavailable, fixture.ads.showTestOfferAd(9))
        assertEquals(AdResult.Unavailable, fixture.ads.showClosingAd(3))
        assertEquals(false, fixture.ads.adsAllowed.first())
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
    fun aRefundBringsTheAdsBackWithoutAskingAnythingAgain() = runTest {
        val fixture = fixture(premium = PremiumStatus.Premium(PurchaseSource.Play))

        fixture.entitlement.update(PremiumStatus.Free)

        // Nothing else changed, so "may we ask" follows the entitlement alone: the user has no
        // second question to answer.
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
    fun theClosingAdFollowsTheEntitlementToo() = runTest {
        assertEquals(
            AdResult.Unavailable,
            fixture(premium = PremiumStatus.Premium(PurchaseSource.Play)).ads.showClosingAd(3),
        )
    }

    @Test
    fun theClosingAdIsShownToAUserWhoRefusedPersonalizing() = runTest {
        assertEquals(AdResult.Shown, fixture(personalizedAds = false).ads.showClosingAd(3))
    }

    // --- what a placement reads ---------------------------------------------

    @Test
    fun theAllowedAnswerIsAvailableAsAFlowForThePlacementsToFollow() = runTest {
        val fixture = fixture(personalizedAds = false)

        // What a banner placement reads: the answer as it changes, rather than a snapshot taken when
        // the screen was built. What it depends on can change while the user is looking at the screen
        // - they can buy Premium - and the personalization answer, which can too, is not part of it.
        assertEquals(true, fixture.ads.adsAllowed.first())

        fixture.preferences.setPersonalizedAdsEnabled(true)
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
        val paid = fixture(premium = PremiumStatus.Premium(PurchaseSource.Play))
        paid.ads.preloadTestOffer()
        assertEquals(emptyList(), paid.dataSource.preloaded)
    }

    // --- the age answer -----------------------------------------------------

    @Test
    fun anUnder16WithoutAnAgreementIsStillShownAdsButNeverPersonalizedOnes() = runTest {
        val fixture = fixture(ageBand = AgeBand.Under16)

        // What the switch says is not the question: nobody who cannot agree alone gets a personalized
        // ad, whatever was stored. They are shown generic ones like everybody who has not agreed.
        assertTrue(fixture.preferences.preferences.value.personalizedAdsEnabled)
        assertFalse(fixture.ads.personalizedAds())
        assertEquals(listOf(false), fixture.dataSource.personalizedAds)
        assertTrue(fixture.ads.mayRequestAds())
        assertEquals(AdResult.Shown, fixture.ads.showTestOfferAd(9))
    }

    @Test
    fun anUnansweredQuestionIsNeverPersonalizedEither() = runTest {
        val fixture = fixture(ageBand = null)

        assertFalse(fixture.ads.personalizedAds())
        assertTrue(fixture.ads.mayRequestAds())
    }

    @Test
    fun aParentOrGuardiansAgreementNeverUnlocksPersonalizedAds() = runTest {
        val fixture = fixture(ageBand = AgeBand.Under16, parentalConsent = true)

        // The agreement opens the crash-report and statistics switches and nothing more: under 18 no
        // answer personalizes anything, whoever gave it.
        assertTrue(fixture.preferences.preferences.value.personalizedAdsEnabled)
        assertFalse(fixture.ads.personalizedAds())
        assertEquals(listOf(false), fixture.dataSource.personalizedAds)
    }

    @Test
    fun a16To17YearOldIsNeverPersonalizedEitherButIsShownAds() = runTest {
        val fixture = fixture(ageBand = AgeBand.From16To17)

        assertTrue(fixture.preferences.preferences.value.personalizedAdsEnabled)
        assertFalse(fixture.ads.personalizedAds())
        assertTrue(fixture.ads.mayRequestAds())
        assertEquals(AdResult.Shown, fixture.ads.showTestOfferAd(9))
    }

    @Test
    fun anAdultWhoSaidYesIsPersonalized() = runTest {
        assertTrue(fixture(ageBand = AgeBand.Adult, personalizedAds = true).ads.personalizedAds())
    }

    @Test
    fun anAdultWhoAnsweredNoIsNotPersonalized() = runTest {
        assertFalse(fixture(personalizedAds = false).ads.personalizedAds())
    }

    @Test
    fun theSdkIsToldWhoTheAdsAreFor() = runTest {
        // Pushed when the repository is built, because a banner asks the SDK directly rather than
        // coming through here. Each band has its own treatment: the child flag and rating, a teen
        // rating without the flag, and neither for an adult.
        assertEquals(listOf(AdAudience.Child), fixture(ageBand = AgeBand.Under16).dataSource.audiences)
        assertEquals(listOf(AdAudience.Teen), fixture(ageBand = AgeBand.From16To17).dataSource.audiences)
        assertEquals(listOf(AdAudience.Adult), fixture(ageBand = AgeBand.Adult).dataSource.audiences)
    }

    @Test
    fun anUnansweredQuestionIsTreatedAsAChild() = runTest {
        // Conservative on purpose: the child treatment comes off on an answer the user gave, never by
        // default.
        assertEquals(listOf(AdAudience.Child), fixture(ageBand = null).dataSource.audiences)
    }

    @Test
    fun theAudienceFollowsTheAnswerWhenItChanges() = runTest {
        val fixture = fixture(ageBand = AgeBand.Under16)

        fixture.preferences.setAgeBand(AgeBand.Adult)
        fixture.ads.pushConsent()

        // The way up lifts the child treatment as well as setting it on the way down.
        assertEquals(listOf(AdAudience.Child, AdAudience.Adult), fixture.dataSource.audiences)
    }
}

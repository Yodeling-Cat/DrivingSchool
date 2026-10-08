package com.spiraclestudios.autoskola.settings

import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.data.ads.AdAudience
import com.spiraclestudios.autoskola.data.analytics.AnalyticsEvent
import com.spiraclestudios.autoskola.data.preferences.AgeBand
import com.spiraclestudios.autoskola.data.preferences.TestSelection
import com.spiraclestudios.autoskola.data.premium.PurchaseResult
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.onboarding.ExamLabel
import com.spiraclestudios.autoskola.premium.PremiumFeature
import com.spiraclestudios.autoskola.premium.PremiumStatus
import com.spiraclestudios.autoskola.premium.PurchaseSource
import com.spiraclestudios.autoskola.testing.TestRepositories
import com.spiraclestudios.autoskola.testing.ViewModelTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsViewModelTest : ViewModelTest() {

    private val repositories = TestRepositories().apply {
        preferences.select(TestSelection("sk-sk", LicenseCategory.C))
        // An adult, because the three switches can only be answered by one — or by an under-16 whose
        // parent or guardian agreed. The locked state has its own tests below.
        preferences.setAgeBand(AgeBand.Adult)
    }
    private val viewModel by lazy {
        SettingsViewModel(
            repositories.preferences,
            repositories.exams,
            repositories.analytics,
            repositories.ads,
            repositories.premium,
            repositories.premiumSync,
        )
    }

    @Test
    fun theThreeAnswersCanBeChangedHere() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.SetCrashReporting(true))

        assertEquals(true, state.value.crashReportingEnabled)
        assertEquals(false, state.value.analyticsEnabled)
        assertEquals(false, state.value.personalizedAdsEnabled)
        assertEquals(true, repositories.preferences.preferences.value.crashReportingEnabled)
    }

    @Test
    fun thePersonalizedAdsAnswerBelongsToSettingsToo() = runTest {
        val state = observe(viewModel.uiState)

        // Off, because it starts off and this is a fresh install. The row is what lets a user who
        // said no in onboarding change their mind without reinstalling.
        assertFalse(state.value.personalizedAdsEnabled)

        viewModel.onEvent(SettingsUiEvent.SetPersonalizedAds(true))

        assertTrue(state.value.personalizedAdsEnabled)
        assertTrue(repositories.preferences.preferences.value.personalizedAdsEnabled)

        viewModel.onEvent(SettingsUiEvent.SetPersonalizedAds(false))

        assertFalse(state.value.personalizedAdsEnabled)
        assertFalse(repositories.preferences.preferences.value.personalizedAdsEnabled)
    }

    @Test
    fun changingThePersonalizedAdsSwitchRecordsTheChange() = runTest {
        repositories.preferences.setAnalyticsEnabled(true)
        observe(viewModel.uiState)
        repositories.analyticsSink.events.clear()

        viewModel.onEvent(SettingsUiEvent.SetPersonalizedAds(true))

        assertEquals(listOf(AnalyticsEvent.ConsentChanged), repositories.analyticsSink.events)
        assertEquals(
            mapOf("switch" to "personalized_ads", "enabled" to true),
            repositories.analyticsSink.properties.single(),
        )
    }

    @Test
    fun thePersonalizedAdsAnswerIsNotOnePostHogIsTold() = runTest {
        repositories.preferences.setAnalyticsEnabled(true)
        observe(viewModel.uiState)
        val before = repositories.analyticsSink.consents.size

        viewModel.onEvent(SettingsUiEvent.SetPersonalizedAds(true))

        // `syncConsent` is what tells PostHog which of its own switches are on, so it is called for
        // the two answers that gate what PostHog may collect and not for this one: the ad SDK is told
        // through `AdRepository`.
        assertEquals(before, repositories.analyticsSink.consents.size)
    }

    @Test
    fun showsTheCurrentChoices() = runTest {
        val state = observe(viewModel.uiState).value

        assertEquals(ExamLabel(LicenseCategory.C, "sk"), state.exam)
        assertTrue(state.checkAnswersImmediately)
    }

    @Test
    fun changesAreSaved() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.SelectLanguage(AppLanguage.Hungarian))
        viewModel.onEvent(SettingsUiEvent.SetCheckAnswersImmediately(false))

        assertEquals(AppLanguage.Hungarian, state.value.language)
        assertFalse(state.value.checkAnswersImmediately)
        assertFalse(repositories.preferences.preferences.value.checkAnswersImmediately)
    }

    @Test
    fun autoAdvanceIsSavedAndOnlyWhileAnswersArentCheckedImmediately() = runTest {
        val state = observe(viewModel.uiState)

        // Checked answers are the default, and they rule auto-advance out.
        assertFalse(state.value.advanceAfterAnswer)
        assertFalse(state.value.canAdvanceAfterAnswer)

        viewModel.onEvent(SettingsUiEvent.SetAdvanceAfterAnswer(true))
        assertFalse(state.value.advanceAfterAnswer)

        viewModel.onEvent(SettingsUiEvent.SetCheckAnswersImmediately(false))
        assertTrue(state.value.canAdvanceAfterAnswer)
        viewModel.onEvent(SettingsUiEvent.SetAdvanceAfterAnswer(true))
        assertTrue(state.value.advanceAfterAnswer)
        assertTrue(repositories.preferences.preferences.value.advanceAfterAnswer)

        // Checking answers as they're picked turns it back off.
        viewModel.onEvent(SettingsUiEvent.SetCheckAnswersImmediately(true))
        assertFalse(state.value.advanceAfterAnswer)
        assertFalse(repositories.preferences.preferences.value.advanceAfterAnswer)
        assertFalse(state.value.canAdvanceAfterAnswer)
    }

    @Test
    fun changingAConsentSwitchRecordsTheChange() = runTest {
        repositories.preferences.setAnalyticsEnabled(true)
        observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.SetCrashReporting(true))

        assertEquals(listOf(AnalyticsEvent.ConsentChanged), repositories.analyticsSink.events)
        assertEquals(
            mapOf("switch" to "crash_reporting", "enabled" to true),
            repositories.analyticsSink.properties.single(),
        )
    }

    @Test
    fun turningAnalyticsOffRecordsTheChangeTheUserJustMade() = runTest {
        // The answer in force when the switch is pressed is still "on", so the withdrawal is
        // recorded; the preference is then written, and nothing more is sent from then on.
        repositories.preferences.setAnalyticsEnabled(true)
        observe(viewModel.uiState)
        repositories.analyticsSink.events.clear()
        repositories.analyticsSink.properties.clear()

        viewModel.onEvent(SettingsUiEvent.SetAnalytics(false))

        assertEquals(listOf(AnalyticsEvent.ConsentChanged), repositories.analyticsSink.events)
        assertEquals(
            mapOf("switch" to "analytics", "enabled" to false),
            repositories.analyticsSink.properties.single(),
        )
        assertFalse(repositories.preferences.preferences.value.analyticsEnabled)
    }

    @Test
    fun settingASwitchToWhatItAlreadyIsRecordsNothing() = runTest {
        repositories.preferences.setAnalyticsEnabled(true)
        observe(viewModel.uiState)
        repositories.analyticsSink.events.clear()

        viewModel.onEvent(SettingsUiEvent.SetAnalytics(true))

        assertEquals(emptyList(), repositories.analyticsSink.events)
    }

    @Test
    fun pickingADifferentLanguageRecordsIt() = runTest {
        repositories.preferences.setAnalyticsEnabled(true)
        observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.SelectLanguage(AppLanguage.Hungarian))

        assertEquals(listOf(AnalyticsEvent.LanguageChanged), repositories.analyticsSink.events)
        assertEquals(mapOf("to" to "hu"), repositories.analyticsSink.properties.single())
    }

    @Test
    fun pickingTheLanguageAlreadyInUseRecordsNothing() = runTest {
        repositories.preferences.setAnalyticsEnabled(true)
        observe(viewModel.uiState)
        val current = repositories.preferences.preferences.value.language

        viewModel.onEvent(SettingsUiEvent.SelectLanguage(current))

        assertEquals(emptyList(), repositories.analyticsSink.events)
    }

    @Test
    fun showsTheIdentifierOurStatisticsAreFiledUnder() = runTest {
        repositories.analyticsSink.identifier = "ident-9"
        val state = observe(viewModel.uiState)

        // A fresh install has agreed to nothing, so there is nothing to file anything under yet.
        assertNull(state.value.deviceIdentifier)

        viewModel.onEvent(SettingsUiEvent.SetAnalytics(true))

        assertEquals("ident-9", state.value.deviceIdentifier)
    }

    @Test
    fun showsTheIdentifierThatWasRememberedWithBothAnswersOff() = runTest {
        repositories.analyticsSink.identifier = "ident-9"
        repositories.preferences.setAnalyticsEnabled(true)
        repositories.analytics.syncConsent()
        repositories.analytics.refreshDeviceIdentifier()

        // What a restart with both answers off looks like: the SDK reports nothing, and the row still
        // shows the identifier the earlier data is filed under.
        repositories.preferences.setAnalyticsEnabled(false)
        repositories.analytics.syncConsent()
        repositories.analyticsSink.identifier = null
        val state = observe(viewModel.uiState)

        assertFalse(state.value.analyticsEnabled)
        assertEquals("ident-9", state.value.deviceIdentifier)
    }

    // ── The age question ──
    //
    // The one thing on this screen a user changes because they changed: somebody answers at 15 and is
    // 16 the year after. Only ever upwards - nobody gets younger. Under 16 the crash-report and
    // statistics switches are drawn locked, and refused here as well.

    @Test
    fun anUnder16HasTheSwitchesLockedAndTheReasonDrawn() = runTest {
        repositories.preferences.setAgeBand(AgeBand.Under16)
        val state = observe(viewModel.uiState)

        assertEquals(AgeBand.Under16, state.value.ageBand)
        assertFalse(state.value.canAnswerSwitches)
        assertTrue(state.value.underConsentAge)
        assertTrue(state.value.canCorrectAge)
    }

    @Test
    fun under16LocksTheSwitchesAndRefusesChanges() = runTest {
        repositories.preferences.setAgeBand(AgeBand.Under16)
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.SetAnalytics(true))

        assertFalse(state.value.canAnswerSwitches)
        assertFalse(state.value.analyticsEnabled)
        assertFalse(repositories.preferences.preferences.value.analyticsEnabled)
    }

    @Test
    fun anUnder16WhoTurns18GetsTheSwitchesBack() = runTest {
        repositories.preferences.setAgeBand(AgeBand.Under16)
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.SetAgeBand(AgeBand.Adult))
        viewModel.onEvent(SettingsUiEvent.ConfirmAgeBand)

        assertTrue(state.value.canAnswerSwitches)
        assertFalse(state.value.underConsentAge)
    }

    /** The agreement is given on the consent screen, which is the only screen that offers it. */
    @Test
    fun anAgreementGivenOnTheConsentScreenUnlocksTheFirstTwoHere() = runTest {
        repositories.preferences.setParentalConsent(true)
        repositories.preferences.setAgeBand(AgeBand.Under16)
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.SetCrashReporting(true))
        viewModel.onEvent(SettingsUiEvent.SetPersonalizedAds(true))

        assertTrue(state.value.canAnswerSwitches)
        assertTrue(state.value.crashReportingEnabled)
        // The agreement never opens the third: personalized ads are an adult's alone.
        assertFalse(state.value.canPersonalizeAds)
        assertFalse(state.value.personalizedAdsEnabled)
        assertFalse(repositories.preferences.preferences.value.personalizedAdsEnabled)
    }

    // ── 16 to 17 ──

    @Test
    fun a16To17YearOldHasTheFirstTwoSwitchesAndNotTheAdsOne() = runTest {
        repositories.preferences.setAgeBand(AgeBand.From16To17)
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.SetCrashReporting(true))
        viewModel.onEvent(SettingsUiEvent.SetPersonalizedAds(true))

        assertTrue(state.value.canAnswerSwitches)
        assertFalse(state.value.canPersonalizeAds)
        assertTrue(state.value.crashReportingEnabled)
        assertFalse(state.value.personalizedAdsEnabled)
        // And the age row is still theirs, because the next birthday opens the third switch.
        assertTrue(state.value.canCorrectAge)
        assertFalse(state.value.underConsentAge)
    }

    @Test
    fun anAdultSeesNoAgeRow() = runTest {
        val state = observe(viewModel.uiState)

        assertFalse(state.value.canCorrectAge)
        assertTrue(state.value.canPersonalizeAds)
    }

    @Test
    fun turning18AsksFirstAndThenOpensTheAdsSwitch() = runTest {
        repositories.preferences.setAgeBand(AgeBand.From16To17)
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.SetAgeBand(AgeBand.Adult))

        // Asked about before it counts, because it unlocks a switch and the app cannot check.
        assertEquals(AgeBand.Adult, state.value.confirmingAge)
        assertEquals(AgeBand.From16To17, repositories.preferences.preferences.value.ageBand)
        assertFalse(state.value.canPersonalizeAds)

        viewModel.onEvent(SettingsUiEvent.ConfirmAgeBand)

        assertNull(state.value.confirmingAge)
        assertEquals(AgeBand.Adult, repositories.preferences.preferences.value.ageBand)
        assertTrue(state.value.canPersonalizeAds)
        assertEquals(AdAudience.Adult, repositories.adDataSource.audiences.last())
    }

    @Test
    fun a16To17YearOldIsNotOfferedUnder16AndCannotAnswerIt() = runTest {
        repositories.preferences.setAgeBand(AgeBand.From16To17)
        repositories.preferences.setCrashReportingEnabled(true)
        val state = observe(viewModel.uiState)

        // The row offers their own band and the one above, and no way back down...
        assertEquals(listOf(AgeBand.From16To17, AgeBand.Adult), state.value.ageOptions)

        // ...and the event is refused as well as the pill being hidden, so the rule holds whichever
        // screen is driving this. Nothing is asked, written, withdrawn or pushed.
        val pushed = repositories.adDataSource.audiences.size
        viewModel.onEvent(SettingsUiEvent.SetAgeBand(AgeBand.Under16))

        assertNull(state.value.confirmingAge)
        assertEquals(AgeBand.From16To17, repositories.preferences.preferences.value.ageBand)
        assertTrue(repositories.preferences.preferences.value.crashReportingEnabled)
        assertEquals(pushed, repositories.adDataSource.audiences.size)
    }

    @Test
    fun anUnder16IsOfferedEveryBand() = runTest {
        repositories.preferences.setAgeBand(AgeBand.Under16)
        val state = observe(viewModel.uiState)

        assertEquals(AgeBand.entries.toList(), state.value.ageOptions)
    }

    @Test
    fun anAdultCannotAgeRegressEither() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.SetAgeBand(AgeBand.Under16))
        viewModel.onEvent(SettingsUiEvent.SetAgeBand(AgeBand.From16To17))

        assertNull(state.value.confirmingAge)
        assertEquals(AgeBand.Adult, repositories.preferences.preferences.value.ageBand)
        assertEquals(listOf(AgeBand.Adult), state.value.ageOptions)
    }

    @Test
    fun turning16ToTeenAsksFirstAndKeepsTheAdsAnswerOff() = runTest {
        repositories.preferences.setAgeBand(AgeBand.Under16)
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.SetAgeBand(AgeBand.From16To17))
        assertEquals(AgeBand.From16To17, state.value.confirmingAge)

        viewModel.onEvent(SettingsUiEvent.ConfirmAgeBand)

        assertEquals(AgeBand.From16To17, repositories.preferences.preferences.value.ageBand)
        assertEquals(AdAudience.Teen, repositories.adDataSource.audiences.last())
        assertFalse(repositories.preferences.preferences.value.personalizedAdsEnabled)
    }

    @Test
    fun answeringTheBandYouAreAlreadyInAsksNothing() = runTest {
        repositories.preferences.setAgeBand(AgeBand.From16To17)
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.SetAgeBand(AgeBand.From16To17))

        assertNull(state.value.confirmingAge)
    }

    @Test
    fun movingUpChangesNoAnswerEitherWay() = runTest {
        repositories.preferences.setAgeBand(AgeBand.Under16)
        repositories.preferences.setCrashReportingEnabled(true)
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.SetAgeBand(AgeBand.Adult))
        viewModel.onEvent(SettingsUiEvent.ConfirmAgeBand)

        // The switches are live and as they were: the app never fills an answer in or takes one away
        // because the user had a birthday.
        assertTrue(state.value.canAnswerSwitches)
        assertTrue(repositories.preferences.preferences.value.crashReportingEnabled)
        assertFalse(repositories.preferences.preferences.value.personalizedAdsEnabled)
    }

    @Test
    fun anUnansweredQuestionLeavesThemLocked() = runTest {
        val fresh = TestRepositories(ageBand = null).apply {
            preferences.select(TestSelection("sk-sk", LicenseCategory.C))
        }
        val model =
            SettingsViewModel(
                fresh.preferences,
                fresh.exams,
                fresh.analytics,
                fresh.ads,
                fresh.premium,
                fresh.premiumSync,
            )

        val state = observe(model.uiState)

        assertFalse(state.value.canAnswerSwitches)
        assertFalse(state.value.underConsentAge)
    }

    // The two questions this screen asks before it changes something.

    @Test
    fun turning16AsksBeforeItCounts() = runTest {
        // An under-16 whose switches are locked, which is the state this question is about.
        repositories.preferences.setAgeBand(AgeBand.Under16)
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.SetAgeBand(AgeBand.Adult))

        assertEquals(AgeBand.Adult, state.value.confirmingAge)
        // Nothing is written while the question is on screen: the answer still in force is the old one.
        assertEquals(AgeBand.Under16, repositories.preferences.preferences.value.ageBand)
        assertFalse(state.value.canAnswerSwitches)
    }

    @Test
    fun confirmingTheAgeAnswerAppliesIt() = runTest {
        repositories.preferences.setAgeBand(AgeBand.Under16)
        val state = observe(viewModel.uiState)
        viewModel.onEvent(SettingsUiEvent.SetAgeBand(AgeBand.Adult))

        viewModel.onEvent(SettingsUiEvent.ConfirmAgeBand)

        assertNull(state.value.confirmingAge)
        assertEquals(AgeBand.Adult, repositories.preferences.preferences.value.ageBand)
        assertTrue(state.value.canAnswerSwitches)
    }

    @Test
    fun backingOutOfTheAgeQuestionChangesNothing() = runTest {
        repositories.preferences.setAgeBand(AgeBand.Under16)
        val state = observe(viewModel.uiState)
        viewModel.onEvent(SettingsUiEvent.SetAgeBand(AgeBand.Adult))

        viewModel.onEvent(SettingsUiEvent.DismissAgeConfirmation)

        assertNull(state.value.confirmingAge)
        assertEquals(AgeBand.Under16, state.value.ageBand)
        assertEquals(AgeBand.Under16, repositories.preferences.preferences.value.ageBand)
    }

    // ── Google's consent form ──

    @Test
    fun theWayBackIntoTheFormIsOfferedOnlyWhereTheFormExists() = runTest {
        val state = observe(viewModel.uiState)
        assertFalse(state.value.adPrivacyOptionsRequired)

        val where = TestRepositories().apply {
            preferences.select(TestSelection("sk-sk", LicenseCategory.C))
            adConsent.privacyOptions(required = true)
        }
        val model =
            SettingsViewModel(
                where.preferences,
                where.exams,
                where.analytics,
                where.ads,
                where.premium,
                where.premiumSync,
            )
        val shown = observe(model.uiState)

        assertTrue(shown.value.adPrivacyOptionsRequired)
    }

    @Test
    fun openingThePrivacyOptionsReopensTheForm() = runTest {
        observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.OpenAdPrivacyOptions)

        assertEquals(1, repositories.adConsent.privacyOptionsShown)
    }

    @Test
    fun turningOlderAsksTheFormAgainForTheNewAudience() = runTest {
        repositories.preferences.setAgeBand(AgeBand.From16To17)
        observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.SetAgeBand(AgeBand.Adult))
        viewModel.onEvent(SettingsUiEvent.ConfirmAgeBand)

        assertEquals(listOf(AdAudience.Adult), repositories.adConsent.gathered)
    }

    @Test
    fun turningPersonalizationOffAsksNothingAndTakesEffectAtOnce() = runTest {
        // Analytics has to be on for the record of a change to be sent at all, which is all
        // `ConsentChanged` is: without it the answer would still be written, and nothing said.
        repositories.preferences.setAnalyticsEnabled(true)
        repositories.preferences.setPersonalizedAdsEnabled(true)
        val state = observe(viewModel.uiState)
        repositories.analyticsSink.events.clear()
        repositories.analyticsSink.properties.clear()

        viewModel.onEvent(SettingsUiEvent.SetPersonalizedAds(false))

        // Nothing stands between the user and the answer: they are shown ads either way, so there is
        // nothing the app loses by it that would be worth a question.
        assertFalse(state.value.personalizedAdsEnabled)
        assertFalse(repositories.preferences.preferences.value.personalizedAdsEnabled)
        assertEquals(listOf(AnalyticsEvent.ConsentChanged), repositories.analyticsSink.events)
        assertEquals(
            mapOf("switch" to "personalized_ads", "enabled" to false),
            repositories.analyticsSink.properties.single(),
        )
    }

    @Test
    fun theAdSdkIsToldTheMomentThePersonalizationAnswerChanges() = runTest {
        observe(viewModel.uiState)
        // What the repository pushed when it was built: generic, because nothing has been agreed to.
        assertEquals(listOf(false), repositories.adDataSource.personalizedAds)

        viewModel.onEvent(SettingsUiEvent.SetPersonalizedAds(true))
        viewModel.onEvent(SettingsUiEvent.SetPersonalizedAds(false))

        // A banner asks the SDK itself, so this is the only way either answer reaches the next one.
        assertEquals(listOf(false, true, false), repositories.adDataSource.personalizedAds)
    }

    @Test
    fun anUnder16WithoutAnAgreementCannotTurnPersonalizationOn() = runTest {
        repositories.preferences.setAgeBand(AgeBand.Under16)
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.SetPersonalizedAds(true))

        assertFalse(state.value.personalizedAdsEnabled)
        assertFalse(repositories.preferences.preferences.value.personalizedAdsEnabled)
        assertEquals(false, repositories.adDataSource.personalizedAds.last())
    }

    @Test
    fun removeAdsIsOfferedAtTheStoresOwnPrice() = runTest {
        val state = observe(viewModel.uiState)

        assertEquals(RemoveAdsOffer.ForSale("€0.99"), state.value.removeAds)
    }

    @Test
    fun whereTheStoreSellsNothingThereIsNoRow() = runTest {
        // The web, iOS, a phone with no Play Store, or a Play that did not answer: no price to show.
        repositories.billing.offer = null
        val state = observe(viewModel.uiState)

        assertEquals(RemoveAdsOffer.None, state.value.removeAds)
    }

    @Test
    fun buyingRemoveAdsHereTakesTheAdsAwayAndTheRowSaysThankYou() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.BuyRemoveAds)

        assertEquals(RemoveAdsOffer.Owned, state.value.removeAds)
        assertTrue(repositories.premium.isUnlocked(PremiumFeature.NoAds))
    }

    @Test
    fun aBuyerIsNotOfferedItAgain() = runTest {
        repositories.premium.update(PremiumStatus.AdsRemoved(PurchaseSource.Play))
        val state = observe(viewModel.uiState)

        assertEquals(RemoveAdsOffer.Owned, state.value.removeAds)
        viewModel.onEvent(SettingsUiEvent.BuyRemoveAds)
        assertEquals(0, repositories.billing.purchases)
    }

    @Test
    fun aSecondPressWhileBuyingStartsNoSecondPurchase() = runTest {
        repositories.billing.purchaseDelayMillis = 30_000
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.BuyRemoveAds)
        viewModel.onEvent(SettingsUiEvent.BuyRemoveAds)

        assertEquals(RemoveAdsOffer.ForSale("€0.99", PurchasePhase.Buying), state.value.removeAds)
        assertEquals(1, repositories.billing.purchases)
    }

    @Test
    fun aPendingPaymentIsSaidSoAndTheAdsStayUntilItCompletes() = runTest {
        repositories.billing.purchase = PurchaseResult.Pending
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.BuyRemoveAds)

        assertEquals(RemoveAdsOffer.ForSale("€0.99", PurchasePhase.Pending), state.value.removeAds)
        assertFalse(repositories.premium.isUnlocked(PremiumFeature.NoAds))
    }

    @Test
    fun aFailedPurchaseIsSaidSoAndCanBeTriedAgain() = runTest {
        repositories.billing.purchase = PurchaseResult.Failed
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.BuyRemoveAds)
        assertEquals(RemoveAdsOffer.ForSale("€0.99", PurchasePhase.Failed), state.value.removeAds)

        repositories.billing.purchase = PurchaseResult.Purchased(PremiumStatus.AdsRemoved(PurchaseSource.Play))
        viewModel.onEvent(SettingsUiEvent.BuyRemoveAds)
        assertEquals(RemoveAdsOffer.Owned, state.value.removeAds)
    }

    @Test
    fun backingOutOfTheStoresSheetLeavesTheOfferAsItWas() = runTest {
        repositories.billing.purchase = PurchaseResult.Canceled
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.BuyRemoveAds)

        assertEquals(RemoveAdsOffer.ForSale("€0.99"), state.value.removeAds)
    }
}

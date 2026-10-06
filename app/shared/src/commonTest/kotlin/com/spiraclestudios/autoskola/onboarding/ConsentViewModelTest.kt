package com.spiraclestudios.autoskola.onboarding

import com.spiraclestudios.autoskola.data.analytics.AnalyticsEvent
import com.spiraclestudios.autoskola.data.preferences.CURRENT_LEGAL_VERSION
import com.spiraclestudios.autoskola.testing.TestRepositories
import com.spiraclestudios.autoskola.testing.ViewModelTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConsentViewModelTest : ViewModelTest() {

    private val repositories = TestRepositories().apply {
        // An adult: this screen's tests are about the three switches, which only an adult — or an
        // under-16 whose parent or guardian agreed — may answer at all. The under-16 path has its own
        // fixture, built where it is used.
        preferences.setOverConsentAge(true)
    }
    private val viewModel by lazy {
        ConsentViewModel(repositories.preferences, repositories.analytics, repositories.ads)
    }

    @Test
    fun nothingIsAnsweredOrAgreedToYet() = runTest {
        val state = observe(viewModel.uiState).value

        assertFalse(state.documentsAccepted)
        assertFalse(state.agreementError)
    }

    @Test
    fun theNarrowerButtonRefusesAndMarksTheBoxUntilTheDocumentsAreAccepted() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(ConsentUiEvent.AcceptChosen)

        assertTrue(state.value.agreementError)
        assertEquals(0, repositories.preferences.preferences.value.legalAcceptedVersion)
        assertEquals(emptyList(), repositories.analyticsSink.events)
    }

    @Test
    fun tickingTheAgreementClearsTheMark() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(ConsentUiEvent.AcceptChosen)
        assertTrue(state.value.agreementError)

        viewModel.onEvent(ConsentUiEvent.SetDocumentsAccepted(true))

        assertFalse(state.value.agreementError)
    }

    @Test
    fun acceptingTheDocumentsRecordsTheCurrentVersion() = runTest {
        observe(viewModel.uiState)

        viewModel.onEvent(ConsentUiEvent.SetDocumentsAccepted(true))
        viewModel.onEvent(ConsentUiEvent.AcceptChosen)

        assertEquals(CURRENT_LEGAL_VERSION, repositories.preferences.preferences.value.legalAcceptedVersion)
    }

    @Test
    fun theThreeSwitchesAreAnsweredSeparately() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(ConsentUiEvent.SetCrashReporting(true))

        assertTrue(state.value.crashReportingEnabled)
        // All three start off, and that is a requirement rather than a default waiting to be
        // improved: a pre-ticked switch takes its answer from the user not touching it.
        assertFalse(state.value.analyticsEnabled)
        assertFalse(state.value.personalizedAdsEnabled)
    }

    @Test
    fun aSwitchOnItsOwnCommitsNothing() = runTest {
        observe(viewModel.uiState)

        viewModel.onEvent(ConsentUiEvent.SetCrashReporting(true))
        viewModel.onEvent(ConsentUiEvent.SetAnalytics(true))
        viewModel.onEvent(ConsentUiEvent.SetPersonalizedAds(true))

        // Held, not written: the screen is still asking and the user can still walk away.
        assertFalse(repositories.preferences.preferences.value.crashReportingEnabled)
        assertFalse(repositories.preferences.preferences.value.analyticsEnabled)
        assertFalse(repositories.preferences.preferences.value.personalizedAdsEnabled)
        // The only push is the repository's own, from when it was built.
        assertEquals(listOf(false to false), repositories.analyticsSink.consents)
    }

    @Test
    fun thePersonalizedAdsAnswerIsCommittedSeparatelyFromTheOtherTwo() = runTest {
        observe(viewModel.uiState)

        // Personalized ads allowed, tracking refused. Three separate answers, and this is the shape a user who
        // wants their ads tailored but not to be measured gives.
        viewModel.onEvent(ConsentUiEvent.SetPersonalizedAds(true))
        viewModel.onEvent(ConsentUiEvent.SetDocumentsAccepted(true))
        viewModel.onEvent(ConsentUiEvent.AcceptChosen)

        val preferences = repositories.preferences.preferences.value
        assertTrue(preferences.personalizedAdsEnabled)
        assertFalse(preferences.crashReportingEnabled)
        assertFalse(preferences.analyticsEnabled)
        // And the analytics sink hears nothing about it: personalization is not a PostHog answer, so the SDK
        // is told only what it is allowed to collect.
        assertEquals(listOf(false to false), repositories.analyticsSink.consents)
    }

    @Test
    fun theAdSdkIsToldWhatWasCommittedAndNothingBefore() = runTest {
        observe(viewModel.uiState)
        // What the repository pushed when it was built: generic, because nothing has been agreed to.
        assertEquals(listOf(false), repositories.adDataSource.personalizedAds)

        viewModel.onEvent(ConsentUiEvent.SetPersonalizedAds(true))
        // A switch on its own is held, so the SDK has still heard nothing new.
        assertEquals(listOf(false), repositories.adDataSource.personalizedAds)

        viewModel.onEvent(ConsentUiEvent.SetDocumentsAccepted(true))
        viewModel.onEvent(ConsentUiEvent.AcceptChosen)

        assertEquals(true, repositories.adDataSource.personalizedAds.last())
    }

    @Test
    fun refusingPersonalizationStillLeavesTheUserInTheApp() = runTest {
        observe(viewModel.uiState)

        viewModel.onEvent(ConsentUiEvent.SetDocumentsAccepted(true))
        viewModel.onEvent(ConsentUiEvent.AcceptChosen)

        // Generic ads, and the same way into the app as anyone who said yes: the answer is not a
        // condition of anything.
        assertFalse(repositories.preferences.preferences.value.personalizedAdsEnabled)
        assertEquals(false, repositories.adDataSource.personalizedAds.last())
        assertTrue(repositories.ads.mayRequestAds())
        assertEquals(CURRENT_LEGAL_VERSION, repositories.preferences.preferences.value.legalAcceptedVersion)
    }

    @Test
    fun acceptingTheChosenAnswersCommitsWhatTheSwitchesShow() = runTest {
        observe(viewModel.uiState)

        viewModel.onEvent(ConsentUiEvent.SetCrashReporting(true))
        viewModel.onEvent(ConsentUiEvent.SetDocumentsAccepted(true))
        viewModel.onEvent(ConsentUiEvent.AcceptChosen)

        val preferences = repositories.preferences.preferences.value
        assertTrue(preferences.crashReportingEnabled)
        assertFalse(preferences.analyticsEnabled)
        assertEquals(true to false, repositories.analyticsSink.consents.last())
    }

    @Test
    fun theOnboardingEventOnlyGoesOnceAnalyticsIsAnswered() = runTest {
        observe(viewModel.uiState)

        // The user agreed to the documents but left both switches off, so nothing may be sent.
        viewModel.onEvent(ConsentUiEvent.SetDocumentsAccepted(true))
        viewModel.onEvent(ConsentUiEvent.AcceptChosen)
        assertEquals(emptyList(), repositories.analyticsSink.events)

        viewModel.onEvent(ConsentUiEvent.SetAnalytics(true))
        viewModel.onEvent(ConsentUiEvent.AcceptChosen)

        assertEquals(AnalyticsEvent.OnboardingCompleted, repositories.analyticsSink.events.last())
    }

    @Test
    fun acceptingEverythingAnswersAllThreeQuestionsWithoutTouchingTheSwitches() = runTest {
        observe(viewModel.uiState)

        viewModel.onEvent(ConsentUiEvent.AcceptAll)

        val preferences = repositories.preferences.preferences.value
        assertTrue(preferences.crashReportingEnabled)
        assertTrue(preferences.analyticsEnabled)
        assertTrue(preferences.personalizedAdsEnabled)
        assertEquals(CURRENT_LEGAL_VERSION, preferences.legalAcceptedVersion)
        // The press is the only thing that could have answered: no switch was touched.
        assertEquals(true to true, repositories.analyticsSink.consents.last())
    }

    @Test
    fun acceptingEverythingNeedsNoTickOnTheAgreement() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(ConsentUiEvent.AcceptAll)

        assertTrue(state.value.documentsAccepted)
    }

    @Test
    fun acceptingEverythingSendsTheOnboardingEventInThatOnePress() = runTest {
        observe(viewModel.uiState)

        viewModel.onEvent(ConsentUiEvent.AcceptAll)

        assertEquals(AnalyticsEvent.OnboardingCompleted, repositories.analyticsSink.events.last())
    }

    @Test
    fun acceptingEverythingRecordsBothAnswersAndTheChoice() = runTest {
        observe(viewModel.uiState)

        viewModel.onEvent(ConsentUiEvent.AcceptAll)

        assertEquals(
            listOf(AnalyticsEvent.ConsentAnswered, AnalyticsEvent.OnboardingCompleted),
            repositories.analyticsSink.events,
        )
        assertEquals(
            mapOf("crash_reporting" to true, "analytics" to true, "personalized_ads" to true, "choice" to "accept_all"),
            repositories.analyticsSink.properties.first(),
        )
    }

    @Test
    fun acceptingTheChosenAnswersRecordsWhatTheyWere() = runTest {
        observe(viewModel.uiState)
        viewModel.onEvent(ConsentUiEvent.SetCrashReporting(true))
        viewModel.onEvent(ConsentUiEvent.SetAnalytics(true))
        viewModel.onEvent(ConsentUiEvent.SetDocumentsAccepted(true))

        viewModel.onEvent(ConsentUiEvent.AcceptChosen)

        assertEquals(
            mapOf(
                "crash_reporting" to true,
                "analytics" to true,
                "personalized_ads" to false,
                "choice" to "accept_chosen",
            ),
            repositories.analyticsSink.properties.first(),
        )
    }

    // ── The age question ──
    //
    // Under 16 in Slovakia the three answers are not the user's to give, so they come after the
    // question and, if the answer is under 16, after a parent or guardian's agreement. Each test here
    // builds its own repositories, because the answer is what it is about.

    /** A user nothing has asked yet: what a fresh install and an updated app both open on. */
    private fun unasked() = TestRepositories(overConsentAge = null)

    @Test
    fun theCardIsClosedUntilTheAgeQuestionIsAnswered() = runTest {
        val fresh = unasked()
        val model = ConsentViewModel(fresh.preferences, fresh.analytics, fresh.ads)
        val state = observe(model.uiState)

        // Closed, which is what the screen draws from: the choices are hidden inside the card and both
        // buttons are dead until the question is answered.
        assertFalse(state.value.ageAnswered)
        assertFalse(state.value.switchesEnabled)
        assertFalse(state.value.asksForParentalConsent)

        model.onEvent(ConsentUiEvent.SetOverConsentAge(true))

        assertTrue(state.value.ageAnswered)
    }

    @Test
    fun aPressBeforeTheQuestionIsAnsweredCommitsNothing() = runTest {
        val fresh = unasked()
        val model = ConsentViewModel(fresh.preferences, fresh.analytics, fresh.ads)
        observe(model.uiState)

        // The buttons are drawn dead on screen; refused here as well, so the rule holds whichever
        // screen is driving this.
        model.onEvent(ConsentUiEvent.SetDocumentsAccepted(true))
        model.onEvent(ConsentUiEvent.AcceptAll)

        assertEquals(0, fresh.preferences.preferences.value.legalAcceptedVersion)
        assertEquals(emptyList(), fresh.analyticsSink.events)
        assertFalse(fresh.preferences.preferences.value.analyticsEnabled)
    }

    @Test
    fun under16LocksTheSwitchesAndAsksForAParentOrGuardian() = runTest {
        val fresh = unasked()
        val model = ConsentViewModel(fresh.preferences, fresh.analytics, fresh.ads)
        val state = observe(model.uiState)

        model.onEvent(ConsentUiEvent.SetOverConsentAge(false))

        assertFalse(state.value.switchesEnabled)
        assertTrue(state.value.asksForParentalConsent)
    }

    @Test
    fun answeringThatYouAreOver16LeavesTheAnswersAlone() = runTest {
        // The app never fills one of these in: only the user's own presses turn an answer on, and
        // answering this question is not one of them.
        val fresh = TestRepositories().apply { preferences.setPersonalizedAdsEnabled(true) }
        val model = ConsentViewModel(fresh.preferences, fresh.analytics, fresh.ads)
        val state = observe(model.uiState)
        assertTrue(state.value.personalizedAdsEnabled)

        model.onEvent(ConsentUiEvent.SetOverConsentAge(true))

        assertTrue(state.value.personalizedAdsEnabled)
        assertTrue(state.value.switchesEnabled)
    }

    @Test
    fun answeringThatYouAreUnder16WithdrawsTheStoredAnswers() = runTest {
        // The state an updated app arrives in, with answers already given: an under-16 cannot have
        // given them, so the question takes them back rather than leaving them on behind locked
        // switches — and it writes, so a restart does not bring them back either.
        val fresh = TestRepositories().apply {
            preferences.setCrashReportingEnabled(true)
            preferences.setAnalyticsEnabled(true)
            preferences.setPersonalizedAdsEnabled(true)
        }
        val model = ConsentViewModel(fresh.preferences, fresh.analytics, fresh.ads)
        val state = observe(model.uiState)

        model.onEvent(ConsentUiEvent.SetOverConsentAge(false))

        assertFalse(state.value.crashReportingEnabled)
        assertFalse(state.value.analyticsEnabled)
        assertFalse(state.value.personalizedAdsEnabled)
        assertFalse(state.value.switchesEnabled)
        assertFalse(fresh.preferences.preferences.value.crashReportingEnabled)
        assertFalse(fresh.preferences.preferences.value.analyticsEnabled)
        assertFalse(fresh.preferences.preferences.value.personalizedAdsEnabled)
    }

    @Test
    fun under16CannotCommitWithoutTheAgreement() = runTest {
        val fresh = unasked()
        val model = ConsentViewModel(fresh.preferences, fresh.analytics, fresh.ads)
        val state = observe(model.uiState)

        model.onEvent(ConsentUiEvent.SetOverConsentAge(false))
        model.onEvent(ConsentUiEvent.SetDocumentsAccepted(true))
        model.onEvent(ConsentUiEvent.AcceptAll)

        assertTrue(state.value.parentalError)
        assertEquals(0, fresh.preferences.preferences.value.legalAcceptedVersion)
        assertEquals(emptyList(), fresh.analyticsSink.events)
        assertFalse(fresh.preferences.preferences.value.personalizedAdsEnabled)
    }

    @Test
    fun anUnder16WithNoAgreementSendsNothing() = runTest {
        val fresh = unasked()
        val model = ConsentViewModel(fresh.preferences, fresh.analytics, fresh.ads)
        observe(model.uiState)

        model.onEvent(ConsentUiEvent.SetOverConsentAge(false))
        model.onEvent(ConsentUiEvent.AcceptAll)

        assertEquals(emptyList(), fresh.analyticsSink.events)
        assertFalse(fresh.preferences.preferences.value.crashReportingEnabled)
        assertFalse(fresh.preferences.preferences.value.analyticsEnabled)
    }

    @Test
    fun withTheAgreementTheThreeAnswersAreTheirsToGive() = runTest {
        val fresh = unasked()
        val model = ConsentViewModel(fresh.preferences, fresh.analytics, fresh.ads)
        val state = observe(model.uiState)

        model.onEvent(ConsentUiEvent.SetOverConsentAge(false))
        model.onEvent(ConsentUiEvent.SetParentalConsent(true))
        assertTrue(state.value.switchesEnabled)

        model.onEvent(ConsentUiEvent.SetPersonalizedAds(true))
        model.onEvent(ConsentUiEvent.SetDocumentsAccepted(true))
        model.onEvent(ConsentUiEvent.AcceptChosen)

        val preferences = fresh.preferences.preferences.value
        assertEquals(false, preferences.overConsentAge)
        assertTrue(preferences.parentalConsent)
        assertTrue(preferences.personalizedAdsEnabled)
        assertFalse(preferences.analyticsEnabled)
        assertEquals(CURRENT_LEGAL_VERSION, preferences.legalAcceptedVersion)
    }

    @Test
    fun withdrawingTheAgreementTurnsTheThreeAnswersOff() = runTest {
        val fresh = unasked()
        val model = ConsentViewModel(fresh.preferences, fresh.analytics, fresh.ads)
        val state = observe(model.uiState)

        model.onEvent(ConsentUiEvent.SetOverConsentAge(false))
        model.onEvent(ConsentUiEvent.SetParentalConsent(true))
        model.onEvent(ConsentUiEvent.SetAnalytics(true))
        assertTrue(state.value.analyticsEnabled)

        model.onEvent(ConsentUiEvent.SetParentalConsent(false))

        // The agreement is what let them be answered at all, so taking it back takes them with it —
        // the same withdrawal as answering "under 16", and written, so a restart does not bring them
        // back either.
        assertFalse(state.value.analyticsEnabled)
        assertFalse(state.value.switchesEnabled)
        assertFalse(fresh.preferences.preferences.value.analyticsEnabled)
    }

    @Test
    fun aReconsentOpensOnTheAnswerAlreadyGiven() = runTest {
        val fresh = TestRepositories().apply { preferences.setOverConsentAge(false) }
        val model = ConsentViewModel(fresh.preferences, fresh.analytics, fresh.ads)

        val state = observe(model.uiState)

        assertEquals(false, state.value.overConsentAge)
        assertTrue(state.value.asksForParentalConsent)
    }
}

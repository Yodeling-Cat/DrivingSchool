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
        assertFalse(state.value.adsEnabled)
    }

    @Test
    fun aSwitchOnItsOwnCommitsNothing() = runTest {
        observe(viewModel.uiState)

        viewModel.onEvent(ConsentUiEvent.SetCrashReporting(true))
        viewModel.onEvent(ConsentUiEvent.SetAnalytics(true))
        viewModel.onEvent(ConsentUiEvent.SetAds(true))

        // Held, not written: the screen is still asking and the user can still walk away.
        assertFalse(repositories.preferences.preferences.value.crashReportingEnabled)
        assertFalse(repositories.preferences.preferences.value.analyticsEnabled)
        assertFalse(repositories.preferences.preferences.value.adsEnabled)
        // The only push is the repository's own, from when it was built.
        assertEquals(listOf(false to false), repositories.analyticsSink.consents)
    }

    @Test
    fun theAdsAnswerIsCommittedSeparatelyFromTheOtherTwo() = runTest {
        observe(viewModel.uiState)

        // Ads allowed, tracking refused. Three separate answers, and this is the shape a user who
        // wants the app to stay free but not to be measured gives.
        viewModel.onEvent(ConsentUiEvent.SetAds(true))
        viewModel.onEvent(ConsentUiEvent.SetDocumentsAccepted(true))
        viewModel.onEvent(ConsentUiEvent.AcceptChosen)

        val preferences = repositories.preferences.preferences.value
        assertTrue(preferences.adsEnabled)
        assertFalse(preferences.crashReportingEnabled)
        assertFalse(preferences.analyticsEnabled)
        // And the analytics sink hears nothing about it: ads are not a PostHog answer, so the SDK
        // is told only what it is allowed to collect.
        assertEquals(listOf(false to false), repositories.analyticsSink.consents)
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
        assertTrue(preferences.adsEnabled)
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
            mapOf("crash_reporting" to true, "analytics" to true, "ads" to true, "choice" to "accept_all"),
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
            mapOf("crash_reporting" to true, "analytics" to true, "ads" to false, "choice" to "accept_chosen"),
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
    fun theSwitchesWaitForTheAgeQuestion() = runTest {
        val fresh = unasked()
        val model = ConsentViewModel(fresh.preferences, fresh.analytics, fresh.ads)
        val state = observe(model.uiState)

        assertFalse(state.value.switchesEnabled)
        assertFalse(state.value.asksForParentalConsent)
    }

    @Test
    fun aPressBeforeTheQuestionIsAnsweredMarksItAndCommitsNothing() = runTest {
        val fresh = unasked()
        val model = ConsentViewModel(fresh.preferences, fresh.analytics, fresh.ads)
        val state = observe(model.uiState)

        model.onEvent(ConsentUiEvent.SetDocumentsAccepted(true))
        model.onEvent(ConsentUiEvent.AcceptAll)

        assertTrue(state.value.ageError)
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
    fun sayingUnder16TakesTheStoredAnswersOffTheSwitches() = runTest {
        val fresh = TestRepositories().apply { preferences.setAdsEnabled(true) }
        val model = ConsentViewModel(fresh.preferences, fresh.analytics, fresh.ads)
        val state = observe(model.uiState)

        model.onEvent(ConsentUiEvent.SetOverConsentAge(false))

        assertFalse(state.value.adsEnabled)
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
        assertFalse(fresh.preferences.preferences.value.adsEnabled)
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

        model.onEvent(ConsentUiEvent.SetAds(true))
        model.onEvent(ConsentUiEvent.SetDocumentsAccepted(true))
        model.onEvent(ConsentUiEvent.AcceptChosen)

        val preferences = fresh.preferences.preferences.value
        assertEquals(false, preferences.overConsentAge)
        assertTrue(preferences.parentalConsent)
        assertTrue(preferences.adsEnabled)
        assertFalse(preferences.analyticsEnabled)
        assertEquals(CURRENT_LEGAL_VERSION, preferences.legalAcceptedVersion)
    }

    @Test
    fun withdrawingTheAgreementTakesTheThreeAnswersBack() = runTest {
        val fresh = unasked()
        val model = ConsentViewModel(fresh.preferences, fresh.analytics, fresh.ads)
        val state = observe(model.uiState)

        model.onEvent(ConsentUiEvent.SetOverConsentAge(false))
        model.onEvent(ConsentUiEvent.SetParentalConsent(true))
        model.onEvent(ConsentUiEvent.SetAnalytics(true))
        assertTrue(state.value.analyticsEnabled)

        model.onEvent(ConsentUiEvent.SetParentalConsent(false))

        assertFalse(state.value.analyticsEnabled)
        assertFalse(state.value.switchesEnabled)
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

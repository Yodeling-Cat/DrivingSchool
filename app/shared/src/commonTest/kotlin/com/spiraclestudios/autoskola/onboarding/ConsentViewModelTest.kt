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

    private val repositories = TestRepositories()
    private val viewModel by lazy { ConsentViewModel(repositories.preferences, repositories.analytics) }

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
}

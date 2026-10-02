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
        assertFalse(state.crashReportingEnabled)
        assertFalse(state.analyticsEnabled)
        assertFalse(state.canContinue)
    }

    @Test
    fun continueIsRefusedUntilTheDocumentsAreAccepted() = runTest {
        observe(viewModel.uiState)

        viewModel.onEvent(ConsentUiEvent.Continue)

        assertEquals(0, repositories.preferences.preferences.value.legalAcceptedVersion)
        assertEquals(emptyList(), repositories.analyticsSink.events)
    }

    @Test
    fun acceptingTheDocumentsRecordsTheCurrentVersion() = runTest {
        observe(viewModel.uiState)

        viewModel.onEvent(ConsentUiEvent.SetDocumentsAccepted(true))
        viewModel.onEvent(ConsentUiEvent.Continue)

        assertEquals(CURRENT_LEGAL_VERSION, repositories.preferences.preferences.value.legalAcceptedVersion)
    }

    @Test
    fun theTwoSwitchesAreAnsweredSeparately() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(ConsentUiEvent.SetCrashReporting(true))

        assertTrue(state.value.crashReportingEnabled)
        assertFalse(state.value.analyticsEnabled)
        // The first push is the repository's own, from when it was built.
        assertEquals(listOf(false to false, true to false), repositories.analyticsSink.consents)
    }

    @Test
    fun theOnboardingEventOnlyGoesOnceAnalyticsIsAnswered() = runTest {
        observe(viewModel.uiState)

        // The user agreed to the documents but left both switches off, so nothing may be sent.
        viewModel.onEvent(ConsentUiEvent.SetDocumentsAccepted(true))
        viewModel.onEvent(ConsentUiEvent.Continue)
        assertEquals(emptyList(), repositories.analyticsSink.events)

        viewModel.onEvent(ConsentUiEvent.SetAnalytics(true))
        viewModel.onEvent(ConsentUiEvent.Continue)

        assertEquals(listOf(AnalyticsEvent.OnboardingCompleted), repositories.analyticsSink.events)
    }
}

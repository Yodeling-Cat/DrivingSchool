package com.spiraclestudios.autoskola.settings

import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.data.analytics.AnalyticsEvent
import com.spiraclestudios.autoskola.data.preferences.TestSelection
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.onboarding.ExamLabel
import com.spiraclestudios.autoskola.testing.TestRepositories
import com.spiraclestudios.autoskola.testing.ViewModelTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsViewModelTest : ViewModelTest() {

    private val repositories = TestRepositories().apply {
        preferences.select(TestSelection("sk-sk", LicenseCategory.C))
    }
    private val viewModel by lazy {
        SettingsViewModel(repositories.preferences, repositories.exams, repositories.analytics)
    }

    @Test
    fun theTwoSharingAnswersCanBeChangedHere() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.SetCrashReporting(true))

        assertEquals(true, state.value.crashReportingEnabled)
        assertEquals(false, state.value.analyticsEnabled)
        assertEquals(true, repositories.preferences.preferences.value.crashReportingEnabled)
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
}

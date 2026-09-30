package uno.lux.autoskola.settings

import kotlinx.coroutines.test.runTest
import uno.lux.autoskola.bank.LicenseCategory
import uno.lux.autoskola.data.preferences.TestSelection
import uno.lux.autoskola.i18n.AppLanguage
import uno.lux.autoskola.onboarding.ExamLabel
import uno.lux.autoskola.testing.TestRepositories
import uno.lux.autoskola.testing.ViewModelTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsViewModelTest : ViewModelTest() {

    private val repositories = TestRepositories().apply {
        preferences.select(TestSelection("sk-sk", LicenseCategory.C))
    }
    private val viewModel by lazy { SettingsViewModel(repositories.preferences, repositories.exams) }

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
}

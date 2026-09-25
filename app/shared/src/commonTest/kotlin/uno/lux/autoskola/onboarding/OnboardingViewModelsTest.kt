package uno.lux.autoskola.onboarding

import kotlinx.coroutines.test.runTest
import uno.lux.autoskola.bank.LicenseCategory
import uno.lux.autoskola.data.preferences.TestSelection
import uno.lux.autoskola.i18n.AppLanguage
import uno.lux.autoskola.testing.TestRepositories
import uno.lux.autoskola.testing.ViewModelTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DisclaimerViewModelTest : ViewModelTest() {

    private val repositories = TestRepositories()
    private val viewModel by lazy { DisclaimerViewModel(repositories.preferences) }

    @Test
    fun languageCanBeChangedBeforeAccepting() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(DisclaimerUiEvent.SelectLanguage(AppLanguage.Czech))

        assertEquals(AppLanguage.Czech, state.value.language)
        assertEquals(AppLanguage.Czech, repositories.preferences.preferences.value.language)
    }

    @Test
    fun acceptingIsRemembered() = runTest {
        viewModel.onEvent(DisclaimerUiEvent.Accept)

        assertTrue(repositories.preferences.preferences.value.disclaimerAccepted)
    }
}

class ExamSelectionViewModelTest : ViewModelTest() {

    private val repositories = TestRepositories()
    private val viewModel by lazy { ExamSelectionViewModel(repositories.preferences, repositories.banks) }

    @Test
    fun listsTheBanks() = runTest {
        assertEquals(listOf("sk-sk"), observe(viewModel.uiState).value.banks?.map { it.id })
    }

    @Test
    fun selectingSavesTheExamAndFinishes() = runTest {
        val state = observe(viewModel.uiState)
        assertFalse(state.value.done)

        viewModel.onEvent(ExamSelectionUiEvent.Select(TestSelection("sk-sk", LicenseCategory.C)))

        assertEquals(TestSelection("sk-sk", LicenseCategory.C), repositories.preferences.preferences.value.selection)
        assertEquals(TestSelection("sk-sk", LicenseCategory.C), state.value.current)
        assertTrue(state.value.done)
    }

    @Test
    fun languageCanBeChanged() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(ExamSelectionUiEvent.SelectLanguage(AppLanguage.Slovak))

        assertEquals(AppLanguage.Slovak, state.value.language)
    }
}

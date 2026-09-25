package uno.lux.autoskola

import kotlinx.coroutines.test.runTest
import uno.lux.autoskola.bank.LicenseCategory
import uno.lux.autoskola.data.preferences.TestSelection
import uno.lux.autoskola.i18n.AppLanguage
import uno.lux.autoskola.testing.TestRepositories
import uno.lux.autoskola.testing.ViewModelTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AppViewModelTest : ViewModelTest() {

    private val repositories = TestRepositories()
    private fun viewModel() = AppViewModel(repositories.preferences, repositories.exams)

    @Test
    fun disclaimerComesFirst() = runTest {
        val state = observe(viewModel().uiState)

        assertEquals(AppDestination.Disclaimer, state.value.destination)
    }

    @Test
    fun afterTheDisclaimerTheUserChoosesAnExam() = runTest {
        repositories.preferences.acceptDisclaimer()

        assertEquals(AppDestination.ChooseExam, observe(viewModel().uiState).value.destination)
    }

    @Test
    fun selectedExamOpens() = runTest {
        val state = observe(viewModel().uiState)
        repositories.preferences.acceptDisclaimer()
        repositories.preferences.select(TestSelection("sk-sk", LicenseCategory.B))

        assertEquals(AppDestination.Exam, state.value.destination)
    }

    @Test
    fun examWhoseBankIsGoneIsChosenAgain() = runTest {
        repositories.preferences.acceptDisclaimer()
        repositories.preferences.select(TestSelection("gone", LicenseCategory.B))

        assertEquals(AppDestination.ChooseExam, observe(viewModel().uiState).value.destination)
    }

    @Test
    fun languageFollowsPreferences() = runTest {
        val state = observe(viewModel().uiState)
        repositories.preferences.selectLanguage(AppLanguage.Hungarian)

        assertEquals(AppLanguage.Hungarian, state.value.language)
    }
}

package uno.lux.autoskola.learn

import kotlinx.coroutines.test.runTest
import uno.lux.autoskola.bank.LicenseCategory
import uno.lux.autoskola.data.banks.testSituationsBank
import uno.lux.autoskola.data.preferences.TestSelection
import uno.lux.autoskola.testing.TestRepositories
import uno.lux.autoskola.testing.ViewModelTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SituationViewModelTest : ViewModelTest() {

    private fun viewModel(
        number: Int,
        repositories: TestRepositories = TestRepositories(testSituationsBank()),
    ): SituationViewModel {
        repositories.preferences.select(TestSelection("sk-sk", LicenseCategory.B))
        return SituationViewModel(number, repositories.exams, repositories.banks)
    }

    @Test
    fun noExamSelectedLeavesTheScreenLoading() = runTest {
        val fresh = TestRepositories(testSituationsBank())

        val state = observe(SituationViewModel(1, fresh.exams, fresh.banks).uiState)

        assertEquals(SituationUiState.Loading, state.value)
    }

    @Test
    fun theSituationTheListNumberedIsTheOneThatOpens() = runTest {
        val state = observe(viewModel(number = 2).uiState)

        val ready = assertIs<SituationUiState.Ready>(state.value)
        assertEquals(2, ready.situation.number)
        assertEquals("test://minv-sk/obr3/ds/02.png", ready.situation.imageUri)
        assertEquals(listOf("Q1815"), ready.situation.questions.map { it.text })
    }

    @Test
    fun theFirstSituationCarriesEveryQuestionAskedAboutIt() = runTest {
        val state = observe(viewModel(number = 1).uiState)

        assertEquals(
            listOf("Q1813", "Q1814"),
            assertIs<SituationUiState.Ready>(state.value).situation.questions.map { it.text },
        )
    }

    @Test
    fun aSituationTheMaterialDoesNotHaveClosesTheScreen() = runTest {
        // A screen restored from a route the bank no longer answers shows nothing rather than
        // another situation's drawing.
        val state = observe(viewModel(number = 9).uiState)

        assertEquals(SituationUiState.Closed, state.value)
    }
}

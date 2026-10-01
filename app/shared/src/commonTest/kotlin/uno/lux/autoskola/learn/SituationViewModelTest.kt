package uno.lux.autoskola.learn

import kotlinx.coroutines.test.runTest
import uno.lux.autoskola.bank.LicenseCategory
import uno.lux.autoskola.data.banks.testSituationsBank
import uno.lux.autoskola.data.preferences.TestSelection
import uno.lux.autoskola.stats.AttemptKind
import uno.lux.autoskola.testing.TestRepositories
import uno.lux.autoskola.testing.ViewModelTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class SituationViewModelTest : ViewModelTest() {

    private fun viewModel(
        number: Int,
        repositories: TestRepositories = TestRepositories(testSituationsBank()),
    ): SituationViewModel {
        repositories.preferences.select(TestSelection("sk-sk", LicenseCategory.B))
        return SituationViewModel(number, repositories.exams, repositories.banks, repositories.preferences)
    }

    @Test
    fun noExamSelectedLeavesTheScreenLoading() = runTest {
        val fresh = TestRepositories(testSituationsBank())

        val state = observe(SituationViewModel(1, fresh.exams, fresh.banks, fresh.preferences).uiState)

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

    @Test
    fun drillingItOpensAnAttemptOverTheQuestionsAskedAboutIt() = runTest {
        val repositories = TestRepositories(testSituationsBank())
        val viewModel = viewModel(number = 1, repositories = repositories)
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SituationUiEvent.Drill)

        val attemptId = assertNotNull(assertIs<SituationUiState.Ready>(state.value).openAttemptId)
        val attempt = assertNotNull(repositories.attempts.get(attemptId))
        assertEquals(AttemptKind.Situations, attempt.kind)
        assertEquals(listOf(1813, 1814), attempt.questionIds)
    }

    @Test
    fun thereIsNothingToDrillWhereThereIsNoSituation() = runTest {
        val viewModel = viewModel(number = 9)
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SituationUiEvent.Drill)

        assertEquals(SituationUiState.Closed, state.value)
    }
}

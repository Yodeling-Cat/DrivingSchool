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
import kotlin.test.assertTrue

class SituationViewModelTest : ViewModelTest() {

    private fun viewModel(
        number: Int,
        repositories: TestRepositories = TestRepositories(testSituationsBank()),
    ): SituationViewModel {
        repositories.preferences.select(TestSelection("sk-sk", LicenseCategory.B))
        return SituationViewModel(
            number,
            repositories.exams,
            repositories.banks,
            repositories.preferences,
            repositories.answers,
        )
    }

    @Test
    fun noExamSelectedLeavesTheScreenLoading() = runTest {
        val fresh = TestRepositories(testSituationsBank())

        val state = observe(
            SituationViewModel(1, fresh.exams, fresh.banks, fresh.preferences, fresh.answers).uiState,
        )

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

    @Test
    fun nothingIsAnsweredUntilTheDriverAnswers() = runTest {
        val state = observe(viewModel(number = 1).uiState)

        assertTrue(assertIs<SituationUiState.Ready>(state.value).answers.isEmpty())
    }

    @Test
    fun answeringIsShownInTheState() = runTest {
        val viewModel = viewModel(number = 1)
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SituationUiEvent.Answer(questionId = 1813, chosen = 1))

        assertEquals(mapOf(1813 to 1), assertIs<SituationUiState.Ready>(state.value).answers)
    }

    @Test
    fun answeringAgainReplacesTheAnswer() = runTest {
        // Practice, not a test: an answer is never locked, so the driver can change it as often as
        // they like - and the screen follows what they last picked.
        val viewModel = viewModel(number = 1)
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SituationUiEvent.Answer(questionId = 1813, chosen = 1))
        viewModel.onEvent(SituationUiEvent.Answer(questionId = 1813, chosen = 0))

        assertEquals(mapOf(1813 to 0), assertIs<SituationUiState.Ready>(state.value).answers)
    }

    @Test
    fun whatTheDriverAnsweredIsThereTheNextTimeTheSituationIsOpened() = runTest {
        val repositories = TestRepositories(testSituationsBank())
        val first = viewModel(number = 1, repositories = repositories)
        observe(first.uiState)
        first.onEvent(SituationUiEvent.Answer(questionId = 1814, chosen = 2))

        val opened = observe(viewModel(number = 1, repositories = repositories).uiState)

        assertEquals(2, assertIs<SituationUiState.Ready>(opened.value).answers[1814])
    }
}

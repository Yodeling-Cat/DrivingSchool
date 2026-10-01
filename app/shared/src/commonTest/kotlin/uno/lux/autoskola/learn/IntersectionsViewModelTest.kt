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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntersectionsViewModelTest : ViewModelTest() {

    /** The exam's situations, with the licence group B selected. */
    private fun viewModel(
        repositories: TestRepositories = TestRepositories(testSituationsBank()),
    ): IntersectionsViewModel {
        repositories.preferences.select(TestSelection("sk-sk", LicenseCategory.B))
        return IntersectionsViewModel(repositories.exams, repositories.banks, repositories.preferences)
    }

    @Test
    fun noExamSelectedLeavesTheScreenLoading() = runTest {
        val fresh = TestRepositories(testSituationsBank())

        val state = observe(IntersectionsViewModel(fresh.exams, fresh.banks, fresh.preferences).uiState)

        assertEquals(IntersectionsUiState.Loading, state.value)
    }

    @Test
    fun theSituationsAreListedInTheBanksOrderWithTheirDrawings() = runTest {
        val state = observe(viewModel().uiState)

        val ready = assertIs<IntersectionsUiState.Ready>(state.value)
        assertEquals(listOf(1, 2), ready.situations.map { it.number })
        assertEquals(
            listOf("test://minv-sk/obr3/ds/01.png", "test://minv-sk/obr3/ds/02.png"),
            ready.situations.map { it.imageUri },
        )
        assertEquals(listOf(2, 1), ready.situations.map { it.questions.size })
        assertEquals(3, ready.questionCount)
    }

    @Test
    fun aQuestionIsCarriedWithItsAnswersAndTheAnswerThatIsRight() = runTest {
        val state = observe(viewModel().uiState)

        val question = assertIs<IntersectionsUiState.Ready>(state.value).situations[0].questions[0]

        assertEquals("Q1813", question.text)
        assertEquals(listOf("right", "wrong", "wrong"), question.answers)
        assertEquals(0, question.correct)
    }

    @Test
    fun onlyTheSituationsTheLicenceGroupIsAskedAreListed() = runTest {
        // The third drawing is asked of group A alone.
        val state = observe(viewModel().uiState)

        assertEquals(
            listOf("test://minv-sk/obr3/ds/01.png", "test://minv-sk/obr3/ds/02.png"),
            assertIs<IntersectionsUiState.Ready>(state.value).situations.map { it.imageUri },
        )
    }

    @Test
    fun anExamWhoseBankAsksAboutNoSituationsIsReadyAndEmpty() = runTest {
        // The bundled banks all ask about situations; a bank that doesn't still has to answer.
        val repositories = TestRepositories()
        repositories.preferences.select(TestSelection("sk-sk", LicenseCategory.B))

        val state = observe(
            IntersectionsViewModel(repositories.exams, repositories.banks, repositories.preferences).uiState,
        )

        val ready = assertIs<IntersectionsUiState.Ready>(state.value)
        assertTrue(ready.situations.isEmpty())
        assertEquals(0, ready.questionCount)
    }

    @Test
    fun drillingThemOpensOneAttemptOverTheirQuestions() = runTest {
        val repositories = TestRepositories(testSituationsBank())
        val viewModel = viewModel(repositories)
        val state = observe(viewModel.uiState)

        viewModel.onEvent(IntersectionsUiEvent.Drill)

        val attemptId = assertNotNull(assertIs<IntersectionsUiState.Ready>(state.value).openAttemptId)
        val attempt = assertNotNull(repositories.attempts.get(attemptId))
        assertEquals(AttemptKind.Situations, attempt.kind)
        // The three questions about the two situations group B is asked, in the bank's order.
        assertEquals(listOf(1813, 1814, 1815), attempt.questionIds)
    }

    @Test
    fun aDrillChecksTheAnswersAsTheDriverAsked() = runTest {
        // A drill is learning, so it goes by the same setting the practice screen does.
        val repositories = TestRepositories(testSituationsBank())
        repositories.preferences.setCheckAnswersImmediately(false)
        val viewModel = viewModel(repositories)
        val state = observe(viewModel.uiState)

        viewModel.onEvent(IntersectionsUiEvent.Drill)

        val attemptId = assertNotNull(assertIs<IntersectionsUiState.Ready>(state.value).openAttemptId)
        assertFalse(assertNotNull(repositories.attempts.get(attemptId)).checkImmediately)
    }

    @Test
    fun theScreenForgetsTheAttemptOnceItIsOpen() = runTest {
        // The screen navigates to the practice screen on the id; a second emission would navigate
        // again every time the state was recomposed.
        val viewModel = viewModel()
        val state = observe(viewModel.uiState)

        viewModel.onEvent(IntersectionsUiEvent.Drill)
        viewModel.onEvent(IntersectionsUiEvent.AttemptOpened)

        assertNull(assertIs<IntersectionsUiState.Ready>(state.value).openAttemptId)
    }
}

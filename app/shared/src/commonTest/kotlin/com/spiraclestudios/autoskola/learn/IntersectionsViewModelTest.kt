package com.spiraclestudios.autoskola.learn

import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.data.banks.testQuestion
import com.spiraclestudios.autoskola.data.banks.testSituationsBank
import com.spiraclestudios.autoskola.data.preferences.TestSelection
import com.spiraclestudios.autoskola.stats.AttemptKind
import com.spiraclestudios.autoskola.testing.TestRepositories
import com.spiraclestudios.autoskola.testing.ViewModelTest
import kotlinx.coroutines.test.runTest
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
        return IntersectionsViewModel(
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
            IntersectionsViewModel(fresh.exams, fresh.banks, fresh.preferences, fresh.answers).uiState,
        )

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
            IntersectionsViewModel(
                repositories.exams,
                repositories.banks,
                repositories.preferences,
                repositories.answers,
            ).uiState,
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
    fun drillingAgainAsksWhatToDoWithTheDrillUnderWay() = runTest {
        // Walking away from a drill and coming back to drill again: only the driver knows whether to
        // carry on where they were or start the whole run over, so the screen asks.
        val viewModel = viewModel()
        val state = observe(viewModel.uiState)
        viewModel.onEvent(IntersectionsUiEvent.Drill)
        val first = assertNotNull(assertIs<IntersectionsUiState.Ready>(state.value).openAttemptId)
        viewModel.onEvent(IntersectionsUiEvent.AttemptOpened)

        viewModel.onEvent(IntersectionsUiEvent.Drill)

        val ready = assertIs<IntersectionsUiState.Ready>(state.value)
        val prompt = assertNotNull(ready.drillPrompt)
        assertEquals(first, prompt.attemptId)
        assertEquals(3, prompt.total)
        // Nothing is opened until the driver answers: the drill sits where it was.
        assertNull(ready.openAttemptId)
    }

    @Test
    fun thePromptSaysHowMuchOfTheDrillIsAnswered() = runTest {
        val repositories = TestRepositories(testSituationsBank())
        val viewModel = viewModel(repositories)
        val state = observe(viewModel.uiState)
        viewModel.onEvent(IntersectionsUiEvent.Drill)
        val drill = assertNotNull(assertIs<IntersectionsUiState.Ready>(state.value).openAttemptId)
        viewModel.onEvent(IntersectionsUiEvent.AttemptOpened)
        repositories.attempts.answer(drill, question = testQuestion(1813, section = 4), chosen = 0)

        viewModel.onEvent(IntersectionsUiEvent.Drill)

        val prompt = assertNotNull(assertIs<IntersectionsUiState.Ready>(state.value).drillPrompt)
        assertEquals(1, prompt.answered)
        assertEquals(3, prompt.total)
    }

    @Test
    fun carryingOnOpensTheDrillWhereItWasLeft() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel.uiState)
        viewModel.onEvent(IntersectionsUiEvent.Drill)
        val drill = assertNotNull(assertIs<IntersectionsUiState.Ready>(state.value).openAttemptId)
        viewModel.onEvent(IntersectionsUiEvent.AttemptOpened)
        viewModel.onEvent(IntersectionsUiEvent.Drill)

        viewModel.onEvent(IntersectionsUiEvent.ContinueDrill)

        val ready = assertIs<IntersectionsUiState.Ready>(state.value)
        assertEquals(drill, ready.openAttemptId)
        assertNull(ready.drillPrompt)
    }

    @Test
    fun beginningAgainThrowsTheDrillAwayAndStartsAFreshOne() = runTest {
        val repositories = TestRepositories(testSituationsBank())
        val viewModel = viewModel(repositories)
        val state = observe(viewModel.uiState)
        viewModel.onEvent(IntersectionsUiEvent.Drill)
        val first = assertNotNull(assertIs<IntersectionsUiState.Ready>(state.value).openAttemptId)
        viewModel.onEvent(IntersectionsUiEvent.AttemptOpened)
        repositories.attempts.answer(first, question = testQuestion(1813, section = 4), chosen = 0)
        viewModel.onEvent(IntersectionsUiEvent.Drill)

        viewModel.onEvent(IntersectionsUiEvent.StartOverDrill)

        val ready = assertIs<IntersectionsUiState.Ready>(state.value)
        val fresh = assertNotNull(ready.openAttemptId)
        assertTrue(fresh != first)
        assertNull(ready.drillPrompt)
        // The drill that was under way is gone, answers and all.
        assertTrue(assertNotNull(repositories.attempts.get(first)).discardedAt != null)
    }

    @Test
    fun closingThePromptLeavesTheDrillAsItWas() = runTest {
        val repositories = TestRepositories(testSituationsBank())
        val viewModel = viewModel(repositories)
        val state = observe(viewModel.uiState)
        viewModel.onEvent(IntersectionsUiEvent.Drill)
        val drill = assertNotNull(assertIs<IntersectionsUiState.Ready>(state.value).openAttemptId)
        viewModel.onEvent(IntersectionsUiEvent.AttemptOpened)
        viewModel.onEvent(IntersectionsUiEvent.Drill)

        viewModel.onEvent(IntersectionsUiEvent.DismissDrillPrompt)

        val ready = assertIs<IntersectionsUiState.Ready>(state.value)
        assertNull(ready.drillPrompt)
        assertNull(ready.openAttemptId)
        assertNull(assertNotNull(repositories.attempts.get(drill)).discardedAt)
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

    @Test
    fun aSituationEveryQuestionOfWhichIsAnsweredRightIsMarkedAsOneToSkip() = runTest {
        val repositories = TestRepositories(testSituationsBank())
        // The first situation's two questions, both right.
        repositories.answers.answer("sk-sk", 1, questionId = 1813, chosen = 0)
        repositories.answers.answer("sk-sk", 1, questionId = 1814, chosen = 0)

        val state = observe(viewModel(repositories).uiState)

        assertEquals(
            listOf(true, false),
            assertIs<IntersectionsUiState.Ready>(state.value).situations.map {
                it.solved
            },
        )
    }

    @Test
    fun aSituationWithOneQuestionLeftWrongIsNotMarked() = runTest {
        val repositories = TestRepositories(testSituationsBank())
        repositories.answers.answer("sk-sk", 1, questionId = 1813, chosen = 0)
        repositories.answers.answer("sk-sk", 1, questionId = 1814, chosen = 1)

        val state = observe(viewModel(repositories).uiState)

        assertFalse(assertIs<IntersectionsUiState.Ready>(state.value).situations.first().solved)
    }

    @Test
    fun theMarkAppearsAsSoonAsTheLastAnswerIsRight() = runTest {
        val repositories = TestRepositories(testSituationsBank())
        repositories.answers.answer("sk-sk", 1, questionId = 1813, chosen = 0)
        repositories.answers.answer("sk-sk", 1, questionId = 1814, chosen = 1)
        val state = observe(viewModel(repositories).uiState)

        repositories.answers.answer("sk-sk", 1, questionId = 1814, chosen = 0)

        assertTrue(assertIs<IntersectionsUiState.Ready>(state.value).situations.first().solved)
    }
}

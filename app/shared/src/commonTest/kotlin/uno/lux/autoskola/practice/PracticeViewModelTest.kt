package uno.lux.autoskola.practice

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import uno.lux.autoskola.bank.LicenseCategory
import uno.lux.autoskola.data.preferences.TestSelection
import uno.lux.autoskola.stats.TestAttempt
import uno.lux.autoskola.testing.TestRepositories
import uno.lux.autoskola.testing.ViewModelTest
import uno.lux.autoskola.testing.currentSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PracticeViewModelTest : ViewModelTest() {

    private val repositories = TestRepositories().apply {
        preferences.select(TestSelection("sk-sk", LicenseCategory.B))
    }

    private suspend fun startTest(checkImmediately: Boolean = false): TestAttempt {
        val session = repositories.exams.currentSession()
        return session.startOrResume(session.tests.first(), checkImmediately)
    }

    private fun TestScope.open(attempt: TestAttempt): Pair<PracticeViewModel, StateFlow<PracticeUiState>> {
        val viewModel = PracticeViewModel(attempt.id, repositories.exams, repositories.banks)
        return viewModel to observe(viewModel.uiState)
    }

    private val StateFlow<PracticeUiState>.ready get() = assertIs<PracticeUiState.Ready>(value)

    @Test
    fun showsTheAttemptsQuestionsInOrder() = runTest {
        val attempt = startTest(checkImmediately = true)

        val state = open(attempt).second.ready

        assertEquals(attempt.questionIds, state.questions.map { it.question.id })
        assertEquals(1, state.testNumber)
        assertEquals(true, state.checkImmediately)
        assertEquals(emptyMap(), state.answers)
        assertEquals(0, state.initialPage)
    }

    @Test
    fun questionsWithImagesHaveImageUris() = runTest {
        val attempt = startTest()

        val state = open(attempt).second.ready

        // Only question 1 of the test bank has an image.
        val withImages = state.questions.filter { it.question.image != null }
        assertEquals(withImages.map { "test://minv-sk/${it.question.image}" }, withImages.map { it.imageUri })
        assertEquals(listOf(null), state.questions.filter { it.question.image == null }.map { it.imageUri }.distinct())
    }

    @Test
    fun resumesAtTheFirstUnansweredQuestion() = runTest {
        val attempt = startTest()
        val session = repositories.exams.currentSession()
        session.answer(attempt.id, session.questionsOf(attempt)[0], chosen = 2)

        val state = open(attempt).second.ready

        assertEquals(mapOf(0 to 2), state.answers)
        assertEquals(1, state.initialPage)
    }

    @Test
    fun answersAreSavedAndShown() = runTest {
        val attempt = startTest()
        val (viewModel, state) = open(attempt)

        viewModel.onEvent(PracticeUiEvent.Answer(questionIndex = 1, answerIndex = 0))
        viewModel.onEvent(PracticeUiEvent.Answer(questionIndex = 1, answerIndex = 2))

        assertEquals(mapOf(1 to 2), state.ready.answers)
        assertEquals(1, repositories.attempts.get(attempt.id)!!.answers.size)
        assertEquals(0, state.ready.initialPage) // fixed when the test opened
    }

    @Test
    fun finishingScoresTheTestAndOpensTheResults() = runTest {
        val attempt = startTest()
        val (viewModel, state) = open(attempt)
        viewModel.onEvent(PracticeUiEvent.Answer(questionIndex = 0, answerIndex = 0))

        viewModel.onEvent(PracticeUiEvent.Finish)

        assertTrue(state.ready.finished)
        assertTrue(state.ready.openResults)
        assertNotNull(repositories.attempts.get(attempt.id)!!.result)

        viewModel.onEvent(PracticeUiEvent.ResultsOpened)
        assertFalse(state.ready.openResults)
        assertTrue(state.ready.finished)
    }

    @Test
    fun allAnsweredOnceEveryQuestionHasAnAnswer() = runTest {
        val attempt = startTest()
        val (viewModel, state) = open(attempt)
        assertFalse(state.ready.allAnswered)

        state.ready.questions.indices.forEach { viewModel.onEvent(PracticeUiEvent.Answer(it, answerIndex = 0)) }

        assertTrue(state.ready.allAnswered)
        assertFalse(state.ready.finished)
    }

    @Test
    fun finishedAttemptOpensForReviewWithItsAnswers() = runTest {
        val attempt = startTest()
        val session = repositories.exams.currentSession()
        session.answer(attempt.id, session.questionsOf(attempt)[1], chosen = 2)
        session.finish(attempt.id)

        val (viewModel, state) = open(attempt)

        assertTrue(state.ready.finished)
        assertFalse(state.ready.openResults)
        assertEquals(mapOf(1 to 2), state.ready.answers)

        viewModel.onEvent(PracticeUiEvent.Answer(questionIndex = 1, answerIndex = 0))
        assertEquals(mapOf(1 to 2), state.ready.answers) // locked once finished
    }

    @Test
    fun attemptThatIsNotOpenIsClosed() = runTest {
        val attempt = startTest()
        repositories.attempts.discard(attempt.id)

        assertEquals(PracticeUiState.Closed, open(attempt).second.value)
    }
}

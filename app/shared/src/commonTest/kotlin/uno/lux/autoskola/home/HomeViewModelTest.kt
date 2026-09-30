package uno.lux.autoskola.home

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import uno.lux.autoskola.bank.LicenseCategory
import uno.lux.autoskola.data.preferences.TestSelection
import uno.lux.autoskola.stats.AttemptKind
import uno.lux.autoskola.testing.TestRepositories
import uno.lux.autoskola.testing.ViewModelTest
import uno.lux.autoskola.testing.currentSession
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeViewModelTest : ViewModelTest() {

    private val repositories = TestRepositories().apply {
        preferences.select(TestSelection("sk-sk", LicenseCategory.B))
    }
    private val viewModel by lazy { HomeViewModel(repositories.exams, repositories.preferences, Random(42)) }

    private val StateFlow<HomeUiState>.ready get() = assertIs<HomeUiState.Ready>(value)

    @Test
    fun showsTheSelectedExam() = runTest {
        val state = observe(viewModel.uiState).ready

        assertEquals(LicenseCategory.B, state.exam.category)
        assertEquals("sk", state.exam.bankLanguage)
        assertEquals(listOf(1, 2), state.tests.map { it.number })
        assertEquals(4, state.progress.questionPoolSize)
        assertNull(state.progress.resumable)
    }

    @Test
    fun startingATestOpensItOnce() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 2))

        val attempt = assertNotNull(repositories.attempts.get(assertNotNull(state.ready.openAttemptId)))
        assertEquals(2, attempt.testNumber)
        assertEquals(setOf(2), state.ready.progress.inProgress)

        viewModel.onEvent(HomeUiEvent.AttemptOpened)
        assertNull(state.ready.openAttemptId)
    }

    @Test
    fun newTestsUseTheCheckAnswersSetting() = runTest {
        val state = observe(viewModel.uiState)
        repositories.preferences.setCheckAnswersImmediately(true)

        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 1))

        assertTrue(repositories.attempts.get(state.ready.openAttemptId!!)!!.checkImmediately)
    }

    @Test
    fun unfinishedTestCanBeResumed() = runTest {
        val state = observe(viewModel.uiState)
        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 1))
        val started = state.ready.openAttemptId!!
        viewModel.onEvent(HomeUiEvent.AttemptOpened)

        val resumable = assertNotNull(state.ready.progress.resumable)
        assertEquals(
            ResumableTest(started, AttemptKind.PracticeTest, testNumber = 1, answered = 0, total = 3),
            resumable,
        )
        viewModel.onEvent(HomeUiEvent.Resume)
        assertEquals(started, state.ready.openAttemptId)
    }

    @Test
    fun pickingATestUnderWayAsksInsteadOfContinuingIt() = runTest {
        val state = observe(viewModel.uiState)
        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 1))
        val started = state.ready.openAttemptId!!
        viewModel.onEvent(HomeUiEvent.AttemptOpened)

        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 1))

        assertNull(state.ready.openAttemptId)
        assertEquals(
            ResumePrompt(started, testNumber = 1, answered = 0, total = 3),
            state.ready.resumePrompt,
        )
    }

    @Test
    fun continuingOpensTheAttemptThatWasAlreadyUnderWay() = runTest {
        val state = observe(viewModel.uiState)
        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 1))
        val started = state.ready.openAttemptId!!
        viewModel.onEvent(HomeUiEvent.AttemptOpened)
        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 1))

        viewModel.onEvent(HomeUiEvent.ContinueTest)

        assertEquals(started, state.ready.openAttemptId)
        assertNull(state.ready.resumePrompt)
    }

    @Test
    fun startingOverDiscardsTheUnfinishedAttemptAndStartsAFreshOne() = runTest {
        val state = observe(viewModel.uiState)
        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 1))
        val started = state.ready.openAttemptId!!
        viewModel.onEvent(HomeUiEvent.AttemptOpened)
        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 1))

        viewModel.onEvent(HomeUiEvent.StartOver)

        assertNull(state.ready.resumePrompt)
        val restarted = assertNotNull(repositories.attempts.get(assertNotNull(state.ready.openAttemptId)))
        assertEquals(1, restarted.testNumber)
        assertTrue(restarted.answers.isEmpty())
        assertNotNull(repositories.attempts.get(started)!!.discardedAt)
    }

    @Test
    fun dismissingThePromptLeavesTheTestUnderWay() = runTest {
        val state = observe(viewModel.uiState)
        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 1))
        val started = state.ready.openAttemptId!!
        viewModel.onEvent(HomeUiEvent.AttemptOpened)
        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 1))

        viewModel.onEvent(HomeUiEvent.DismissResumePrompt)

        assertNull(state.ready.resumePrompt)
        assertNull(state.ready.openAttemptId)
        assertNull(repositories.attempts.get(started)!!.discardedAt)
    }

    @Test
    fun practisingMistakesOpensARetestOnlyWhenThereAreMistakes() = runTest {
        val state = observe(viewModel.uiState)
        viewModel.onEvent(HomeUiEvent.PracticeMistakes)
        assertNull(state.ready.openAttemptId)

        // Get every question of test 1 wrong.
        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 1))
        val attempt = repositories.attempts.get(state.ready.openAttemptId!!)!!
        val session = repositories.exams.currentSession()
        session.questionsOf(attempt).forEach { session.answer(attempt.id, it, chosen = 1) }
        session.finish(attempt.id)
        viewModel.onEvent(HomeUiEvent.AttemptOpened)

        viewModel.onEvent(HomeUiEvent.PracticeMistakes)

        val retest = repositories.attempts.get(state.ready.openAttemptId!!)!!
        assertEquals(AttemptKind.Mistakes, retest.kind)
        assertEquals(attempt.questionIds.toSet(), retest.questionIds.toSet())
    }

    @Test
    fun randomTestCanBeLimitedToTestsNotDoneYet() = runTest {
        val state = observe(viewModel.uiState)
        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 1))
        viewModel.onEvent(HomeUiEvent.AttemptOpened)

        viewModel.onEvent(HomeUiEvent.StartRandomTest(notDoneYet = true))
        assertEquals(2, repositories.attempts.get(state.ready.openAttemptId!!)!!.testNumber)
        viewModel.onEvent(HomeUiEvent.AttemptOpened)

        // Both tests are under way now, so there's nothing left to pick.
        viewModel.onEvent(HomeUiEvent.StartRandomTest(notDoneYet = true))
        assertNull(state.ready.openAttemptId)
    }
}

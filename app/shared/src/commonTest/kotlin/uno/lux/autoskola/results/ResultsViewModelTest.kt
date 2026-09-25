package uno.lux.autoskola.results

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import uno.lux.autoskola.TestResult
import uno.lux.autoskola.bank.LicenseCategory
import uno.lux.autoskola.data.preferences.TestSelection
import uno.lux.autoskola.stats.TestAttempt
import uno.lux.autoskola.testing.TestRepositories
import uno.lux.autoskola.testing.ViewModelTest
import uno.lux.autoskola.testing.currentSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ResultsViewModelTest : ViewModelTest() {

    private val repositories = TestRepositories().apply {
        preferences.select(TestSelection("sk-sk", LicenseCategory.B))
    }

    private suspend fun startTest(): TestAttempt {
        val session = repositories.exams.currentSession()
        return session.startOrResume(session.tests.first(), checkImmediately = false)
    }

    private fun TestScope.open(attempt: TestAttempt): StateFlow<ResultsUiState> =
        observe(ResultsViewModel(attempt.id, repositories.exams).uiState)

    @Test
    fun summarisesAFinishedAttempt() = runTest {
        val attempt = startTest()
        val session = repositories.exams.currentSession()
        val questions = session.questionsOf(attempt)
        // The first test has two questions from section 1 (1 point each) and one from section 2 (2 points).
        session.answer(attempt.id, questions[0], chosen = 0)
        session.answer(attempt.id, questions[1], chosen = 1)
        session.finish(attempt.id)

        val state = assertIs<ResultsUiState.Ready>(open(attempt).value)

        assertEquals(1, state.testNumber)
        assertEquals(TestResult(score = 1, maxScore = 4), state.summary.result)
        assertEquals(1, state.summary.correct)
        assertEquals(1, state.summary.wrong)
        assertEquals(1, state.summary.unanswered)
        assertEquals(listOf("One", "Two"), state.sections.map { it.title })
        assertEquals(listOf(1, 0), state.sections.map { it.summary.correct })
    }

    @Test
    fun unfinishedAttemptHasNoResults() = runTest {
        val attempt = startTest()

        assertEquals(ResultsUiState.Closed, open(attempt).value)
    }

    @Test
    fun discardedAttemptHasNoResults() = runTest {
        val attempt = startTest()
        repositories.attempts.discard(attempt.id)

        assertEquals(ResultsUiState.Closed, open(attempt).value)
    }
}

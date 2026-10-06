package com.spiraclestudios.autoskola.results

import com.spiraclestudios.autoskola.TestResult
import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.data.ads.AdSlot
import com.spiraclestudios.autoskola.data.banks.testSituationsBank
import com.spiraclestudios.autoskola.data.preferences.TestSelection
import com.spiraclestudios.autoskola.premium.PremiumStatus
import com.spiraclestudios.autoskola.premium.PurchaseSource
import com.spiraclestudios.autoskola.stats.TestAttempt
import com.spiraclestudios.autoskola.testing.TestRepositories
import com.spiraclestudios.autoskola.testing.ViewModelTest
import com.spiraclestudios.autoskola.testing.currentSession
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ResultsViewModelTest : ViewModelTest() {

    private val repositories = TestRepositories().apply {
        preferences.select(TestSelection("sk-sk", LicenseCategory.B))
    }

    private suspend fun startTest(): TestAttempt {
        val session = repositories.exams.currentSession()
        return session.startOrResume(session.tests.first())
    }

    private fun TestScope.open(attempt: TestAttempt): StateFlow<ResultsUiState> =
        observe(ResultsViewModel(attempt.id, repositories.exams, repositories.ads).uiState)

    @Test
    fun summarizesAFinishedAttempt() = runTest {
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
    fun aFinishedAttemptCarriesHowLongItTook() = runTest {
        val attempt = startTest()
        val session = repositories.exams.currentSession()
        session.finish(attempt.id, elapsedSeconds = 1_403L)

        val state = assertIs<ResultsUiState.Ready>(open(attempt).value)

        assertEquals(1_403L, state.elapsedSeconds)
    }

    @Test
    fun aTestsResultsAreGradedAgainstThePassMark() = runTest {
        val attempt = startTest()
        repositories.exams.currentSession().finish(attempt.id)

        val state = assertIs<ResultsUiState.Ready>(open(attempt).value)

        assertTrue(state.hasVerdict)
    }

    @Test
    fun aSituationsDrillsResultsAreNotGraded() = runTest {
        // Reading the situations is not an exam, so its card says what was right, not whether the
        // driver passed: there is nothing they were being measured against.
        val drills = TestRepositories(testSituationsBank()).apply {
            preferences.select(TestSelection("sk-sk", LicenseCategory.B))
        }
        val session = drills.exams.currentSession()
        val drill = assertNotNull(session.startSituations(session.bank.questions))
        session.finish(drill.id)

        val state = assertIs<ResultsUiState.Ready>(
            observe(ResultsViewModel(drill.id, drills.exams, drills.ads).uiState).value,
        )

        assertFalse(state.hasVerdict)
    }

    @Test
    fun dismissingTheResultShowsTheClosingAdAndThenLetsTheUserLeave() = runTest {
        val attempt = startTest()
        repositories.exams.currentSession().finish(attempt.id)
        val viewModel = ResultsViewModel(attempt.id, repositories.exams, repositories.ads)
        val state = observe(viewModel.uiState)
        assertFalse(assertIs<ResultsUiState.Ready>(state.value).leaving)

        viewModel.onEvent(ResultsUiEvent.Done)

        assertEquals(listOf(AdSlot.ResultsInterstitial), repositories.adDataSource.shown)
        // The leaving flag is what the route navigates on, so the user only goes once the ad is over.
        assertTrue(assertIs<ResultsUiState.Ready>(state.value).leaving)
    }

    @Test
    fun theClosingAdGivesWayToAGateThatIsAlreadyDue() = runTest {
        // Nine practice tests started, so the driver's next tap is the gated one. Two fullscreen ads
        // on one trip through the app is what the rule avoids, and the gate is the one worth paying
        // for: this screen's ad yields rather than stacking with it.
        val session = repositories.exams.currentSession()
        val attempts = (0 until 9).map { index ->
            session.restart(session.tests[index % session.tests.size]).also { session.finish(it.id) }
        }

        val viewModel = ResultsViewModel(attempts.last().id, repositories.exams, repositories.ads)
        val state = observe(viewModel.uiState)
        viewModel.onEvent(ResultsUiEvent.Done)

        assertEquals(emptyList(), repositories.adDataSource.shown)
        // Yielding is not being stranded: the user still leaves.
        assertTrue(assertIs<ResultsUiState.Ready>(state.value).leaving)
    }

    @Test
    fun aDriverWhoHasPaidForTheAdsToStopLeavesWithoutWaitingForOne() = runTest {
        val attempt = startTest()
        repositories.exams.currentSession().finish(attempt.id)
        repositories.premium.update(PremiumStatus.Premium(PurchaseSource.Play))

        val viewModel = ResultsViewModel(attempt.id, repositories.exams, repositories.ads)
        val state = observe(viewModel.uiState)
        viewModel.onEvent(ResultsUiEvent.Done)

        assertEquals(emptyList(), repositories.adDataSource.shown)
        assertTrue(assertIs<ResultsUiState.Ready>(state.value).leaving)
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

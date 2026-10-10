package com.spiraclestudios.autoskola.home

import com.spiraclestudios.autoskola.StoreListing
import com.spiraclestudios.autoskola.TestResult
import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.data.ads.AdSlot
import com.spiraclestudios.autoskola.data.analytics.AnalyticsEvent
import com.spiraclestudios.autoskola.data.preferences.TestSelection
import com.spiraclestudios.autoskola.premium.PremiumStatus
import com.spiraclestudios.autoskola.premium.PurchaseSource
import com.spiraclestudios.autoskola.stats.AttemptKind
import com.spiraclestudios.autoskola.testing.TestRepositories
import com.spiraclestudios.autoskola.testing.ViewModelTest
import com.spiraclestudios.autoskola.testing.currentSession
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HomeViewModelTest : ViewModelTest() {

    private val repositories = TestRepositories().apply {
        preferences.select(TestSelection("sk-sk", LicenseCategory.B))
    }

    /** The store the rating card sends the user to, which the host platform need not have one of. */
    private val store = StoreListing(name = "Play Store", url = "https://example.test/app")

    private val viewModel by lazy {
        HomeViewModel(
            repositories.exams,
            repositories.analytics,
            repositories.review,
            repositories.ads,
            store,
            Random(42),
        )
    }

    private val StateFlow<HomeUiState>.ready get() = assertIs<HomeUiState.Ready>(value)

    /**
     * Starts and finishes [count] practice tests, which is what the gate counts its frequency in.
     * With [wrong], each one answers its first question wrongly, so there is something to retest.
     */
    private suspend fun practiceTestsStarted(count: Int, wrong: Boolean = false) {
        val session = repositories.exams.currentSession()
        repeat(count) { index ->
            val attempt = session.restart(session.tests[index % session.tests.size])
            if (wrong) {
                val question = session.questionsOf(attempt).first()
                session.answer(attempt.id, question, chosen = if (question.correct == 0) 1 else 0)
            }
            session.finish(attempt.id)
        }
    }

    // ── The every-tenth-test gate ────────────────────────────────────────────

    @Test
    fun theTenthTestAsksBeforeItStarts() = runTest {
        practiceTestsStarted(9)
        val state = observe(viewModel.uiState)
        val test = state.ready.tests.first()

        viewModel.onEvent(HomeUiEvent.StartTest(test.number))

        assertEquals(test.number, state.ready.pendingTestNumber)
        // Nothing is under way: the gate is the door, and this is the user standing in front of it.
        assertNull(state.ready.openAttemptId)
        assertEquals(emptyList(), repositories.adDataSource.shown)
    }

    @Test
    fun aRandomTestIsGatedJustTheSame() = runTest {
        // The random button picks the test, so what the gate holds is the test it picked rather than
        // the fact that a button was pressed.
        practiceTestsStarted(9)
        val state = observe(viewModel.uiState)

        viewModel.onEvent(HomeUiEvent.StartRandomTest(notPassed = true))

        assertNotNull(state.ready.pendingTestNumber)
        assertTrue(state.ready.pendingTestNumber in state.ready.tests.map { it.number })
        assertNull(state.ready.openAttemptId)
    }

    @Test
    fun dismissingTheGateLeavesTheTestUnstarted() = runTest {
        practiceTestsStarted(9)
        val state = observe(viewModel.uiState)
        val test = state.ready.tests.first()
        viewModel.onEvent(HomeUiEvent.StartTest(test.number))

        viewModel.onEvent(HomeUiEvent.DismissAdGate)

        assertNull(state.ready.pendingTestNumber)
        assertNull(state.ready.openAttemptId)
        assertEquals(emptyList(), repositories.adDataSource.shown)
    }

    @Test
    fun askingAgainAsksAgain() = runTest {
        // Dismissing writes nothing, so the offer is still due and the next tap is asked about again
        // rather than quietly starting the test.
        practiceTestsStarted(9)
        val state = observe(viewModel.uiState)
        val test = state.ready.tests.first()
        viewModel.onEvent(HomeUiEvent.StartTest(test.number))
        viewModel.onEvent(HomeUiEvent.DismissAdGate)

        viewModel.onEvent(HomeUiEvent.StartTest(test.number))

        assertEquals(test.number, state.ready.pendingTestNumber)
    }

    @Test
    fun watchingTheAdStartsTheTestThatWasHeld() = runTest {
        practiceTestsStarted(9)
        val state = observe(viewModel.uiState)
        val test = state.ready.tests.first()
        viewModel.onEvent(HomeUiEvent.StartTest(test.number))

        viewModel.onEvent(HomeUiEvent.WatchAd)

        assertEquals(listOf(AdSlot.TestOfferInterstitial), repositories.adDataSource.shown)
        assertNull(state.ready.pendingTestNumber)
        assertNotNull(state.ready.openAttemptId)
    }

    @Test
    fun nothingIsHeldWhenNoAdCanBeAskedFor() = runTest {
        // Premium bought, so there is no gate: the test simply starts, and the user is never stood in
        // front of a dialog about an ad the app is not allowed to request.
        practiceTestsStarted(9)
        repositories.premium.update(PremiumStatus.Premium(PurchaseSource.Play))
        val state = observe(viewModel.uiState)
        val test = state.ready.tests.first()

        viewModel.onEvent(HomeUiEvent.StartTest(test.number))

        assertNull(state.ready.pendingTestNumber)
        assertNotNull(state.ready.openAttemptId)
    }

    @Test
    fun aTestBeforeTheTenthIsNotGated() = runTest {
        practiceTestsStarted(8)
        val state = observe(viewModel.uiState)
        val test = state.ready.tests.first()

        viewModel.onEvent(HomeUiEvent.StartTest(test.number))

        assertNull(state.ready.pendingTestNumber)
        assertNotNull(state.ready.openAttemptId)
    }

    @Test
    fun theDrillsAreNotGated() = runTest {
        // A repeat of the questions the driver got wrong is a short drill started on impulse, and it
        // is not what the gate is for - even with the offer due.
        practiceTestsStarted(9, wrong = true)
        val state = observe(viewModel.uiState)

        viewModel.onEvent(HomeUiEvent.PracticeMistakes)

        assertNull(state.ready.pendingTestNumber)
        assertNotNull(state.ready.openAttemptId)
    }

    @Test
    fun theHeldTestIsForgottenOnceItIsUnderWay() = runTest {
        practiceTestsStarted(9)
        val state = observe(viewModel.uiState)
        val test = state.ready.tests.first()
        viewModel.onEvent(HomeUiEvent.StartTest(test.number))
        viewModel.onEvent(HomeUiEvent.WatchAd)

        viewModel.onEvent(HomeUiEvent.AttemptOpened)

        assertNull(state.ready.pendingTestNumber)
        assertNull(state.ready.openAttemptId)
    }

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
    fun aPromptComingAndGoingKeepsTheProgressInstance() = runTest {
        // The tiles and the license card take the progress; a new but equal one would redraw them
        // every time a dialog opens or closes.
        val state = observe(viewModel.uiState)
        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 1))
        viewModel.onEvent(HomeUiEvent.AttemptOpened)
        val before = state.ready.progress

        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 1))
        assertNotNull(state.ready.resumePrompt)
        viewModel.onEvent(HomeUiEvent.DismissResumePrompt)

        assertSame(before, state.ready.progress)
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

    // ── A test's history ─────────────────────────────────────────────────────

    /** Takes test [testNumber] once and finishes it, then starts it again and leaves it under way. */
    private suspend fun finishedThenUnderWay(testNumber: Int): Pair<String, String> {
        val session = repositories.exams.currentSession()
        val test = session.tests.first { it.number == testNumber }
        val finished = session.restart(test)
        session.finish(finished.id)
        val underWay = session.restart(test)
        session.answer(underWay.id, session.questionsOf(underWay).first(), chosen = 0)
        return finished.id to underWay.id
    }

    @Test
    fun showingATestsHistoryListsItsTakesNewestFirst() = runTest {
        val (finished, underWay) = finishedThenUnderWay(testNumber = 1)
        val state = observe(viewModel.uiState)

        viewModel.onEvent(HomeUiEvent.ShowHistory(testNumber = 1))

        val history = assertNotNull(state.ready.history)
        assertEquals(1, history.testNumber)
        assertEquals(listOf(underWay, finished), history.entries.map { it.attemptId })
        assertEquals(listOf(2, 1), history.entries.map { it.take })
        assertEquals(listOf(true, false), history.entries.map { it.resumable })
        assertEquals(1, history.entries.first().answered)
        assertNotNull(history.entries.last().result)
    }

    @Test
    fun aTestNeverTakenHasAnEmptyHistory() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(HomeUiEvent.ShowHistory(testNumber = 1))

        assertEquals(emptyList(), assertNotNull(state.ready.history).entries)
    }

    @Test
    fun dismissingTheHistoryClosesIt() = runTest {
        val state = observe(viewModel.uiState)
        viewModel.onEvent(HomeUiEvent.ShowHistory(testNumber = 1))

        viewModel.onEvent(HomeUiEvent.DismissHistory)

        assertNull(state.ready.history)
    }

    @Test
    fun openingAFinishedTakeOpensItForReviewWithoutRecordingAStart() = runTest {
        // A finished take is only looked at again: the practice screen shows it locked, and nothing
        // was started or continued, so nothing is recorded as such.
        repositories.preferences.setAnalyticsEnabled(true)
        val (finished, _) = finishedThenUnderWay(testNumber = 1)
        val state = observe(viewModel.uiState)
        viewModel.onEvent(HomeUiEvent.ShowHistory(testNumber = 1))
        repositories.analyticsSink.events.clear()

        viewModel.onEvent(HomeUiEvent.OpenHistoryEntry(finished))

        assertEquals(finished, state.ready.openAttemptId)
        assertNull(state.ready.history)
        assertEquals(emptyList(), repositories.analyticsSink.events)
        // Opening it is not taking the test again: the take under way is still the one to continue.
        assertNotNull(repositories.exams.currentSession().openAttemptFor(state.ready.tests.first()))
    }

    @Test
    fun openingTheTakeUnderWayContinuesIt() = runTest {
        repositories.preferences.setAnalyticsEnabled(true)
        val (_, underWay) = finishedThenUnderWay(testNumber = 1)
        val state = observe(viewModel.uiState)
        viewModel.onEvent(HomeUiEvent.ShowHistory(testNumber = 1))
        repositories.analyticsSink.events.clear()
        repositories.analyticsSink.properties.clear()

        viewModel.onEvent(HomeUiEvent.OpenHistoryEntry(underWay))

        assertEquals(underWay, state.ready.openAttemptId)
        assertNull(state.ready.history)
        assertEquals(listOf(AnalyticsEvent.TestStarted), repositories.analyticsSink.events)
        assertEquals(
            mapOf("kind" to "practice_test", "resumed" to true),
            repositories.analyticsSink.properties.single(),
        )
    }

    @Test
    fun anAttemptNotInTheHistoryIsNotOpened() = runTest {
        val state = observe(viewModel.uiState)
        viewModel.onEvent(HomeUiEvent.ShowHistory(testNumber = 1))

        viewModel.onEvent(HomeUiEvent.OpenHistoryEntry("not-listed"))

        assertNull(state.ready.openAttemptId)
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
    fun notPassedStaysLiveUntilEveryTestIsPassed() = runTest {
        val state = observe(viewModel.uiState)
        assertTrue(state.ready.hasNotPassedTests)

        // A test that was taken and failed has not been passed, so it can be taken again.
        finishTest(testNumber = 1, passed = false)
        assertTrue(state.ready.hasNotPassedTests)

        finishTest(testNumber = 1, passed = true)
        assertTrue(state.ready.hasNotPassedTests)

        finishTest(testNumber = 2, passed = true)
        assertFalse(state.ready.hasNotPassedTests)
    }

    @Test
    fun randomNotPassedRetakesAFailedTest() = runTest {
        val state = observe(viewModel.uiState)
        finishTest(testNumber = 1, passed = true)
        finishTest(testNumber = 2, passed = false)

        // Only the failed test is left unpassed, so that's the one it opens.
        viewModel.onEvent(HomeUiEvent.StartRandomTest(notPassed = true))

        assertEquals(2, repositories.attempts.get(assertNotNull(state.ready.openAttemptId))!!.testNumber)
    }

    @Test
    fun randomNotPassedPicksNothingOnceEveryTestIsPassed() = runTest {
        val state = observe(viewModel.uiState)
        finishTest(testNumber = 1, passed = true)
        finishTest(testNumber = 2, passed = true)

        viewModel.onEvent(HomeUiEvent.StartRandomTest(notPassed = true))

        assertNull(state.ready.openAttemptId)
    }

    @Test
    fun randomNotPassedPicksATestThatIsUnderWay() = runTest {
        val state = observe(viewModel.uiState)
        finishTest(testNumber = 2, passed = true)
        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 1))
        val started = state.ready.openAttemptId!!
        viewModel.onEvent(HomeUiEvent.AttemptOpened)

        // Half-done test 1 is the only one left, and picking it asks what to do with it.
        viewModel.onEvent(HomeUiEvent.StartRandomTest(notPassed = true))

        assertEquals(ResumePrompt(started, testNumber = 1, answered = 0, total = 3), state.ready.resumePrompt)
    }

    /** Takes [testNumber] from the beginning and finishes it with every answer right, or with one
     * wrong: three questions worth four points means any mistake is below the 90 % pass mark. */
    private suspend fun finishTest(testNumber: Int, passed: Boolean) {
        val session = repositories.exams.currentSession()
        val attempt = session.startOrResume(session.tests.first { it.number == testNumber })
        session.questionsOf(attempt).forEach { question ->
            val chosen = if (passed) question.correct else question.answers.indices.first { it != question.correct }
            session.answer(attempt.id, question, chosen)
        }
        session.finish(attempt.id)
    }

    @Test
    fun pickingATestAlreadyPassedAsksBeforeRedoingIt() = runTest {
        val state = observe(viewModel.uiState)
        finishTest(testNumber = 1, passed = true)

        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 1))

        assertNull(state.ready.openAttemptId)
        assertEquals(PassedPrompt(testNumber = 1, score = 4, maxScore = 4), state.ready.passedPrompt)
    }

    @Test
    fun redoingAPassedTestStartsItFromTheBeginning() = runTest {
        val state = observe(viewModel.uiState)
        finishTest(testNumber = 1, passed = true)
        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 1))

        viewModel.onEvent(HomeUiEvent.RedoTest)

        assertNull(state.ready.passedPrompt)
        val redone = assertNotNull(repositories.attempts.get(assertNotNull(state.ready.openAttemptId)))
        assertEquals(1, redone.testNumber)
        assertTrue(redone.answers.isEmpty())
        // The score is only given up once the test is taken again: a finished attempt is final, so
        // it stays until the new one replaces it as the test's latest result.
        assertEquals(TestResult(4, 4), state.ready.progress.results[1])
    }

    @Test
    fun dismissingThePassedPromptLeavesTheTestAsItWas() = runTest {
        val state = observe(viewModel.uiState)
        finishTest(testNumber = 1, passed = true)
        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 1))

        viewModel.onEvent(HomeUiEvent.DismissPassedPrompt)

        assertNull(state.ready.passedPrompt)
        assertNull(state.ready.openAttemptId)
        assertEquals(TestResult(4, 4), state.ready.progress.results[1])
    }

    @Test
    fun aFailedTestIsStartedAgainWithoutAsking() = runTest {
        val state = observe(viewModel.uiState)
        finishTest(testNumber = 1, passed = false)

        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 1))

        assertNull(state.ready.passedPrompt)
        assertEquals(1, repositories.attempts.get(assertNotNull(state.ready.openAttemptId))!!.testNumber)
    }

    @Test
    fun redoingAPassedTestRecordsItAsNewlyStarted() = runTest {
        repositories.preferences.setAnalyticsEnabled(true)
        val state = observe(viewModel.uiState)
        finishTest(testNumber = 1, passed = true)
        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 1))
        repositories.analyticsSink.events.clear()
        repositories.analyticsSink.properties.clear()

        viewModel.onEvent(HomeUiEvent.RedoTest)

        assertEquals(listOf(AnalyticsEvent.TestStarted), repositories.analyticsSink.events)
        assertEquals(
            mapOf("kind" to "practice_test", "resumed" to false),
            repositories.analyticsSink.properties.single(),
        )
    }

    @Test
    fun askingWhetherToRedoAPassedTestRecordsNothing() = runTest {
        repositories.preferences.setAnalyticsEnabled(true)
        val state = observe(viewModel.uiState)
        finishTest(testNumber = 1, passed = true)
        repositories.analyticsSink.events.clear()

        // The question is not a start yet, so it must not be recorded as one.
        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 1))

        assertEquals(emptyList(), repositories.analyticsSink.events)
        assertNotNull(state.ready.passedPrompt)
    }

    @Test
    fun startingATestRecordsItAsNewlyStarted() = runTest {
        repositories.preferences.setAnalyticsEnabled(true)
        observe(viewModel.uiState)

        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 2))

        assertEquals(listOf(AnalyticsEvent.TestStarted), repositories.analyticsSink.events)
        assertEquals(
            mapOf("kind" to "practice_test", "resumed" to false),
            repositories.analyticsSink.properties.single(),
        )
    }

    @Test
    fun resumingATestRecordsItAsContinued() = runTest {
        repositories.preferences.setAnalyticsEnabled(true)
        val state = observe(viewModel.uiState)
        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 1))
        viewModel.onEvent(HomeUiEvent.AttemptOpened)
        repositories.analyticsSink.events.clear()
        repositories.analyticsSink.properties.clear()

        viewModel.onEvent(HomeUiEvent.Resume)

        assertEquals(listOf(AnalyticsEvent.TestStarted), repositories.analyticsSink.events)
        assertEquals(
            mapOf("kind" to "practice_test", "resumed" to true),
            repositories.analyticsSink.properties.single(),
        )
    }

    @Test
    fun askingWhatToDoAboutAnAttemptUnderWayRecordsNothing() = runTest {
        repositories.preferences.setAnalyticsEnabled(true)
        val state = observe(viewModel.uiState)
        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 1))
        viewModel.onEvent(HomeUiEvent.AttemptOpened)
        repositories.analyticsSink.events.clear()

        // The prompt is not a start or a continuation yet, so it must not be recorded as one.
        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 1))

        assertEquals(emptyList(), repositories.analyticsSink.events)
        assertNotNull(state.ready.resumePrompt)
    }

    @Test
    fun practisingMistakesRecordsAMistakesRetest() = runTest {
        repositories.preferences.setAnalyticsEnabled(true)
        val state = observe(viewModel.uiState)
        viewModel.onEvent(HomeUiEvent.StartTest(testNumber = 1))
        val attempt = repositories.attempts.get(state.ready.openAttemptId!!)!!
        val session = repositories.exams.currentSession()
        session.questionsOf(attempt).forEach { session.answer(attempt.id, it, chosen = 1) }
        session.finish(attempt.id)
        viewModel.onEvent(HomeUiEvent.AttemptOpened)
        repositories.analyticsSink.events.clear()
        repositories.analyticsSink.properties.clear()

        viewModel.onEvent(HomeUiEvent.PracticeMistakes)

        assertEquals(
            mapOf("kind" to "mistakes", "resumed" to false),
            repositories.analyticsSink.properties.single(),
        )
    }

    @Test
    fun theRateCardComesAfterTenFinishedTests() = runTest {
        val state = observe(viewModel.uiState)

        // Nine tests done, failed ones among them: not yet.
        repeat(9) { finishTest(testNumber = 1, passed = false) }
        assertNull(state.ready.rateCard)

        finishTest(testNumber = 1, passed = false)

        assertEquals(RateCard("Play Store"), state.ready.rateCard)
    }

    @Test
    fun dismissingTheCardPutsItAwayUntilTwentyMoreTestsHaveBeenDone() = runTest {
        val state = observe(viewModel.uiState)
        repeat(10) { finishTest(testNumber = 1, passed = false) }

        viewModel.onEvent(HomeUiEvent.DismissRateCard)
        assertNull(state.ready.rateCard)

        repeat(19) { finishTest(testNumber = 1, passed = false) }
        assertNull(state.ready.rateCard)

        finishTest(testNumber = 1, passed = false)
        assertEquals(RateCard("Play Store"), state.ready.rateCard)
    }

    @Test
    fun theLastCardWaitsUntilEveryTestHasBeenPassed() = runTest {
        val state = observe(viewModel.uiState)
        repeat(10) { finishTest(testNumber = 1, passed = false) }
        viewModel.onEvent(HomeUiEvent.DismissRateCard)
        repeat(20) { finishTest(testNumber = 1, passed = false) }
        assertEquals(RateCard("Play Store"), state.ready.rateCard)

        viewModel.onEvent(HomeUiEvent.DismissRateCard)
        assertNull(state.ready.rateCard)

        // Tests done after the last card was closed are not what it waits for: the exam has to be
        // passed in full, both of its tests.
        repeat(5) { finishTest(testNumber = 1, passed = false) }
        finishTest(testNumber = 2, passed = false)
        assertNull(state.ready.rateCard)

        finishTest(testNumber = 1, passed = true)
        assertNull(state.ready.rateCard)

        finishTest(testNumber = 2, passed = true)
        assertEquals(RateCard("Play Store"), state.ready.rateCard)
    }

    @Test
    fun goingToTheStoreStopsTheAsking() = runTest {
        val state = observe(viewModel.uiState)
        repeat(10) { finishTest(testNumber = 1, passed = false) }

        viewModel.onEvent(HomeUiEvent.RateApp)

        assertNull(state.ready.rateCard)
        repeat(30) { finishTest(testNumber = 1, passed = false) }
        finishTest(testNumber = 1, passed = true)
        finishTest(testNumber = 2, passed = true)
        assertNull(state.ready.rateCard)
    }

    @Test
    fun theButtonAsksTheStoreForItsRatingDialog() = runTest {
        observe(viewModel.uiState)
        repeat(10) { finishTest(testNumber = 1, passed = false) }

        viewModel.onEvent(HomeUiEvent.RateApp)

        assertEquals(listOf(store), repositories.storeReview.requested)
    }

    @Test
    fun theCrossDoesNotAskTheStore() = runTest {
        observe(viewModel.uiState)
        repeat(10) { finishTest(testNumber = 1, passed = false) }

        viewModel.onEvent(HomeUiEvent.DismissRateCard)

        assertEquals(emptyList(), repositories.storeReview.requested)
    }

    @Test
    fun theBannerIsDrawnUntilPremiumIsBought() = runTest {
        val state = observe(viewModel.uiState)

        // Ads are shown to everyone who has not paid for them to stop, whatever they answered about
        // personalizing them.
        assertTrue(state.ready.showAds)

        repositories.premium.update(PremiumStatus.Premium(PurchaseSource.Play))

        assertFalse(state.ready.showAds)
    }

    @Test
    fun nothingIsAskedWhereThereIsNoStoreToRateTheAppIn() = runTest {
        // The web build has no store, and neither has an iOS build whose page cannot be addressed
        // yet: the card is left off rather than sending the driver nowhere.
        val withoutStore = HomeViewModel(
            repositories.exams,
            repositories.analytics,
            repositories.review,
            repositories.ads,
            store = null,
            random = Random(42),
        )
        val state = observe(withoutStore.uiState)

        repeat(10) { finishTest(testNumber = 1, passed = false) }
        finishTest(testNumber = 1, passed = true)
        finishTest(testNumber = 2, passed = true)

        assertNull(state.ready.rateCard)
    }
}

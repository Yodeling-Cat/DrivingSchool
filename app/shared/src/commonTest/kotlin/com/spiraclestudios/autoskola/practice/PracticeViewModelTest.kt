package com.spiraclestudios.autoskola.practice

import androidx.lifecycle.viewModelScope
import com.spiraclestudios.autoskola.TestResult
import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.data.banks.testSituationsBank
import com.spiraclestudios.autoskola.data.preferences.TestSelection
import com.spiraclestudios.autoskola.stats.TestAttempt
import com.spiraclestudios.autoskola.testing.TestRepositories
import com.spiraclestudios.autoskola.testing.ViewModelTest
import com.spiraclestudios.autoskola.testing.currentSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeViewModelTest : ViewModelTest() {

    private val repositories = TestRepositories().apply {
        preferences.select(TestSelection("sk-sk", LicenseCategory.B))
    }

    private suspend fun startTest(checkImmediately: Boolean = false): TestAttempt {
        repositories.preferences.setCheckAnswersImmediately(checkImmediately)
        val session = repositories.exams.currentSession()
        return session.startOrResume(session.tests.first())
    }

    private fun TestScope.open(attempt: TestAttempt): Pair<PracticeViewModel, StateFlow<PracticeUiState>> {
        val viewModel = PracticeViewModel(
            attempt.id,
            repositories.exams,
            repositories.banks,
            repositories.preferences,
            repositories.answers,
        )
        return viewModel to observe(viewModel.uiState)
    }

    /** The practice screen over an attempt started by [repositories], which is how a drill is run. */
    private fun TestScope.open(
        attempt: TestAttempt,
        repositories: TestRepositories,
    ): Pair<PracticeViewModel, StateFlow<PracticeUiState>> {
        val viewModel = PracticeViewModel(
            attempt.id,
            repositories.exams,
            repositories.banks,
            repositories.preferences,
            repositories.answers,
        )
        return viewModel to observe(viewModel.uiState)
    }

    /** Turns auto-advance on, which is only possible while answers aren't checked as they're picked. */
    private fun autoAdvance() {
        repositories.preferences.setCheckAnswersImmediately(false)
        repositories.preferences.setAdvanceAfterAnswer(true)
    }

    private val StateFlow<PracticeUiState>.ready get() = assertIs<PracticeUiState.Ready>(value)

    /**
     * Runs [body] with the screen in the foreground, and clears the ViewModel when it ends - also
     * when [body] throws.
     *
     * The practice screen ticks once a second while it is in the foreground. A ticker that is still
     * running when a test ends leaves the test's scheduler with work forever, and `runTest` waits
     * for that scheduler to go idle before it returns: the test hangs, and an assertion that failed
     * is never even reported. Clearing the ViewModel stops the ticker, which is exactly what the
     * screen going away does on a device.
     */
    private inline fun PracticeViewModel.withScreen(body: () -> Unit) {
        onEvent(PracticeUiEvent.Foreground(true))
        try {
            body()
        } finally {
            viewModelScope.cancel()
        }
    }

    /**
     * Lets [seconds] of the test's virtual time pass, the tick that falls on the second included.
     *
     * `advanceTimeBy` stops just short of the moment it advances to, and the practice timer ticks
     * on the second, so the tick due at the end of the wait needs a `runCurrent` to run.
     */
    private fun TestScope.secondsPass(seconds: Int) {
        testScheduler.advanceTimeBy(seconds * 1_000L)
        testScheduler.runCurrent()
    }

    /** Lets [duration] of the test's virtual time pass, work due at the end of the wait included. */
    private fun TestScope.millisPass(duration: Duration) {
        testScheduler.advanceTimeBy(duration.inWholeMilliseconds)
        testScheduler.runCurrent()
    }

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
    fun answeringMovesOnToTheNextQuestionShortlyAfterTheAnswer() = runTest {
        val attempt = startTest()
        autoAdvance()
        val (viewModel, state) = open(attempt)

        viewModel.onEvent(PracticeUiEvent.Answer(questionIndex = 0, answerIndex = 1))

        // The pick shows on the question it was given on first, so it is seen before the test moves on.
        assertEquals(mapOf(0 to 1), state.ready.answers)
        millisPass(AUTO_ADVANCE_DELAY_MILLIS - 1.milliseconds)
        assertNull(state.ready.advanceToPage)

        millisPass(1.milliseconds)
        assertEquals(1, state.ready.advanceToPage)

        // The screen reports the move, so it isn't asked for a second time.
        viewModel.onEvent(PracticeUiEvent.PageAdvanced)
        assertNull(state.ready.advanceToPage)
    }

    @Test
    fun aSavedTestGoesByTheSettingsTheUserHasNowNotTheOnesItStartedWith() = runTest {
        // Started with answers checked as they are picked, the user then turns that off in the
        // settings and comes back to the test: it must not go on behaving as it did when it started.
        val attempt = startTest(checkImmediately = true)
        val (viewModel, state) = open(attempt)
        assertTrue(state.ready.checkImmediately)

        repositories.preferences.setCheckAnswersImmediately(false)
        repositories.preferences.setAdvanceAfterAnswer(true)

        assertFalse(state.ready.checkImmediately)

        // The pick is neither revealed nor locked, so it can be taken back...
        viewModel.onEvent(PracticeUiEvent.Answer(questionIndex = 0, answerIndex = 1))
        assertEquals(mapOf(0 to 1), state.ready.answers)
        viewModel.onEvent(PracticeUiEvent.Answer(questionIndex = 0, answerIndex = 2))
        assertEquals(mapOf(0 to 2), state.ready.answers)

        // ...and the test moves on by itself, as the settings now say.
        millisPass(AUTO_ADVANCE_DELAY_MILLIS)
        assertEquals(1, state.ready.advanceToPage)
    }

    @Test
    fun theLastQuestionStaysOpenWhenAutoAdvanceIsOn() = runTest {
        val attempt = startTest()
        autoAdvance()
        val (viewModel, state) = open(attempt)
        val last = state.ready.questions.lastIndex

        viewModel.onEvent(PracticeUiEvent.Answer(questionIndex = last, answerIndex = 0))

        assertEquals(mapOf(last to 0), state.ready.answers)
        millisPass(AUTO_ADVANCE_DELAY_MILLIS)
        assertNull(state.ready.advanceToPage)
    }

    @Test
    fun aFinishedTestDoesNotMoveOn() = runTest {
        val attempt = startTest()
        autoAdvance()
        repositories.attempts.finish(attempt.id, TestResult(0, 4))
        val (viewModel, state) = open(attempt)

        viewModel.onEvent(PracticeUiEvent.Answer(questionIndex = 0, answerIndex = 1))

        millisPass(AUTO_ADVANCE_DELAY_MILLIS)
        assertNull(state.ready.advanceToPage)
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

    @Test
    fun elapsedTimeStartsAtZeroForNewAttempt() = runTest {
        val attempt = startTest()
        val (_, state) = open(attempt)

        assertEquals(0L, state.ready.elapsedSeconds)
        assertFalse(state.ready.showTimeLimitToast)
    }

    @Test
    fun elapsedTimeRestoresFromSavedAttempt() = runTest {
        val attempt = startTest()
        repositories.attempts.updateElapsed(attempt.id, 120L)

        val (_, state) = open(attempt)

        assertEquals(120L, state.ready.elapsedSeconds)
    }

    @Test
    fun elapsedTimeTicksOnlyWhileInForeground() = runTest {
        val attempt = startTest()
        val (viewModel, state) = open(attempt)

        // Does not tick in background
        secondsPass(3)
        assertEquals(0L, state.ready.elapsedSeconds)

        viewModel.withScreen {
            // Ticks while the screen is in the foreground
            secondsPass(3)
            assertEquals(3L, state.ready.elapsedSeconds)

            // Leaving the foreground stops the ticking and saves what was reached
            viewModel.onEvent(PracticeUiEvent.Foreground(false))
            secondsPass(5)
            assertEquals(3L, state.ready.elapsedSeconds)
            assertEquals(3L, repositories.attempts.get(attempt.id)!!.elapsedSeconds)

            // Coming back picks up where it left off
            viewModel.onEvent(PracticeUiEvent.Foreground(true))
            secondsPass(2)
            assertEquals(5L, state.ready.elapsedSeconds)
        }
    }

    @Test
    fun triggersToastWhenAllottedTimeReachedWithoutStoppingTest() = runTest {
        val attempt = startTest()
        repositories.attempts.updateElapsed(attempt.id, 1798L)
        val (viewModel, state) = open(attempt)

        viewModel.withScreen {
            secondsPass(1)
            assertEquals(1799L, state.ready.elapsedSeconds)
            assertFalse(state.ready.showTimeLimitToast)
            assertFalse(state.ready.finished)

            // Reaches allotted time (1800s)
            secondsPass(1)
            assertEquals(1800L, state.ready.elapsedSeconds)
            assertTrue(state.ready.showTimeLimitToast)
            assertFalse(state.ready.finished) // Test does not stop!

            // Acknowledge toast
            viewModel.onEvent(PracticeUiEvent.TimeLimitToastShown)
            assertFalse(state.ready.showTimeLimitToast)

            // Further ticks do not re-trigger toast
            secondsPass(2)
            assertEquals(1802L, state.ready.elapsedSeconds)
            assertFalse(state.ready.showTimeLimitToast)
            assertFalse(state.ready.finished)
        }
    }

    @Test
    fun theStateCarriesTheTimeATestIsGiven() = runTest {
        val attempt = startTest()
        val (viewModel, state) = open(attempt)

        viewModel.withScreen {
            assertEquals(1800L, state.ready.allottedSeconds)
        }
    }

    @Test
    fun doesNotTriggerToastOnResumeIfAlreadyPastAllottedTime() = runTest {
        val attempt = startTest()
        repositories.attempts.updateElapsed(attempt.id, 1850L)
        val (viewModel, state) = open(attempt)

        viewModel.withScreen {
            secondsPass(1)
            assertEquals(1851L, state.ready.elapsedSeconds)
            assertFalse(state.ready.showTimeLimitToast)
        }
    }

    @Test
    fun theClockStopsOnTheAnswerThatChecksTheLastQuestion() = runTest {
        val attempt = startTest(checkImmediately = true)
        val (viewModel, state) = open(attempt)

        viewModel.withScreen {
            secondsPass(4)
            assertEquals(4L, state.ready.elapsedSeconds)

            // Every answer is checked as it is picked, so the last one ends the test's work.
            state.ready.questions.indices.forEach { viewModel.onEvent(PracticeUiEvent.Answer(it, answerIndex = 0)) }
            assertTrue(state.ready.allAnswered)

            secondsPass(10)
            assertEquals(4L, state.ready.elapsedSeconds)
        }
    }

    @Test
    fun theClockKeepsRunningWhileAnswersAreOnlyScoredAtTheEnd() = runTest {
        val attempt = startTest(checkImmediately = false)
        val (viewModel, state) = open(attempt)

        viewModel.withScreen {
            secondsPass(4)
            state.ready.questions.indices.forEach { viewModel.onEvent(PracticeUiEvent.Answer(it, answerIndex = 0)) }
            assertTrue(state.ready.allAnswered)

            // Without check-as-you-go the test is still the user's to run: it is timed until they
            // finish it themselves.
            secondsPass(3)
            assertEquals(7L, state.ready.elapsedSeconds)
        }
    }

    @Test
    fun elapsedTimeStopsTickingWhenFinished() = runTest {
        val attempt = startTest()
        val (viewModel, state) = open(attempt)

        viewModel.withScreen {
            secondsPass(5)
            assertEquals(5L, state.ready.elapsedSeconds)

            viewModel.onEvent(PracticeUiEvent.Finish)
            assertTrue(state.ready.finished)

            // Finishing stops the ticking, and keeps the time that was reached
            secondsPass(5)
            assertEquals(5L, state.ready.elapsedSeconds)
            assertEquals(5L, repositories.attempts.get(attempt.id)!!.elapsedSeconds)
        }
    }

    @Test
    fun finishedAttemptOpenedForReviewDoesNotTick() = runTest {
        val attempt = startTest()
        repositories.attempts.finish(attempt.id, TestResult(100, 100), elapsedSeconds = 450L)

        val (viewModel, state) = open(attempt)
        assertEquals(450L, state.ready.elapsedSeconds)

        viewModel.withScreen {
            // A finished test is being reviewed, not taken: nothing ticks.
            secondsPass(5)
            assertEquals(450L, state.ready.elapsedSeconds)
        }
    }

    @Test
    fun theDebugShortcutAnswersEveryQuestionCorrectly() = runTest {
        val attempt = startTest()
        val (viewModel, state) = open(attempt)

        viewModel.withScreen { viewModel.onEvent(PracticeUiEvent.AnswerAllCorrectly) }

        val questions = state.ready.questions
        assertEquals(questions.indices.toList(), state.ready.answers.keys.sorted())
        questions.forEachIndexed { index, question ->
            assertEquals(question.question.correct, state.ready.answers[index], "question $index")
        }
    }

    @Test
    fun theDebugShortcutAnswersEveryQuestionWithOneOfThatQuestionsOptions() = runTest {
        val attempt = startTest()
        val (viewModel, state) = open(attempt)

        viewModel.withScreen { viewModel.onEvent(PracticeUiEvent.AnswerAllRandomly) }

        val questions = state.ready.questions
        assertEquals(questions.indices.toList(), state.ready.answers.keys.sorted())
        questions.forEachIndexed { index, question ->
            val chosen = assertNotNull(state.ready.answers[index], "question $index")
            assertTrue(chosen in question.question.answers.indices, "answer $chosen for question $index")
        }
    }

    @Test
    fun theDebugShortcutLeavesTheAnswersTheUserAlreadyGave() = runTest {
        val attempt = startTest()
        val (viewModel, state) = open(attempt)

        viewModel.withScreen {
            // A deliberately wrong answer to the first question, as a user might give.
            viewModel.onEvent(PracticeUiEvent.Answer(0, 2))
            viewModel.onEvent(PracticeUiEvent.AnswerAllCorrectly)
        }

        assertEquals(2, state.ready.answers[0])
        assertEquals(0, state.ready.answers[1])
    }

    @Test
    fun anAnswerInASituationsDrillIsTheAnswerToThatSituation() = runTest {
        val drills = TestRepositories(testSituationsBank()).apply {
            preferences.select(TestSelection("sk-sk", LicenseCategory.B))
        }
        val session = drills.exams.currentSession()
        val drill = assertNotNull(session.startSituations(session.bank.questions))
        val (viewModel, state) = open(drill, drills)

        viewModel.withScreen {
            val question = state.ready.questions[0].question
            viewModel.onEvent(PracticeUiEvent.Answer(questionIndex = 0, answerIndex = 1))

            // The situation the list draws shows the same answer: it was answered here.
            assertEquals(1, drills.answers.answers("sk-sk", 1).first().chosenFor(question.id))
        }
    }

    @Test
    fun anAnswerToATestIsNotAnAnswerToASituation() = runTest {
        // Tests and situations are answered in the same screen, but a test holds questions about
        // everything: only a drill is a run through the situations.
        val attempt = startTest()
        val (viewModel, _) = open(attempt)

        viewModel.withScreen { viewModel.onEvent(PracticeUiEvent.Answer(0, 1)) }

        assertTrue(repositories.answers.answers("sk-sk", 1).first().byQuestion.isEmpty())
    }

    @Test
    fun aSituationsDrillIsGivenNoTimeAtAllAndNeverRunsOut() = runTest {
        val drills = TestRepositories(testSituationsBank()).apply {
            preferences.select(TestSelection("sk-sk", LicenseCategory.B))
        }
        val session = drills.exams.currentSession()
        val drill = assertNotNull(session.startSituations(session.bank.questions))
        drills.attempts.updateElapsed(drill.id, 1_798L)
        val (viewModel, state) = open(drill, drills)

        viewModel.withScreen {
            secondsPass(5)

            // Past where a test's thirty minutes would be: the clock keeps counting, quietly.
            assertNull(state.ready.allottedSeconds)
            assertEquals(1_803L, state.ready.elapsedSeconds)
            assertFalse(state.ready.showTimeLimitToast)
        }
    }

    @Test
    fun leavingATestNothingWasAnsweredInDropsIt() = runTest {
        // Nothing was answered, so there is no work in it to come back to.
        val attempt = startTest()
        val (viewModel, state) = open(attempt)

        viewModel.withScreen { viewModel.onEvent(PracticeUiEvent.Leave) }

        assertTrue(assertNotNull(repositories.attempts.get(attempt.id)).discardedAt != null)
        assertEquals(PracticeUiState.Closed, state.value)
    }

    @Test
    fun leavingADrillNothingWasAnsweredInDropsThatToo() = runTest {
        // The rule is about leaving, not about tests: a drill nobody answered in goes the same way.
        val drills = TestRepositories(testSituationsBank()).apply {
            preferences.select(TestSelection("sk-sk", LicenseCategory.B))
        }
        val session = drills.exams.currentSession()
        val drill = assertNotNull(session.startSituations(session.bank.questions))
        val (viewModel, state) = open(drill, drills)

        viewModel.withScreen { viewModel.onEvent(PracticeUiEvent.Leave) }

        assertTrue(assertNotNull(drills.attempts.get(drill.id)).discardedAt != null)
        assertEquals(PracticeUiState.Closed, state.value)
    }

    @Test
    fun leavingATestWithAnswersInItKeepsItWhereItWas() = runTest {
        val attempt = startTest()
        val (viewModel, state) = open(attempt)

        viewModel.withScreen {
            viewModel.onEvent(PracticeUiEvent.Answer(questionIndex = 0, answerIndex = 1))
            viewModel.onEvent(PracticeUiEvent.Leave)

            assertNull(assertNotNull(repositories.attempts.get(attempt.id)).discardedAt)
            assertEquals(1, state.ready.answers[0])
            // The screen closes once the leaving has been dealt with; the test waits to be continued.
            assertTrue(state.ready.close)
        }
    }
}

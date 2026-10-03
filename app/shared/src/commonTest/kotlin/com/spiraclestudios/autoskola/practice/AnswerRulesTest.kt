package com.spiraclestudios.autoskola.practice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnswerRulesTest {

    @Test
    fun answersLockOnlyOnceCheckedImmediately() {
        assertFalse(isAnswerLocked(checkImmediately = false, finished = false, selected = 1))
        assertFalse(isAnswerLocked(checkImmediately = true, finished = false, selected = null))
        assertTrue(isAnswerLocked(checkImmediately = true, finished = false, selected = 1))
    }

    @Test
    fun everyAnswerIsLockedOnceTheTestIsFinished() {
        assertTrue(isAnswerLocked(checkImmediately = false, finished = true, selected = 1))
        assertTrue(isAnswerLocked(checkImmediately = false, finished = true, selected = null))
    }

    @Test
    fun correctnessIsRevealedWhenCheckingImmediatelyOrOnceFinished() {
        assertFalse(revealsCorrectness(checkImmediately = false, finished = false))
        assertTrue(revealsCorrectness(checkImmediately = true, finished = false))
        assertTrue(revealsCorrectness(checkImmediately = false, finished = true))
    }

    @Test
    fun finishPromptOffersToFinishThenToShowResults() {
        assertEquals(null, finishPrompt(allAnswered = false, finished = false))
        assertEquals(FinishPrompt.Finish, finishPrompt(allAnswered = true, finished = false))
        assertEquals(FinishPrompt.ShowResults, finishPrompt(allAnswered = true, finished = true))
        assertEquals(FinishPrompt.ShowResults, finishPrompt(allAnswered = false, finished = true))
    }

    @Test
    fun aTestCountsWhatIsAnsweredAndWhatIsLeftOpen() {
        val answers = mapOf(0 to 1, 1 to 0, 2 to 3)

        assertEquals(3 to 2, answeredAndSkipped(answers, questionCount = 5))
        assertEquals(0 to 5, answeredAndSkipped(emptyMap(), questionCount = 5))
        assertEquals(5 to 0, answeredAndSkipped((0 until 5).associateWith { 0 }, questionCount = 5))
    }

    @Test
    fun aSectionCountsOnlyItsOwnQuestionsAsAnswered() {
        val answers = mapOf(2 to 1, 3 to 0, 9 to 2)

        assertEquals(2, answeredAmong(answers, 0..5))
        assertEquals(1, answeredAmong(answers, 8..9))
        assertEquals(0, answeredAmong(answers, 5..7))
        assertEquals(3, answeredAmong(answers, 0 until 10))
    }

    @Test
    fun aTestWithNoQuestionsHasNothingAnsweredOrLeftOpen() {
        assertEquals(0 to 0, answeredAndSkipped(emptyMap(), questionCount = 0))
    }

    @Test
    fun theProgressLineCountsWhatIsAnsweredOutOfTheWholeTest() {
        assertEquals(0f, answeredFraction(answered = 0, questionCount = 4))
        assertEquals(0.25f, answeredFraction(answered = 1, questionCount = 4))
        assertEquals(0.5f, answeredFraction(answered = 2, questionCount = 4))
        assertEquals(1f, answeredFraction(answered = 4, questionCount = 4))
    }

    @Test
    fun aTestWithNoQuestionsHasNoProgressToShow() {
        assertEquals(0f, answeredFraction(answered = 0, questionCount = 0))
    }

    @Test
    fun theProgressLineNeverRunsPastTheWholeTest() {
        assertEquals(1f, answeredFraction(answered = 6, questionCount = 4))
    }

    @Test
    fun answersMoveTheTestOnOnlyWhenThatIsAskedForAndNothingIsRevealed() {
        assertTrue(advances(advanceAfterAnswer = true, checkImmediately = false))
        assertFalse(advances(advanceAfterAnswer = false, checkImmediately = false))
        // A checked answer is shown on its own question, so there is nothing to move on from.
        assertFalse(advances(advanceAfterAnswer = true, checkImmediately = true))
    }

    @Test
    fun theLastQuestionOfATestHasNowhereToMoveOnTo() {
        assertTrue(advances(advanceAfterAnswer = true, checkImmediately = false, questionIndex = 2, count = 4))
        assertFalse(advances(advanceAfterAnswer = true, checkImmediately = false, questionIndex = 3, count = 4))
        assertFalse(advances(advanceAfterAnswer = true, checkImmediately = false, questionIndex = 0, count = 1))
    }

    @Test
    fun aFinishedTestDoesNotMoveOn() {
        assertFalse(advances(advanceAfterAnswer = true, checkImmediately = false, finished = true))
    }

    @Test
    fun withoutCheckingOnlyTheSelectionIsShown() {
        val states = (0..2).map { answerState(index = it, selected = 1, correct = 0, revealed = false) }
        assertEquals(listOf(AnswerState.Neutral, AnswerState.Selected, AnswerState.Neutral), states)
    }

    @Test
    fun checkingShowsTheCorrectAnswerAndTheWrongPick() {
        val states = (0..2).map { answerState(index = it, selected = 1, correct = 0, revealed = true) }
        assertEquals(listOf(AnswerState.Correct, AnswerState.Wrong, AnswerState.Neutral), states)
    }

    @Test
    fun aCorrectPickIsJustCorrect() {
        val states = (0..2).map { answerState(index = it, selected = 2, correct = 2, revealed = true) }
        assertEquals(listOf(AnswerState.Neutral, AnswerState.Neutral, AnswerState.Correct), states)
    }

    @Test
    fun questionListOnlyRevealsCorrectnessWhenCheckingImmediately() {
        assertEquals(
            ChipState.Unanswered,
            chipState(selected = null, correct = 0, revealCorrectness = true, finished = false),
        )
        assertEquals(
            ChipState.Answered,
            chipState(selected = 1, correct = 0, revealCorrectness = false, finished = false),
        )
        assertEquals(
            ChipState.Incorrect,
            chipState(selected = 1, correct = 0, revealCorrectness = true, finished = false),
        )
        assertEquals(
            ChipState.Correct,
            chipState(selected = 0, correct = 0, revealCorrectness = true, finished = false),
        )
    }

    @Test
    fun aQuestionLeftOpenInAFinishedTestIsSkipped() {
        assertTrue(questionSkipped(selected = null, finished = true))
        assertFalse(questionSkipped(selected = 1, finished = true))
    }

    @Test
    fun aQuestionTheUserHasNotReachedYetIsNotSkipped() {
        assertFalse(questionSkipped(selected = null, finished = false))
        assertFalse(questionSkipped(selected = 1, finished = false))
    }

    @Test
    fun questionListMarksSkippedQuestionsOnlyOnceTheTestIsFinished() {
        assertEquals(
            ChipState.Skipped,
            chipState(selected = null, correct = 0, revealCorrectness = true, finished = true),
        )
        assertEquals(
            ChipState.Unanswered,
            chipState(selected = null, correct = 0, revealCorrectness = true, finished = false),
        )
    }

    /** [advancesAfterAnswer] with the parts a test doesn't care about filled in. */
    private fun advances(
        advanceAfterAnswer: Boolean,
        checkImmediately: Boolean,
        finished: Boolean = false,
        questionIndex: Int = 0,
        count: Int = 4,
    ) = advancesAfterAnswer(
        advanceAfterAnswer = advanceAfterAnswer,
        checkImmediately = checkImmediately,
        finished = finished,
        questionIndex = questionIndex,
        count = count,
    )
}

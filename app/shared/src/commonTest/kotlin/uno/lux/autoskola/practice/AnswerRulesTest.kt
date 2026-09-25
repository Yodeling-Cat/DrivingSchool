package uno.lux.autoskola.practice

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
        assertEquals(ChipState.Unanswered, chipState(selected = null, correct = 0, revealCorrectness = true))
        assertEquals(ChipState.Answered, chipState(selected = 1, correct = 0, revealCorrectness = false))
        assertEquals(ChipState.Incorrect, chipState(selected = 1, correct = 0, revealCorrectness = true))
        assertEquals(ChipState.Correct, chipState(selected = 0, correct = 0, revealCorrectness = true))
    }
}

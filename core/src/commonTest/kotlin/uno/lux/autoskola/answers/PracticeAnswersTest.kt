package uno.lux.autoskola.answers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PracticeAnswersTest {

    @Test
    fun aQuestionNobodyHasAnsweredHasNoAnswer() {
        assertNull(PracticeAnswers().chosenFor(140))
    }

    @Test
    fun anAnswerIsReadBackForTheQuestionItWasGiven() {
        val answers = PracticeAnswers().withAnswer(140, 2)

        assertEquals(2, answers.chosenFor(140))
        assertNull(answers.chosenFor(141))
    }

    @Test
    fun answeringAgainReplacesTheAnswer() {
        // Practice, not a test: there is nothing to lock, so a driver may change their mind.
        val answers = PracticeAnswers().withAnswer(140, 2).withAnswer(140, 0)

        assertEquals(0, answers.chosenFor(140))
        assertEquals(1, answers.byQuestion.size)
    }

    @Test
    fun answersToSeveralQuestionsAreKeptTogether() {
        val answers = PracticeAnswers().withAnswer(140, 1).withAnswer(141, 2)

        assertEquals(mapOf(140 to 1, 141 to 2), answers.byQuestion)
    }

    @Test
    fun theSameAnswersAreTheSameAnswers() {
        // The store keeps a flow that only emits when the answers changed, which is what data-class
        // equality gives it.
        assertEquals(PracticeAnswers().withAnswer(140, 1), PracticeAnswers().withAnswer(140, 1))
        assertNotEquals(PracticeAnswers().withAnswer(140, 1), PracticeAnswers().withAnswer(140, 2))
    }

    @Test
    fun anAnswerIsNotCheckedUntilTheDriverChecksIt() {
        val answers = PracticeAnswers().withAnswer(140, 1)

        assertFalse(answers.isRevealed(140))
        assertTrue(answers.withReveal(140).isRevealed(140))
    }

    @Test
    fun checkingAnAnswerLeavesTheAnswersAlone() {
        val answers = PracticeAnswers().withAnswer(140, 1).withReveal(140)

        assertEquals(1, answers.chosenFor(140))
        assertFalse(answers.isRevealed(141))
    }

    @Test
    fun aQuestionWithNoAnswerHasNothingToCheck() {
        // The screen offers checking only once an answer is picked, and the answers say the same.
        assertFalse(PracticeAnswers().withReveal(140).isRevealed(140))
    }

    @Test
    fun changingACheckedAnswerKeepsItChecked() {
        // The marks are wanted for the answer the driver has now, not the one they had.
        val answers = PracticeAnswers().withAnswer(140, 1).withReveal(140).withAnswer(140, 0)

        assertEquals(0, answers.chosenFor(140))
        assertTrue(answers.isRevealed(140))
    }
}

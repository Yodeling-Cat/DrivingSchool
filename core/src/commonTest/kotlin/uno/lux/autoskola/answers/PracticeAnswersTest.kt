package uno.lux.autoskola.answers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

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
}

package uno.lux.autoskola.practice

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QuestionArrowKeysTest {
    @Test
    fun leftAndUpGoToTheQuestionBefore() {
        assertEquals(2, questionArrowTarget(Key.DirectionLeft, currentPage = 3, pageCount = 10))
        assertEquals(2, questionArrowTarget(Key.DirectionUp, currentPage = 3, pageCount = 10))
    }

    @Test
    fun rightAndDownGoToTheQuestionAfter() {
        assertEquals(4, questionArrowTarget(Key.DirectionRight, currentPage = 3, pageCount = 10))
        assertEquals(4, questionArrowTarget(Key.DirectionDown, currentPage = 3, pageCount = 10))
    }

    @Test
    fun otherKeysStayOnTheQuestion() {
        assertNull(questionArrowTarget(Key.Spacebar, currentPage = 3, pageCount = 10))
        assertNull(questionArrowTarget(Key.Enter, currentPage = 3, pageCount = 10))
        assertNull(questionArrowTarget(Key.A, currentPage = 3, pageCount = 10))
    }

    @Test
    fun thereIsNothingBeforeTheFirstQuestion() {
        assertNull(questionArrowTarget(Key.DirectionLeft, currentPage = 0, pageCount = 10))
        assertNull(questionArrowTarget(Key.DirectionUp, currentPage = 0, pageCount = 10))
    }

    @Test
    fun thereIsNothingAfterTheLastQuestion() {
        assertNull(questionArrowTarget(Key.DirectionRight, currentPage = 9, pageCount = 10))
        assertNull(questionArrowTarget(Key.DirectionDown, currentPage = 9, pageCount = 10))
    }
}

package uno.lux.autoskola.practice

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArrowKeyTargetTest {
    @Test
    fun leftAndUpGoToThePageBefore() {
        assertEquals(2, arrowKeyTarget(Key.DirectionLeft, currentPage = 3, pageCount = 10))
        assertEquals(2, arrowKeyTarget(Key.DirectionUp, currentPage = 3, pageCount = 10))
    }

    @Test
    fun rightAndDownGoToThePageAfter() {
        assertEquals(4, arrowKeyTarget(Key.DirectionRight, currentPage = 3, pageCount = 10))
        assertEquals(4, arrowKeyTarget(Key.DirectionDown, currentPage = 3, pageCount = 10))
    }

    @Test
    fun otherKeysStayOnThePage() {
        assertNull(arrowKeyTarget(Key.Spacebar, currentPage = 3, pageCount = 10))
        assertNull(arrowKeyTarget(Key.Enter, currentPage = 3, pageCount = 10))
        assertNull(arrowKeyTarget(Key.A, currentPage = 3, pageCount = 10))
    }

    @Test
    fun thereIsNothingBeforeTheFirstPage() {
        assertNull(arrowKeyTarget(Key.DirectionLeft, currentPage = 0, pageCount = 10))
        assertNull(arrowKeyTarget(Key.DirectionUp, currentPage = 0, pageCount = 10))
    }

    @Test
    fun thereIsNothingAfterTheLastPage() {
        assertNull(arrowKeyTarget(Key.DirectionRight, currentPage = 9, pageCount = 10))
        assertNull(arrowKeyTarget(Key.DirectionDown, currentPage = 9, pageCount = 10))
    }
}

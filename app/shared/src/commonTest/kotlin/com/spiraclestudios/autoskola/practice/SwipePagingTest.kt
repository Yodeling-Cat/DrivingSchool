package com.spiraclestudios.autoskola.practice

import kotlin.test.Test
import kotlin.test.assertEquals

class SwipePagingTest {

    @Test
    fun aShortSlowDragSpringsBackWhereItWas() {
        assertEquals(2, swipe(from = 2, dragged = -100f))
        assertEquals(2, swipe(from = 2, dragged = 200f))
    }

    @Test
    fun aDragPastAQuarterOfThePageMovesOn() {
        assertEquals(3, swipe(from = 2, dragged = -300f))
        assertEquals(1, swipe(from = 2, dragged = 300f))
    }

    @Test
    fun aFlickMovesOnHoweverShortItWas() {
        assertEquals(3, swipe(from = 2, dragged = -20f, velocity = -1500f))
        assertEquals(1, swipe(from = 2, dragged = 20f, velocity = 1500f))
        // A flick from a standstill carries the page on by itself.
        assertEquals(3, swipe(from = 2, velocity = -1500f))
    }

    @Test
    fun aFlickThatBarelyMovesThePointerDoesNotCount() {
        assertEquals(2, swipe(from = 2, dragged = -100f, velocity = -400f))
    }

    @Test
    fun aFlickBackUndoesADragThatHadNotPassedTheQuarter() {
        assertEquals(2, swipe(from = 2, dragged = -400f, velocity = 1500f))
    }

    @Test
    fun theEndsOfTheTestHaveNowhereToGo() {
        assertEquals(0, swipe(from = 0, dragged = 400f))
        assertEquals(0, swipe(from = 0, dragged = 400f, velocity = 2000f))
        assertEquals(39, swipe(from = 39, dragged = -400f))
        assertEquals(39, swipe(from = 39, dragged = -400f, velocity = -2000f))
    }

    @Test
    fun aPageWithNoWidthMovesNowhere() {
        assertEquals(2, swipe(from = 2, dragged = -500f, pageSize = 0f))
    }

    @Test
    fun aTestWithoutQuestionsStaysOnItsOnlyPage() {
        assertEquals(0, swipe(from = 0, dragged = -500f, pageCount = 0))
    }

    /** The page a drag of [dragged] px, let go at [velocity] px/s, lands on. */
    private fun swipe(
        from: Int,
        dragged: Float = 0f,
        velocity: Float = 0f,
        pageSize: Float = 900f,
        pageCount: Int = 40,
    ) = swipedPage(
        currentPage = from,
        dragged = dragged,
        pageSize = pageSize,
        velocity = velocity,
        pageCount = pageCount,
    )
}

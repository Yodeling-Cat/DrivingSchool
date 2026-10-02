package com.spiraclestudios.autoskola.review

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReviewRequestTest {

    @Test
    fun nothingIsAskedUntilTenTestsHaveBeenDone() {
        val request = ReviewRequest()

        assertFalse(request.isDue(finishedTests = 9, everyTestPassed = false))
        assertTrue(request.isDue(finishedTests = 10, everyTestPassed = false))
    }

    @Test
    fun theFirstCardIsDismissedWhereItWasShown() {
        val dismissed = ReviewRequest().dismissed(finishedTests = 12, everyTestPassed = false)

        assertEquals(ReviewAsk.SecondAsk, dismissed.ask)
        assertFalse(dismissed.isDue(finishedTests = 12, everyTestPassed = false))
    }

    @Test
    fun theSecondCardComesTwentyTestsAfterTheFirstWasDismissed() {
        val dismissed = ReviewRequest().dismissed(finishedTests = 12, everyTestPassed = false)

        // Twelve tests had been done when the card was closed, so the next one is due at thirty-two.
        assertFalse(dismissed.isDue(finishedTests = 31, everyTestPassed = false))
        assertTrue(dismissed.isDue(finishedTests = 32, everyTestPassed = false))
    }

    @Test
    fun theLastCardWaitsUntilEveryTestHasBeenPassed() {
        val dismissed = ReviewRequest()
            .dismissed(finishedTests = 10, everyTestPassed = false)
            .dismissed(finishedTests = 30, everyTestPassed = false)

        assertEquals(ReviewAsk.FinalAsk, dismissed.ask)
        assertFalse(dismissed.isDue(finishedTests = 60, everyTestPassed = false))
        assertTrue(dismissed.isDue(finishedTests = 60, everyTestPassed = true))
    }

    @Test
    fun dismissingTheLastCardEndsTheAsking() {
        val done = ReviewRequest()
            .dismissed(finishedTests = 10, everyTestPassed = false)
            .dismissed(finishedTests = 30, everyTestPassed = false)
            .dismissed(finishedTests = 60, everyTestPassed = true)

        assertEquals(ReviewAsk.AskedEnough, done.ask)
        assertFalse(done.isDue(finishedTests = 200, everyTestPassed = true))
    }

    @Test
    fun dismissingTheSecondCardWithEverythingPassedEndsTheAsking() {
        // Passing everything before the second card arrives leaves the last rung nothing to wait
        // for, so closing the second card is the end of the asking rather than a card that comes
        // straight back.
        val done = ReviewRequest()
            .dismissed(finishedTests = 20, everyTestPassed = true)
            .dismissed(finishedTests = 40, everyTestPassed = true)

        assertEquals(ReviewAsk.AskedEnough, done.ask)
        assertFalse(done.isDue(finishedTests = 41, everyTestPassed = true))
    }

    @Test
    fun goingToTheStoreEndsTheAsking() {
        val rated = ReviewRequest()
            .dismissed(finishedTests = 10, everyTestPassed = false)
            .rated()

        assertEquals(ReviewAsk.AskedEnough, rated.ask)
        assertFalse(rated.isDue(finishedTests = 500, everyTestPassed = true))
    }
}

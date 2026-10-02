package com.spiraclestudios.autoskola.practice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PracticeBarTextTest {

    @Test
    fun aPhoneInLandscapeReadsTheTestNameBesideTheQuestionNumber() {
        assertEquals("Test 2 · ", barQuestionPrefix("Test 2"))
    }

    @Test
    fun theCountIsDrawnOneSpaceBehindTheNumberOfTheQuestion() {
        // The bar draws the two runs one after the other, and must read "9 / 40".
        assertEquals("9 / 40", "9" + barQuestionCount("/ 40"))
    }

    @Test
    fun thePointsLineEndsWithTheSeparatorTheClockFollows() {
        assertEquals("za 3 body · ", barPointsLine("za 3 body"))
    }

    @Test
    fun theSeparatorIsDrawnAsADotAndNeverAsTheEscapeThatWritesIt() {
        // Written with a doubled backslash, "\u00b7" is the six characters of the escape and the bar
        // reads them out on a phone in landscape.
        val line = barQuestionPrefix("Test 2")
        assertTrue(line.contains('·'), line)
        assertFalse(line.contains('\\'), line)
    }
}

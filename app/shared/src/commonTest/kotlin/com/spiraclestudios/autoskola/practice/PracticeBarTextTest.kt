package com.spiraclestudios.autoskola.practice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PracticeBarTextTest {

    @Test
    fun aPhoneInLandscapeReadsTheTestNameBesideTheQuestionNumber() {
        assertEquals("Test 2 · Otázka 9 z 40", barQuestionLine("Test 2", "Otázka 9 z 40"))
    }

    @Test
    fun thePointsLineEndsWithTheSeparatorTheClockFollows() {
        assertEquals("za 3 body · ", barPointsLine("za 3 body"))
    }

    @Test
    fun theSeparatorIsDrawnAsADotAndNeverAsTheEscapeThatWritesIt() {
        // Written with a doubled backslash, "\u00b7" is the six characters of the escape and the bar
        // reads them out on a phone in landscape.
        val line = barQuestionLine("Test 2", "Otázka 9 z 40")
        assertTrue(line.contains('·'), line)
        assertFalse(line.contains('\\'), line)
    }
}

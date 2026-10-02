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
        // The counter is the number, then the count one space behind it: "9 / 40".
        assertEquals("9 / 40", questionCounterLabel("9", "/ 40"))
    }

    @Test
    fun aSectionIsCountedAsWhatOfItIsAnswered() {
        assertEquals("6 / 12", questionCounterLabel("6", "/ 12"))
    }

    @Test
    fun theTestLineCountsWhatIsAnsweredAndWhatIsLeftOpen() {
        assertEquals("6 zodpovedaných · 1 nezodpovedaná", barAnsweredLine("6 zodpovedaných", "1 nezodpovedaná"))
    }

    @Test
    fun aPhoneInLandscapeReadsTheClockAfterTheTestLine() {
        assertEquals("za 3 body · ", barBeforeClock("za 3 body"))
        assertEquals("6 zodpovedaných · 1 nezodpovedaná · ", barBeforeClock("6 zodpovedaných · 1 nezodpovedaná"))
    }

    @Test
    fun theSeparatorIsDrawnAsADotAndNeverAsTheEscapeThatWritesIt() {
        // Written with a doubled backslash, "\u00b7" is the six characters of the escape and the bar
        // reads them out on a phone in landscape.
        val lines = listOf(barQuestionPrefix("Test 2"), barAnsweredLine("6 answered", "1 skipped"))

        lines.forEach { line ->
            assertTrue(line.contains('·'), line)
            assertFalse(line.contains('\\'), line)
        }
    }
}

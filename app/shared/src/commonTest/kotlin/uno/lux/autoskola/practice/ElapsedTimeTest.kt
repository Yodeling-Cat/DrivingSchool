package uno.lux.autoskola.practice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ElapsedTimeTest {

    @Test
    fun aFreshTestReadsZero() {
        assertEquals("00:00", elapsedClock(0))
    }

    @Test
    fun minutesAndSecondsArePaddedToTwoDigits() {
        assertEquals("00:07", elapsedClock(7))
        assertEquals("00:59", elapsedClock(59))
        assertEquals("01:00", elapsedClock(60))
        assertEquals("07:42", elapsedClock(462))
    }

    @Test
    fun theLastSecondBeforeAnHourIsStillMinutes() {
        assertEquals("59:59", elapsedClock(3599))
    }

    @Test
    fun anHourIsShownAsHoursAndMinutes() {
        assertEquals("1:00:00", elapsedClock(3600))
        assertEquals("1:15:25", elapsedClock(4525))
        assertEquals("10:00:00", elapsedClock(36000))
    }

    @Test
    fun theExamsOwnTimeLimitReadsAsMinutes() {
        assertEquals("45:00", elapsedClock(45 * 60))
    }

    @Test
    fun aNegativeCountIsReadAsZeroRatherThanMinus() {
        assertEquals("00:00", elapsedClock(-1))
    }

    @Test
    fun aTestIsOverTimeOnceItHasHadTheTimeItIsGiven() {
        assertFalse(isOverTime(elapsedSeconds = 1799, allottedSeconds = 1800))
        assertTrue(isOverTime(elapsedSeconds = 1800, allottedSeconds = 1800))
        assertTrue(isOverTime(elapsedSeconds = 1801, allottedSeconds = 1800))
    }

    @Test
    fun theClockMeasuredForWidthIsAllZerosWithTheSameColons() {
        // Zero is the widest digit in this font, so a clock of them is as wide as any clock gets.
        assertEquals("00:00", elapsedClockPlaceholder("07:42"))
        assertEquals("0:00:00", elapsedClockPlaceholder("1:07:42"))
    }

    @Test
    fun thePlaceholderIsAsLongAsTheClockItStandsFor() {
        listOf(0L, 7L, 59L, 60L, 462L, 3599L, 3600L, 4525L, 36000L).forEach { seconds ->
            val clock = elapsedClock(seconds)
            assertEquals(clock.length, elapsedClockPlaceholder(clock).length, "placeholder for $clock")
        }
    }
}

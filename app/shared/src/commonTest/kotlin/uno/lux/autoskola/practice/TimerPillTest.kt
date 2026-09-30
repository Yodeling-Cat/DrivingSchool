package uno.lux.autoskola.practice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimerPillTest {
    @Test
    fun theClockStopsOnceEveryAnswerIsCheckedOnTheSpot() {
        assertTrue(stopsClockAtEnd(allAnswered = true, checkImmediately = true))
    }

    @Test
    fun theClockKeepsRunningWhileThereIsSomethingLeftToAnswerOrToCheckLater() {
        assertFalse(stopsClockAtEnd(allAnswered = false, checkImmediately = true))
        assertFalse(stopsClockAtEnd(allAnswered = true, checkImmediately = false))
        assertFalse(stopsClockAtEnd(allAnswered = false, checkImmediately = false))
    }

    @Test
    fun aRunningTestHasAPlainPillEvenWhenItIsOverItsTime() {
        assertEquals(
            TimerPill.Running,
            timerPill(allAnswered = false, checkImmediately = true, finished = false, overTime = true),
        )
        assertEquals(
            TimerPill.Running,
            timerPill(allAnswered = true, checkImmediately = false, finished = false, overTime = false),
        )
    }

    @Test
    fun aStoppedClockShowsHowTheTestWent() {
        assertEquals(
            TimerPill.InTime,
            timerPill(allAnswered = true, checkImmediately = true, finished = false, overTime = false),
        )
        assertEquals(
            TimerPill.OverTime,
            timerPill(allAnswered = true, checkImmediately = true, finished = false, overTime = true),
        )
    }

    @Test
    fun aFinishedTestCountsAsStoppedToo() {
        assertEquals(
            TimerPill.InTime,
            timerPill(allAnswered = false, checkImmediately = false, finished = true, overTime = false),
        )
        assertEquals(
            TimerPill.OverTime,
            timerPill(allAnswered = false, checkImmediately = false, finished = true, overTime = true),
        )
    }
}

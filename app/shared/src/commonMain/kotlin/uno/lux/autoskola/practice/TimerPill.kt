package uno.lux.autoskola.practice

/**
 * How the test's clock is shown. While the test is running it's the plain white pill, with the time
 * itself warning when it is over the limit; once there is nothing left to time the pill carries the
 * result of the clock instead — green within the limit, red past it.
 */
internal enum class TimerPill { Running, InTime, OverTime }

/**
 * Which of those three the pill is. The clock counts as stopped when the test is finished, or when
 * every answer has been checked as it was picked (see [stopsClockAtEnd]).
 */
internal fun timerPill(
    allAnswered: Boolean,
    checkImmediately: Boolean,
    finished: Boolean,
    overTime: Boolean,
): TimerPill = when {
    !(finished || stopsClockAtEnd(allAnswered, checkImmediately)) -> TimerPill.Running
    overTime -> TimerPill.OverTime
    else -> TimerPill.InTime
}

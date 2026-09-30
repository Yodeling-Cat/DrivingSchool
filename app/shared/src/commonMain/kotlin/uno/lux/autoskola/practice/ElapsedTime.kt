package uno.lux.autoskola.practice

/**
 * How long the test has been running, as a clock the user can read at a glance: "07:42", and
 * "1:07:42" once it has gone past the hour. A count a test never gets to (a negative one) reads as
 * zero rather than as a minus.
 */
internal fun elapsedClock(seconds: Long): String {
    val count = seconds.coerceAtLeast(0)
    val minutes = (count % 3600) / 60
    val padded = "${minutes.toString().padStart(2, '0')}:${(count % 60).toString().padStart(2, '0')}"
    val hours = count / 3600
    return if (hours > 0) "$hours:$padded" else padded
}

/**
 * Whether a test that has been running [elapsedSeconds] has had the [allottedSeconds] it's given.
 * A test that reached the time exactly is over it: the clock stops being "still fine" the moment
 * the last minute is up, and the notice that tells the user so arrives at the same moment.
 */
internal fun isOverTime(elapsedSeconds: Long, allottedSeconds: Long): Boolean = elapsedSeconds >= allottedSeconds

/**
 * The clock drawn invisibly beside the real one so the pill around it keeps its width as the digits
 * change: every digit becomes the widest one, and the colons stay where they are. The app's font is
 * proportional - "00:11" is 23% narrower than "00:00" in DM Sans - and the widest digit is a zero
 * (0.707 em against the eight's 0.632), so a clock of zeros is as wide as any clock can be.
 */
internal fun elapsedClockPlaceholder(elapsed: String): String =
    elapsed.map { if (it.isDigit()) '0' else it }.joinToString("")

/**
 * Whether there is nothing left to time. With answers checked as they are picked, the last answer is
 * the end of the work: the user can still read the marks and finish the test, but they are done, so
 * the clock stops where it stood. Without that mode the answers are only scored at the end, and the
 * test is not over until the user says so.
 */
internal fun stopsClockAtEnd(allAnswered: Boolean, checkImmediately: Boolean): Boolean = allAnswered && checkImmediately

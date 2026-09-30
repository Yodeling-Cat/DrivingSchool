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

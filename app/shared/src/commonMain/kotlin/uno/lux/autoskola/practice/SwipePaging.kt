package uno.lux.autoskola.practice

/** How far a drag has to travel to count, as a share of the page it moves between. */
private const val SWIPE_PAGE_FRACTION = 0.25f

/**
 * How far a flick is taken to carry the page on: the distance the drag travelled plus what
 * [FLING_PROJECTION_SECONDS] of the flick that ended it would add. Judging the flick by the ground
 * it covers, rather than by the flick alone, keeps a jitter at the end of a slow drag from flipping
 * the page.
 */
private const val FLING_PROJECTION_SECONDS = 0.15f

/**
 * The question a drag lands on: the next one when the pointer moved towards the start of the layout
 * (a negative [dragged], which is how Compose reports a drag to the left) or the one before it the
 * other way, and the question the user is already on when the drag was too short and too slow to
 * count. [velocity] is how fast the pointer was moving when it was let go. A drag can't leave the
 * test: the last question has no next one, and the first has no previous one.
 */
internal fun swipedPage(
    currentPage: Int,
    dragged: Float,
    pageSize: Float,
    velocity: Float,
    pageCount: Int,
): Int {
    val reached = pageSize * SWIPE_PAGE_FRACTION
    val carried = dragged + velocity * FLING_PROJECTION_SECONDS
    val moved = when {
        pageSize > 0f && carried <= -reached -> 1
        pageSize > 0f && carried >= reached -> -1
        else -> 0
    }
    return (currentPage + moved).coerceIn(0, (pageCount - 1).coerceAtLeast(0))
}

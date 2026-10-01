package uno.lux.autoskola.practice

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.launch

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

/**
 * A horizontal pager that answers a mouse as well as a finger: Compose's own scrollable ignores
 * drags made with a mouse (its `canDrag` says so), so the drag is taken here and the page it lands
 * on is the one [swipedPage] picks. A touch drag never reaches it - the pager consumes it first,
 * being the closer of the two.
 *
 * Used by both the practice screen's questions and the situations, which page the same way.
 */
@Composable
internal fun SwipePager(
    state: PagerState,
    modifier: Modifier = Modifier,
    onPageSize: (widthPx: Int) -> Unit = {},
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable (page: Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var dragged by remember { mutableFloatStateOf(0f) }
    var pageSize by remember { mutableFloatStateOf(0f) }
    val dragState = rememberDraggableState { delta ->
        dragged += delta
        scope.launch { state.dispatchRawDelta(-delta) }
    }
    Box(
        modifier = modifier.draggable(
            state = dragState,
            orientation = Orientation.Horizontal,
            onDragStarted = { dragged = 0f },
            onDragStopped = { velocity ->
                state.animateScrollToPage(
                    swipedPage(
                        currentPage = state.currentPage,
                        dragged = dragged,
                        pageSize = pageSize,
                        velocity = velocity,
                        pageCount = state.pageCount,
                    ),
                )
            },
        ),
    ) {
        HorizontalPager(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    pageSize = it.width.toFloat()
                    onPageSize(it.width)
                },
            verticalAlignment = verticalAlignment,
        ) { page ->
            content(page)
        }
    }
}

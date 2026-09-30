package uno.lux.autoskola.practice

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.unit.isSpecified

/**
 * Question pictures are drawn half again as big as they are. The pictures in the bank are small: a
 * road sign has to be readable, and this is as far as we can stretch one before it turns to mush.
 */
internal const val QUESTION_IMAGE_SCALE = 1.5f

/** How tall the picture of a question may get, before the room it's shown in has a say. */
internal val QuestionImageMaxHeight = 260.dp * QUESTION_IMAGE_SCALE

/**
 * The same for a phone in landscape, where the picture sits next to the question instead of under
 * it: it gets less of the little height there is.
 */
internal val QuestionImageSideBySideMaxHeight = 220.dp * QUESTION_IMAGE_SCALE

/**
 * The size the picture of a question is drawn at: [QUESTION_IMAGE_SCALE] times its own size, shrunk
 * to fit in [maxWidth] x [maxHeight] without distorting it. A picture that already fills the room
 * doesn't grow, and a tiny one is never blown up past the scale above.
 *
 * A picture we don't know the size of yet is drawn at [DpSize.Zero], so the caller can hold the
 * space it will take instead of drawing nothing.
 */
internal fun questionImageSize(intrinsic: DpSize, maxWidth: Dp, maxHeight: Dp): DpSize {
    if (!intrinsic.isDrawable) return DpSize.Zero
    return (intrinsic * QUESTION_IMAGE_SCALE).fittedInto(maxWidth, maxHeight)
}

/**
 * The size of the enlarged copy of a picture drawn at [drawn]: up to [maxScale] times as big, but
 * never bigger than [viewport], the room there is for it on screen. A picture whose drawn size isn't
 * known fills the viewport instead.
 */
internal fun enlargedImageSize(drawn: DpSize, viewport: DpSize, maxScale: Float = 2f): DpSize {
    if (!drawn.isDrawable) return viewport
    return (drawn * maxScale).fittedInto(viewport.width, viewport.height)
}

/** [this] shrunk to fit in [maxWidth] x [maxHeight], keeping its aspect ratio. */
private fun DpSize.fittedInto(maxWidth: Dp, maxHeight: Dp): DpSize {
    val scale = minOf(1f, maxWidth / width, maxHeight / height)
    return if (scale >= 1f) this else this * scale
}

/** Whether this is a size something can be drawn at. */
private val DpSize.isDrawable: Boolean
    get() = isSpecified && width.isFinite && height.isFinite && width > 0.dp && height > 0.dp

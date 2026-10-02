package com.spiraclestudios.autoskola.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.practice.IconClose
import com.spiraclestudios.autoskola.ui.theme.Naj

/**
 * Pictures of the bank are drawn half again as big as they are. They are small: a road sign has to
 * be readable, and this is as far as we can stretch one before it turns to mush.
 */
internal const val IMAGE_SCALE = 1.5f

/** How tall a picture may get, before the room it's shown in has a say. */
internal val ImageMaxHeight = 260.dp * IMAGE_SCALE

/**
 * The same for a phone in landscape, where the picture sits next to the question instead of under
 * it: it gets less of the little height there is.
 */
internal val ImageSideBySideMaxHeight = 220.dp * IMAGE_SCALE

/**
 * How round a picture is, where it sits on a plain background rather than running to its edges.
 *
 * A bank picture is a photograph of a sign on white, so its corners are the corners of the white
 * around it, and the room it is drawn in is small: 12 dp reads as a soft card on a 390 dp question
 * and as a blob on a 52 dp sign in a list. Half that is still rounded, without eating the picture.
 */
internal val ImageCornerRadius = 6.dp

/** Over the page behind an enlarged picture, which is drawn on top of it. */
private val EnlargedImageScrim = Color(0xB3000000)

/** How far an enlarged picture stays away from the edges of the screen it's shown on. */
private val EnlargedImageMargin = 24.dp

/**
 * A picture from the bank, drawn [IMAGE_SCALE] times its own size so it's readable, and tapping it
 * opens a copy as large as the screen allows.
 *
 * [page] is the room the screen has and [maxHeight] how tall the picture may get in the column it
 * sits in, so a picture is never drawn bigger than the screen showing it. [shape] is how the picture
 * is clipped, which by default is what a picture of the bank is always clipped with: a
 * traffic-situation drawing runs to its own edges and is cut square, everything else is rounded.
 *
 * The enlarged copy is drawn from the painter the inline one already decoded: a second
 * `rememberAsyncImagePainter` for the same URI would flash empty for a frame, and two painters
 * sharing one `drawSize` fight over which size to request the image at.
 */
@Composable
internal fun EnlargeableImage(
    uri: String,
    page: DpSize,
    modifier: Modifier = Modifier,
    maxHeight: Dp = ImageMaxHeight,
    shape: Shape = pictureShape(uri),
) {
    // The pictures are drawn bigger than they are, so the sharper filter is what keeps their edges
    // from turning into steps: bicubic on Skia, the platform's best on Android.
    val painter = rememberAsyncImagePainter(uri, filterQuality = FilterQuality.High)
    val imageState by painter.state.collectAsState()
    val density = LocalDensity.current

    /** Whether the enlarged copy is open. */
    var enlarged by remember(uri) { mutableStateOf(false) }

    val loaded = imageState
    if (loaded is AsyncImagePainter.State.Success) {
        val intrinsic = with(density) { painter.intrinsicSize.toDpSize() }
        BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val room = DpSize(maxWidth, minOf(maxHeight, page.height))
            val size = scaledImageSize(intrinsic, maxWidth = room.width, maxHeight = room.height)
            Image(
                painter = painter,
                contentDescription = strings.enlargeImage,
                modifier = Modifier
                    .size(size)
                    .clip(shape)
                    .clickable { enlarged = true },
                contentScale = ContentScale.Fit,
            )
            if (enlarged) {
                EnlargedImage(painter = loaded.painter, intrinsic = intrinsic, onDismiss = { enlarged = false })
            }
        }
    } else {
        // Keep the space, so what surrounds the picture doesn't jump when it arrives.
        Spacer(modifier.height(minOf(maxHeight, page.height)))
    }
}

/**
 * One picture, as large as the screen allows, over the screen it belongs to. It's closed by tapping
 * anywhere - including the picture itself - or with the button in the corner.
 */
@Composable
private fun EnlargedImage(painter: Painter, intrinsic: DpSize, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(EnlargedImageScrim)
                .clickable(onClick = onDismiss)
                .padding(EnlargedImageMargin),
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints(contentAlignment = Alignment.Center) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.size(enlargedImageSize(intrinsic, DpSize(maxWidth, maxHeight))),
                )
            }
            RoundButton(
                icon = IconClose,
                contentDescription = strings.closeImage,
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

/**
 * The size a picture is drawn at: [IMAGE_SCALE] times its own size, shrunk to fit in [maxWidth] x
 * [maxHeight] without distorting it. A picture that already fills the room doesn't grow, and a tiny
 * one is never blown up past the scale above.
 *
 * A picture we don't know the size of yet is drawn at [DpSize.Zero], so the caller can hold the
 * space it will take instead of drawing nothing.
 */
internal fun scaledImageSize(intrinsic: DpSize, maxWidth: Dp, maxHeight: Dp): DpSize {
    if (!intrinsic.isDrawable) return DpSize.Zero
    return (intrinsic * IMAGE_SCALE).fittedInto(maxWidth, maxHeight)
}

/**
 * The size of the enlarged copy of a picture [intrinsic] dp across: as big as [viewport] allows,
 * keeping its aspect ratio. The copy is there to be looked at, so it is scaled to fill the room
 * rather than drawn at the size it has inline - a sign in a list is drawn small enough to read the
 * list, and twice that is still small. A picture whose size isn't known fills the viewport.
 */
internal fun enlargedImageSize(intrinsic: DpSize, viewport: DpSize): DpSize {
    if (!intrinsic.isDrawable) return viewport
    return intrinsic.fittedInto(viewport.width, viewport.height, shrinkOnly = false)
}

/** [this] scaled to fit in [maxWidth] x [maxHeight], keeping its aspect ratio. */
private fun DpSize.fittedInto(maxWidth: Dp, maxHeight: Dp, shrinkOnly: Boolean = true): DpSize {
    val scale = minOf(maxWidth / width, maxHeight / height)
    return if (shrinkOnly) {
        if (scale >= 1f) this else this * scale
    } else {
        this * scale
    }
}

/** Whether this is a size something can be drawn at. */
private val DpSize.isDrawable: Boolean
    get() = isSpecified && width.isFinite && height.isFinite && width > 0.dp && height > 0.dp

// ── Previews ──

/**
 * The picture before it has loaded, which is the only state a preview can reach: a preview has no
 * way to fetch from the bundled resources. It holds the room the real picture takes, so the layout
 * a preview shows is the layout the app draws.
 */
@Preview(widthDp = 400, heightDp = 200)
@Composable
private fun EnlargeableImagePreview() {
    AppPreview {
        Box(Modifier.fillMaxSize().padding(16.dp)) {
            EnlargeableImage(
                uri = "preview://minv-sk/obr3/dz/140.jpg",
                page = DpSize(400.dp, 200.dp),
                maxHeight = ImageSideBySideMaxHeight,
            )
        }
    }
}

/** The same, in the narrow column a sign of the road-sign list is drawn in. */
@Preview(widthDp = 200, heightDp = 120)
@Composable
private fun EnlargeableImageNarrowPreview() {
    AppPreview {
        Box(Modifier.fillMaxSize().padding(8.dp)) {
            EnlargeableImage(
                uri = "preview://minv-sk/obr3/dz/140.jpg",
                page = DpSize(200.dp, 120.dp),
                maxHeight = 52.dp,
            )
        }
    }
}

package com.spiraclestudios.autoskola.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.spiraclestudios.autoskola.bank.Question
import com.spiraclestudios.autoskola.bank.QuestionBank
import com.spiraclestudios.autoskola.data.banks.QuestionBankRepository
import com.spiraclestudios.autoskola.i18n.strings

/** A video a question is about, as the screens draw it. */
data class VideoItem(
    /** For the platform's player. */
    val uri: String,
    /** See [com.spiraclestudios.autoskola.bank.QuestionVideo.loops]. */
    val loops: Boolean,
)

/** [question]'s video as the screens draw it, or `null` when it has none. */
internal fun QuestionBankRepository.videoItem(bank: QuestionBank, question: Question): VideoItem? =
    question.video?.let { VideoItem(uri = imageUri(bank, it.file), loops = it.loops) }

/**
 * A question's picture, or the video it is about in place of it, which is how the practice and
 * situation screens draw what a question shows. [active] says whether its page is the one on
 * screen: a pager composes the pages beside it ahead of time, and a video there must not play.
 */
@Composable
internal fun QuestionMedia(
    video: VideoItem?,
    imageUri: String?,
    page: DpSize,
    active: Boolean,
    modifier: Modifier = Modifier,
    maxHeight: Dp = ImageMaxHeight,
    onImageError: () -> Unit = {},
) {
    when {
        video != null -> QuestionVideo(video, stillUri = imageUri, active = active, modifier = modifier)
        imageUri != null -> EnlargeableImage(imageUri, page, modifier, maxHeight = maxHeight, onError = onImageError)
    }
}

/**
 * The video a question is about, in place of its picture.
 *
 * A clip that loops plays at once and over and over, as the ministry's own site plays it. A
 * situation to watch shows its [stillUri] with a play button, plays once from start to end when
 * asked to, and then offers to play again: it is the whole of what the question is about, so it
 * starts when the driver is ready rather than while they are still reading.
 *
 * Only while [active] does anything play: a page a pager has composed beside the one on screen
 * shows the still, and a situation to watch that is swiped away stops, to be played again when
 * the driver comes back to it.
 *
 * A video plays without sound, and in a preview not at all: a preview has no player, so it shows the
 * still.
 */
@Composable
internal fun QuestionVideo(
    video: VideoItem,
    stillUri: String?,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val preview = LocalInspectionMode.current
    var playing by rememberSaveable(video.uri) { mutableStateOf(video.loops) }
    var watched by rememberSaveable(video.uri) { mutableStateOf(false) }
    LaunchedEffect(active) {
        if (!active && !video.loops) playing = false
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(VIDEO_ASPECT_RATIO)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        // Under the player too: it covers the still once its first frame is drawn, and until then
        // the still is what the driver sees rather than a black box.
        stillUri?.let { Still(it) }
        if (playing && active && !preview) {
            VideoSurface(
                uri = video.uri,
                loops = video.loops,
                onEnded = {
                    playing = false
                    watched = true
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else if (!video.loops) {
            PlayButton(watched = watched, onClick = { playing = true })
        }
    }
}

@Composable
private fun Still(uri: String) {
    Image(
        painter = rememberAsyncImagePainter(uri, filterQuality = FilterQuality.High),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit,
    )
}

/** Starts the video, or once it has been watched, starts it again. */
@Composable
private fun PlayButton(watched: Boolean, onClick: () -> Unit) {
    PrimaryPill(
        text = if (watched) strings.watchVideoAgain else strings.playVideo,
        onClick = onClick,
        icon = if (watched) Icons.Filled.Replay else Icons.Filled.PlayArrow,
    )
}

/**
 * Plays the video at [uri] without sound, filling [modifier]'s bounds and keeping its proportions,
 * from the moment it is composed until it leaves the composition. One that [loops] starts again at
 * its end; one that does not stops there and calls [onEnded].
 *
 * Nothing may be drawn over it: on the web it is an HTML element above the page's canvas.
 */
@Composable
internal expect fun VideoSurface(
    uri: String,
    loops: Boolean,
    onEnded: () -> Unit,
    modifier: Modifier,
)

/** The site's videos are 16:9; a clip a little wider is drawn with thin bars above and below. */
private const val VIDEO_ASPECT_RATIO = 16f / 9f

// ── Previews ──

/** A situation to watch, before it has been played: its still and the button that plays it. */
@Preview(widthDp = 400, heightDp = 300)
@Composable
private fun QuestionVideoPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("a situation to watch, not played yet")
            QuestionVideo(
                VideoItem(uri = "preview://md-cz/situation.mp4", loops = false),
                stillUri = "preview://md-cz/situation.png",
                active = true,
            )
        }
    }
}

/** A question with no video: its picture, as the bank's other questions show it. */
@Preview(widthDp = 400, heightDp = 300)
@Composable
private fun QuestionMediaPicturePreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("a question with a picture and no video")
            QuestionMedia(
                video = null,
                imageUri = "preview://minv-sk/obr3/dz/140.jpg",
                page = DpSize(400.dp, 300.dp),
                active = true,
            )
        }
    }
}

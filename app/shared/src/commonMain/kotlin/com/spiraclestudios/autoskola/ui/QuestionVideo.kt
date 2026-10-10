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
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.spiraclestudios.autoskola.i18n.strings

/** A video a question is about, as the screens draw it. */
data class VideoItem(
    /** For the platform's player. */
    val uri: String,
    /** See [com.spiraclestudios.autoskola.bank.QuestionVideo.loops]. */
    val loops: Boolean,
)

/**
 * The video a question is about, in place of its picture.
 *
 * A clip that loops plays at once and over and over, as the ministry's own site plays it. A
 * situation to watch shows its [stillUri] with a play button, plays once from start to end when
 * asked to, and then offers to play again: it is the whole of what the question is about, so it
 * starts when the driver is ready rather than while they are still reading.
 *
 * A video plays without sound, and in a preview not at all: a preview has no player, so it shows the
 * still.
 */
@Composable
internal fun QuestionVideo(video: VideoItem, stillUri: String?, modifier: Modifier = Modifier) {
    val preview = LocalInspectionMode.current
    var playing by rememberSaveable(video.uri) { mutableStateOf(video.loops) }
    var watched by rememberSaveable(video.uri) { mutableStateOf(false) }
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
        if (playing && !preview) {
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
            )
        }
    }
}

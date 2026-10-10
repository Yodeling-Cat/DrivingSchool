package com.spiraclestudios.autoskola.ui

import android.view.TextureView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer

/**
 * Media3's ExoPlayer, drawing into a [TextureView]: unlike a SurfaceView, it scrolls and clips with
 * the page around it. A bundled file's URI points into the APK's assets, which ExoPlayer reads as it
 * is. The video pauses while the app is in the background and goes on when it comes back.
 */
@Composable
internal actual fun VideoSurface(
    uri: String,
    loops: Boolean,
    onEnded: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val ended by rememberUpdatedState(onEnded)

    /** Width over height, once the player knows it; until then the surface fills the room. */
    var aspectRatio by remember(uri) { mutableStateOf<Float?>(null) }
    val player = remember(uri, loops) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = if (loops) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            volume = 0f
            prepare()
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) ended()
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    aspectRatio = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    LifecycleStartEffect(player) {
        player.play()
        onStopOrDispose { player.pause() }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { TextureView(it).also(player::setVideoTextureView) },
            modifier = aspectRatio?.let { Modifier.aspectRatio(it) } ?: Modifier.fillMaxSize(),
        )
    }
}

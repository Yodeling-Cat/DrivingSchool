package com.spiraclestudios.autoskola.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerLayer
import platform.CoreGraphics.CGRectMake
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.UIKit.UIView

/**
 * AVFoundation's player in a view of its own, whose layer follows the view's size. A bundled file's
 * URI is a `file://` URL inside the app. A clip that loops is sent back to its start when it ends.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun VideoSurface(
    uri: String,
    loops: Boolean,
    onEnded: () -> Unit,
    modifier: Modifier,
) {
    val ended by rememberUpdatedState(onEnded)
    val player = remember(uri) {
        AVPlayer(uRL = NSURL.URLWithString(uri) ?: NSURL.fileURLWithPath(uri)).apply { muted = true }
    }
    DisposableEffect(player, loops) {
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = player.currentItem,
            queue = NSOperationQueue.mainQueue,
        ) { _ ->
            if (loops) {
                player.seekToTime(CMTimeMake(value = 0, timescale = 1))
                player.play()
            } else {
                ended()
            }
        }
        player.play()
        onDispose {
            player.pause()
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
    }
    UIKitView(
        factory = { PlayerView(AVPlayerLayer.playerLayerWithPlayer(player)) },
        modifier = modifier,
    )
}

/** A view that keeps [playerLayer] as big as itself, with the video fitted inside it. */
@OptIn(ExperimentalForeignApi::class)
private class PlayerView(private val playerLayer: AVPlayerLayer) : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    init {
        playerLayer.videoGravity = AVLayerVideoGravityResizeAspect
        layer.addSublayer(playerLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        playerLayer.frame = bounds
    }
}

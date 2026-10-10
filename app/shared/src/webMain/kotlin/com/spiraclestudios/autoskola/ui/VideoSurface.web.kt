package com.spiraclestudios.autoskola.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.WebElementView
import kotlinx.browser.document
import org.w3c.dom.HTMLVideoElement

/**
 * The browser's own `<video>` element, laid over the page's canvas where the composable is. A
 * bundled file is served next to the page, so its URI is a path the element loads as it is.
 * `playsinline` keeps Safari on an iPhone from taking it full screen.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun VideoSurface(
    uri: String,
    loops: Boolean,
    onEnded: () -> Unit,
    modifier: Modifier,
) {
    val ended by rememberUpdatedState(onEnded)
    WebElementView(
        factory = {
            (document.createElement("video") as HTMLVideoElement).apply {
                src = uri
                muted = true
                loop = loops
                autoplay = true
                setAttribute("playsinline", "")
                style.width = "100%"
                style.height = "100%"
                style.setProperty("object-fit", "contain")
                addEventListener("ended", { ended() })
            }
        },
        modifier = modifier,
        onRelease = { video ->
            // Stop it loading: an element that is only detached carries on fetching its file.
            video.pause()
            video.removeAttribute("src")
            video.load()
        },
    )
}

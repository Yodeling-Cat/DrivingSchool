package com.spiraclestudios.autoskola.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.spiraclestudios.autoskola.data.ads.adRequest
import com.spiraclestudios.autoskola.data.ads.adUnitId
import com.spiraclestudios.autoskola.isDebugBuild
import kotlin.math.roundToInt

/** An [AdView] that has been asked for an ad, as [RetainedBanner] keeps it. */
private class PlayBanner(val view: AdView) : LoadedBanner {
    override fun destroy() = view.destroy()
}

/**
 * The banner, as an [AdView] that [state] keeps.
 *
 * The width is measured rather than assumed: an anchored adaptive banner is asked for the width the
 * placement actually has, and that is the whole reason the request carries a size at all. A fixed
 * 320x50 would be the wrong shape on a tablet and a stripe of nothing in a narrow column, which is
 * what `plans/ads.md` records about where the size is chosen.
 *
 * The view belongs to [state], not to this composable. Each time the placement is drawn it gets a fresh
 * frame and the loaded view is moved into it, so a banner scrolled out of a lazy list and back is the
 * same ad, at once, rather than a new request. A new width - a rotation, a resized window - asks for a
 * new one rather than stretching an ad that was rendered for the old one.
 */
@Composable
internal actual fun PlatformAdBanner(state: AdBannerState, modifier: Modifier) {
    val unit = adUnitId(state.slot, debug = isDebugBuild()) ?: return
    val context = LocalContext.current
    val density = LocalDensity.current
    // From the width the banner was last asked for, so a placement coming back keeps its height
    // instead of collapsing to nothing until it is measured again.
    var widthDp by remember(state) { mutableStateOf(state.banner.widthDp) }

    Box(
        modifier = modifier.fillMaxWidth().onSizeChanged {
            val measured = with(density) { it.width.toDp().value.roundToInt() }
            if (measured > 0 && measured != widthDp) widthDp = measured
        },
    ) {
        if (widthDp <= 0) return@Box
        key(widthDp) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx -> FrameLayout(ctx) },
                update = { frame ->
                    val banner = state.banner.at(widthDp) { width ->
                        PlayBanner(
                            AdView(context).apply {
                                adUnitId = unit
                                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width))
                                loadAd(adRequest())
                            },
                        )
                    } as PlayBanner
                    // Still inside the frame of the place it was last drawn, which the lazy list let go
                    // of: an Android view can have one parent.
                    if (banner.view.parent !== frame) {
                        (banner.view.parent as? ViewGroup)?.removeView(banner.view)
                        frame.removeAllViews()
                        frame.addView(banner.view)
                    }
                },
                // The frame goes, the ad stays with the state.
                onRelease = { frame -> frame.removeAllViews() },
            )
        }
    }
}

package com.spiraclestudios.autoskola.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.spiraclestudios.autoskola.content.BundledContent
import com.spiraclestudios.autoskola.data.ads.adRequest
import com.spiraclestudios.autoskola.data.ads.adUnitId
import com.spiraclestudios.autoskola.isDebugBuild
import org.koin.compose.koinInject
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
 * The width is read while the slot is laid out, not after, and an anchored adaptive banner's height
 * follows from its width alone - the SDK works it out on the device, with no request. The slot is that
 * tall whenever [RetainedBanner.takesSpace] says so, and no taller than nothing otherwise: from the
 * first frame for a banner that holds its space while loading, from the ad's arrival for the rest.
 *
 * The view belongs to [state], not to this composable. Each time the placement is drawn it gets a fresh
 * frame and the loaded view is moved into it, so a banner scrolled out of a lazy list and back is the
 * same ad, at once, rather than a new request. A new width - a rotation, a resized window - asks for a
 * new one rather than stretching an ad that was rendered for the old one.
 */
@Composable
internal actual fun PlatformAdBanner(state: AdBannerState, modifier: Modifier) {
    val country = koinInject<BundledContent>().country
    val unit = adUnitId(state.slot, country, debug = isDebugBuild()) ?: return
    val context = LocalContext.current

    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (!constraints.hasBoundedWidth) return@BoxWithConstraints
        val widthDp = maxWidth.value.roundToInt()
        if (widthDp <= 0) return@BoxWithConstraints
        val size = remember(context, widthDp) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
        }
        val height = if (state.banner.takesSpace) size.height.dp else 0.dp
        key(widthDp) {
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(height),
                factory = { ctx -> FrameLayout(ctx) },
                update = { frame ->
                    val banner = state.banner.at(widthDp) {
                        val view = AdView(context).apply {
                            adUnitId = unit
                            setAdSize(size)
                        }
                        PlayBanner(view).also { loaded ->
                            // Set before the request, so the first answer is not missed. A refresh that
                            // fails later leaves the last ad on screen, which `failed` already knows.
                            view.adListener = object : AdListener() {
                                override fun onAdLoaded() = state.banner.loaded(loaded)
                                override fun onAdFailedToLoad(error: LoadAdError) = state.banner.failed(loaded)
                            }
                            view.loadAd(adRequest())
                        }
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

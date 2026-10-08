package com.spiraclestudios.autoskola.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spiraclestudios.autoskola.data.ads.AdSlot

/**
 * One banner placement, drawn where it is put.
 *
 * It draws nothing when there is no unit to draw - the web build, and any target without an SDK - so
 * a placement can be written without asking whether the target supports ads.
 *
 * It does **not** decide whether ads are allowed. That belongs to the screen's own state, which is
 * where the consent answer and the entitlement have already been read into a flag: render this only
 * where the state says ads are on. Putting the check inside here instead would mean every placement
 * silently ignoring the entitlement it cannot see.
 *
 * The size is the request's, not the unit's: the implementation asks for an anchored adaptive banner
 * as wide as the space the placement has been given, which is why a banner belongs in a slot rather
 * than beside something with its own width.
 *
 * **In a lazy list, use the other overload** with a state remembered outside the list.
 */
@Composable
fun AdBanner(slot: AdSlot, modifier: Modifier = Modifier) {
    PlatformAdBanner(rememberAdBannerState(slot), modifier)
}

/**
 * The banner [state] keeps, for a placement inside a lazy list: remember [state] outside the list with
 * [rememberAdBannerState]. A lazy list drops an item that is scrolled off screen, and a banner
 * remembered inside the item goes with it: scrolling back would then be a new ad request, a second of
 * empty space and the content below jumping. Held outside, the item only shows the banner that is
 * already loaded.
 */
@Composable
fun AdBanner(state: AdBannerState, modifier: Modifier = Modifier) {
    PlatformAdBanner(state, modifier)
}

/**
 * A banner pinned to the bottom of a screen: below its content, outside whatever scrolls or pages, so
 * it is on screen from the first frame and stays the same ad while the user scrolls or swipes - a
 * banner inside a pager would be a new request on every page.
 *
 * Capped to [maxWidth] and centered like the content above it, because an adaptive banner as wide as
 * a desktop window is a strip, not a small ad. It sits above the floating navigation pill where a
 * screen has one ([LocalNavBarInset]).
 *
 * **Not drawn at all on a compact height** - a phone on its side - where a bar under the content would
 * take a fifth of the little height there is (`plans/ads.md`, "Placements"), **nor while the keyboard
 * is open**: the bar would ride on top of the keys, which is exactly where the next tap is going, and
 * an ad placed to catch taps meant for something else is what AdMob's policy forbids.
 */
@Composable
fun PinnedAdBanner(slot: AdSlot, modifier: Modifier = Modifier, maxWidth: Dp = ContentWidth.Reading) {
    // Held above the two reasons to step aside, so a bar that hides for the keyboard comes back with
    // the ad it had rather than asking for another when the keyboard closes.
    val banner = rememberAdBannerState(slot)
    if (LocalHeightClass.current == HeightClass.Compact) return
    if (WindowInsets.ime.getBottom(LocalDensity.current) > 0) return
    Box(modifier.fillMaxWidth().padding(bottom = LocalNavBarInset.current), contentAlignment = Alignment.TopCenter) {
        AdBanner(banner, Modifier.widthIn(max = maxWidth).padding(start = 16.dp, end = 16.dp, top = 8.dp))
    }
}

/** The platform's banner for [state]: Google's `AdView` on Android, nothing where there is no SDK. */
@Composable
internal expect fun PlatformAdBanner(state: AdBannerState, modifier: Modifier)

/**
 * A banner placement's loaded ad, kept for as long as the composable that remembered it - the screen,
 * when it is remembered outside a lazy list - and destroyed when that leaves.
 */
@Stable
class AdBannerState internal constructor(val slot: AdSlot) {
    internal val banner = RetainedBanner()
}

/** Remembers [slot]'s banner here, and destroys it when this leaves the composition. */
@Composable
fun rememberAdBannerState(slot: AdSlot): AdBannerState {
    val state = remember(slot) { AdBannerState(slot) }
    DisposableEffect(state) { onDispose { state.banner.release() } }
    return state
}

/** A banner the platform has asked for, as far as keeping it is concerned. */
internal interface LoadedBanner {
    /** Stops it and lets the SDK let go of it. */
    fun destroy()
}

/**
 * The one banner a placement has, so that showing it again is not asking for another.
 *
 * A banner refreshes itself on the interval set in AdMob; a new request every time it comes back on
 * screen would only add requests, lowering the fill rate and showing nothing while it loads.
 */
internal class RetainedBanner {
    private var loaded: LoadedBanner? = null

    /**
     * The width the banner was last asked for, in dp, or 0 before it was. A placement that comes back
     * starts at this width, so its slot keeps its size instead of collapsing until it is measured.
     */
    var widthDp: Int = 0
        private set

    /**
     * The banner for [widthDp]: the one already loaded if it was asked for at that width, otherwise a new
     * one from [load], with the old one destroyed - an ad rendered for another width is the wrong shape.
     */
    fun at(widthDp: Int, load: (widthDp: Int) -> LoadedBanner): LoadedBanner {
        loaded?.let { if (widthDp == this.widthDp) return it }
        release()
        return load(widthDp).also {
            loaded = it
            this.widthDp = widthDp
        }
    }

    /** Destroys the banner, if there is one; the next [at] asks again. */
    fun release() {
        loaded?.destroy()
        loaded = null
    }
}

// ── Previews ──

/**
 * Where the pinned banner sits. A preview has no ad SDK, so the bar is drawn empty: what this shows
 * is that it takes no room on a compact height and is capped and centered on a wide one.
 */
@Preview(widthDp = 400, heightDp = 300)
@Composable
private fun PinnedAdBannerPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("Pinned under the content (empty: the ad is the SDK's)")
            PinnedAdBanner(AdSlot.SituationBanner)
        }
    }
}

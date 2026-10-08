package com.spiraclestudios.autoskola.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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

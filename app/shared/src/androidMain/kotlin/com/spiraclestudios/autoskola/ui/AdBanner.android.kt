package com.spiraclestudios.autoskola.ui

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
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.spiraclestudios.autoskola.data.ads.AdSlot
import com.spiraclestudios.autoskola.data.ads.adUnitId
import com.spiraclestudios.autoskola.isDebugBuild
import kotlin.math.roundToInt

/**
 * The banner, as an [AdView].
 *
 * The width is measured rather than assumed: an anchored adaptive banner is asked for the width the
 * placement actually has, and that is the whole reason the request carries a size at all. A fixed
 * 320x50 would be the wrong shape on a tablet and a stripe of nothing in a narrow column, which is
 * what `plans/ads.md` records about where the size is chosen.
 *
 * The view is keyed on the measured width, so a rotation or a resize builds a new one and asks again
 * rather than stretching an ad that was rendered for the old one - and, just as importantly, so that
 * recomposition does not re-request an ad the user is already looking at.
 */
@Composable
actual fun AdBanner(slot: AdSlot, modifier: Modifier) {
    val unit = adUnitId(slot, debug = isDebugBuild()) ?: return
    val context = LocalContext.current
    val density = LocalDensity.current
    var widthDp by remember { mutableStateOf(0) }

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
                factory = { ctx ->
                    AdView(ctx).apply {
                        adUnitId = unit
                        setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
                        loadAd(AdRequest.Builder().build())
                    }
                },
            )
        }
    }
}

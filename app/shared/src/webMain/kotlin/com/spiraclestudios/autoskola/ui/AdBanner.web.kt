package com.spiraclestudios.autoskola.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.spiraclestudios.autoskola.data.ads.AdSlot

/**
 * Nothing in a browser.
 *
 * AdMob does not serve pages, so there is no unit for a browser to draw and nothing for this to do.
 * Web ads are their own job with their own vendor and their own consent mechanism, written up as
 * `plans/ads.md` step 15.
 */
@Composable
actual fun AdBanner(slot: AdSlot, modifier: Modifier) = Unit

package com.spiraclestudios.autoskola.data.ads

/**
 * No ads in a browser, and none to come from this SDK.
 *
 * AdMob serves apps, not pages, so the web build has no ad implementation and cannot get one from
 * here. Web ads are a different job with a different vendor and a different consent mechanism,
 * written up as `plans/ads.md` step 15.
 */
actual fun platformAdDataSource(country: String): AdDataSource = NoOpAdDataSource()

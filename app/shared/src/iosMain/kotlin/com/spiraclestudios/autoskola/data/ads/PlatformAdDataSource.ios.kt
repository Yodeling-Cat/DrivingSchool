package com.spiraclestudios.autoskola.data.ads

/**
 * No ads on iOS yet.
 *
 * Google's SDK is a Swift package linked from `app/iosApp`, which needs a Mac to do and is
 * `plans/ads.md` step 13. Until then this reports [AdDataSource.available] as false, so the app
 * behaves on iOS exactly as it would if ads were unavailable rather than as though they were on and
 * failing - and the banner actual draws nothing.
 *
 * The Android build is what ships first (see `plans/ads.md`, step 13).
 */
actual fun platformAdDataSource(country: String): AdDataSource = NoOpAdDataSource()

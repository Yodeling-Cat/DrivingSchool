package com.spiraclestudios.autoskola.data.ads

import com.spiraclestudios.autoskola.applicationContext

/**
 * Google's ads SDK, once the entry point has handed over an application.
 *
 * Without one — in a host test, or in a preview — this answers the no-op source rather than failing.
 * That is the same shape as [AdRepository]'s other refusals: nothing can be asked of an SDK with no
 * context, so nothing is, and the app behaves as it does on a target with no ads at all.
 */
actual fun platformAdDataSource(): AdDataSource {
    val context = applicationContext() ?: return NoOpAdDataSource()
    return GoogleMobileAdsDataSource(context)
}

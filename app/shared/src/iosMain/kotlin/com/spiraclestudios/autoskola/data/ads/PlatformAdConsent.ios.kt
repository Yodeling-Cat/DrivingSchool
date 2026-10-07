package com.spiraclestudios.autoskola.data.ads

/** No form on iOS until the SDK is linked (`plans/ads.md` step 13), so nothing is requested. */
actual fun platformAdConsent(): AdConsent = NoAdConsent()

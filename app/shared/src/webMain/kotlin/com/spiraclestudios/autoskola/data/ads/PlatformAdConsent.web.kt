package com.spiraclestudios.autoskola.data.ads

/** No form in a browser: the web build has no ads, and its eventual consent is a different mechanism. */
actual fun platformAdConsent(): AdConsent = NoAdConsent()

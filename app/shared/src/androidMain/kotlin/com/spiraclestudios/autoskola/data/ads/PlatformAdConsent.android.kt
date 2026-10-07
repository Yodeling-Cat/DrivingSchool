package com.spiraclestudios.autoskola.data.ads

import com.spiraclestudios.autoskola.applicationContext

/**
 * Google's consent form, once the entry point has handed over an application.
 *
 * Without one — in a host test, or in a preview — this answers [NoAdConsent], which asks for nothing,
 * the same refusal [platformAdDataSource] makes.
 */
actual fun platformAdConsent(): AdConsent {
    val context = applicationContext() ?: return NoAdConsent()
    return UmpAdConsent(context)
}

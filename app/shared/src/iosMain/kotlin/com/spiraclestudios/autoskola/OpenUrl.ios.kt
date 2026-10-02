package com.spiraclestudios.autoskola

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/** Opens the address in Safari, or whichever browser the user has set as the default. */
actual fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
}

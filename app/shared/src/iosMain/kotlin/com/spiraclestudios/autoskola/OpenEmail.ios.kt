package com.spiraclestudios.autoskola

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * Mail is a system scheme, so `canOpenURL` answers for it without the app declaring it anywhere,
 * and the mail app is asked to open the report already written.
 */
actual fun openEmail(url: String): Boolean {
    val nsUrl = NSURL.URLWithString(url) ?: return false
    val application = UIApplication.sharedApplication
    if (!application.canOpenURL(nsUrl)) return false
    application.openURL(nsUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
    return true
}

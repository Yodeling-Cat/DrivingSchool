package com.spiraclestudios.autoskola

import android.content.Intent
import android.net.Uri

/**
 * `ACTION_SENDTO` on a `mailto:` address is the intent only mail apps answer, so the chooser
 * cannot offer a chat app for a report that is meant as mail. It is started from the application
 * context, so it carries `NEW_TASK`; a device with nothing handling it is the one failure the
 * caller is told about.
 */
actual fun openEmail(url: String): Boolean {
    val context = applicationContext() ?: return false
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return runCatching { context.startActivity(intent) }.isSuccess
}

/** A phone has an email app to open a report in; whether this one has is what [openEmail] answers. */
actual fun hasEmailApp(): Boolean = true

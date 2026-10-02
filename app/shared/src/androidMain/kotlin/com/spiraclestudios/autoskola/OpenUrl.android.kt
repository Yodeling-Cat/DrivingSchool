package com.spiraclestudios.autoskola

import android.content.Intent
import android.net.Uri

/**
 * Hands the address to the browser. The intent is started from the application context, so it
 * carries `NEW_TASK`: the shared module has no activity to start it from.
 */
actual fun openUrl(url: String) {
    val context = applicationContext() ?: return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

package com.spiraclestudios.autoskola

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * Puts [text] on the system clipboard, where the mail app the user writes in can paste it.
 *
 * Nothing is answered: the clipboard on a phone is a local service that either takes the clip or
 * is not there at all, and the screen that offers the copy shows the same text in a selection as
 * well, so there is a way through without it.
 */
actual fun copyToClipboard(text: String) {
    val context = applicationContext() ?: return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    // No label: Android shows one in its own clipboard preview, and the app has no word for it that
    // is not a translation of "copied" in a place the user is not looking.
    runCatching { clipboard.setPrimaryClip(ClipData.newPlainText(null, text)) }
}

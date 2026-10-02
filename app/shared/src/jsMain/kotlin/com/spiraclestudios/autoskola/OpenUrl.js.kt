@file:Suppress("MatchingDeclarationName")

package com.spiraclestudios.autoskola

/**
 * A new tab rather than the current one: the app is the page, and navigating it away would end the
 * session the user is in the middle of.
 */
actual fun openUrl(url: String) {
    js("window.open(url, '_blank')")
}

@file:Suppress("MatchingDeclarationName")

package com.spiraclestudios.autoskola

import kotlinx.browser.window

/** The browser's own clipboard, which the mail app the page's user writes in can paste from. */
actual fun copyToClipboard(text: String) {
    window.navigator.clipboard.writeText(text)
}

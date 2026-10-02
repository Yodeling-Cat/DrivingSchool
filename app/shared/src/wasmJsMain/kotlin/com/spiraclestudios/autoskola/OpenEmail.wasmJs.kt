@file:Suppress("MatchingDeclarationName")

package com.spiraclestudios.autoskola

import kotlinx.browser.window

/**
 * The address bar rather than a new tab: a browser hands `mailto:` to whatever the system opens
 * for mail and stays on the page, where a new tab would only come up blank. Whether the machine
 * has a mail handler at all is not something a page can ask, so this answers that it was handed
 * over.
 */
actual fun openEmail(url: String): Boolean {
    window.location.href = url
    return true
}

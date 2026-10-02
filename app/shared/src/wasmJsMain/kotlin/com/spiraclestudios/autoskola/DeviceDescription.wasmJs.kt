package com.spiraclestudios.autoskola

import kotlinx.browser.window

/**
 * A page has no phone of its own, so what it reports is the browser's user-agent string: the
 * browser, and the system it is running on. Reading it needs no permission, and the user is shown
 * it in the report before they send anything.
 */
actual fun deviceDescription(): String? = window.navigator.userAgent.takeIf { it.isNotBlank() }

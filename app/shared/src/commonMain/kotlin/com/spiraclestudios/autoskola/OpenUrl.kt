package com.spiraclestudios.autoskola

/**
 * Opens [url] in whatever the platform uses to show a web page: the user's browser on Android and
 * iOS, a new tab on the web.
 *
 * The consent screen needs it for the privacy policy and the terms, which live outside the app.
 * Nothing else in the app leaves it, and a failure to open is not worth interrupting the user for.
 */
expect fun openUrl(url: String)

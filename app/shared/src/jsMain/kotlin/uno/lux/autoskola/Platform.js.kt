package uno.lux.autoskola

import kotlinx.browser.window
import web.navigator.navigator

class JsPlatform : Platform {
    private val userAgent = navigator.userAgent
    private val browserList = listOf("Chrome", "Firefox", "Safari", "Edge")

    override val name: String = userAgent.findAnyOf(browserList, ignoreCase = true)
        ?.let { (startIndex) -> userAgent.substring(startIndex).substringBefore(" ") }
        ?: "Unknown"
}

actual fun getPlatform(): Platform = JsPlatform()

/** The hosts the development web server serves on; a deployed build is not served from one. */
private val DEV_SERVER_HOSTS = setOf("localhost", "127.0.0.1", "[::1]")

/**
 * The web has no debug build type of its own, so the development server is what stands in for one:
 * `jsBrowserDevelopmentRun` serves the app on a loopback host and a deployed bundle does not.
 */
actual fun isDebugBuild(): Boolean = window.location.hostname in DEV_SERVER_HOSTS

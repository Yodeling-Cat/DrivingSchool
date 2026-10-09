package com.spiraclestudios.autoskola

import kotlinx.browser.window

/** The hosts the development web server serves on; a deployed build is not served from one. */
private val DEV_SERVER_HOSTS = setOf("localhost", "127.0.0.1", "[::1]")

/**
 * The web has no debug build type of its own, so the development server is what stands in for one:
 * `jsBrowserDevelopmentRun` (or `wasmJsBrowserDevelopmentRun`) serves the app on a loopback host and a deployed bundle does not.
 */
actual fun isDebugBuild(): Boolean = window.location.hostname in DEV_SERVER_HOSTS

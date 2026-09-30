package uno.lux.autoskola

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

/**
 * Whether this build is one a developer runs rather than one a user has: the practice screen's
 * shortcuts for filling a test in are kept for these builds only.
 */
expect fun isDebugBuild(): Boolean

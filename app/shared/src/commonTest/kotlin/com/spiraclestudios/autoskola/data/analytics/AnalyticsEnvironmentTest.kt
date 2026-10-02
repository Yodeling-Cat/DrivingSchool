package com.spiraclestudios.autoskola.data.analytics

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The two environment values are a contract with the dashboard: renaming one silently splits every
 * insight that filters on it, and nothing else in the app would notice.
 */
class AnalyticsEnvironmentTest {

    @Test
    fun aDevelopersBuildReportsItselfAsDebug() {
        assertEquals("debug", analyticsEnvironment(developmentBuild = true))
    }

    @Test
    fun aUsersBuildReportsItselfAsRelease() {
        assertEquals("release", analyticsEnvironment(developmentBuild = false))
    }
}

package com.spiraclestudios.autoskola

/**
 * The version this web bundle was built from, which the Gradle build stamps into a generated source
 * (`app/shared/build.gradle.kts`): a page is served from a file rather than installed, so nothing
 * else on it would know which build it is.
 *
 * It is the same number an Android install of that build reports. iOS is the one platform whose
 * version comes from its own project, which nothing in this build can reach.
 */
actual fun appVersion(): String? = WEB_APP_VERSION

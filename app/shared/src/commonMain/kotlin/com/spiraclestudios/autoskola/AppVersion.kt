package com.spiraclestudios.autoskola

/**
 * The version of the app the user is running, as the platform reports it, or `null` where the
 * platform has no such thing.
 *
 * Android reads the application module's `versionName` by name: the shared module is a library and
 * generates no `BuildConfig` of its own (see [isDebugBuild]), and a host test has no application
 * module at all. iOS reads its bundle's marketing version. The web bundle is served from a file
 * rather than installed, so the Gradle build stamps its version into the source (see
 * `app/shared/build.gradle.kts`). `null` is then the answer of a platform that cannot say.
 */
expect fun appVersion(): String?

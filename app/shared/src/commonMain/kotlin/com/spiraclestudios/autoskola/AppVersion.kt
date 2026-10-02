package com.spiraclestudios.autoskola

/**
 * The version of the app the user is running, as the platform reports it, or `null` where the
 * platform has no such thing.
 *
 * Android reads the application module's `versionName` by name: the shared module is a library and
 * generates no `BuildConfig` of its own (see [isDebugBuild]), and a host test has no application
 * module at all. iOS reads its bundle's marketing version. The web build is served from whatever
 * was deployed last and carries no version, so it answers `null` and the About screen leaves the
 * line out.
 */
expect fun appVersion(): String?

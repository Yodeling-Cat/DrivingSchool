package com.spiraclestudios.autoskola

/**
 * The app's build type, which is what "a debug build" means on Android. The shared module has no
 * build type of its own - the multiplatform library plugin offers no BuildConfig - so the app's is
 * read by name. A release build that no longer carries the field, or a host test, answers false.
 */
actual fun isDebugBuild(): Boolean = runCatching {
    val buildConfig = Class.forName("com.spiraclestudios.autoskola.BuildConfig")
    buildConfig.getField("DEBUG").getBoolean(null)
}.getOrDefault(false)

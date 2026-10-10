package com.spiraclestudios.autoskola

/**
 * The app's build type, which is what "a debug build" means on Android. The shared module has no
 * build type of its own - the multiplatform library plugin offers no BuildConfig - so the app's is
 * read by name. A release build that no longer carries the field, or a host test, answers false.
 */
actual fun isDebugBuild(): Boolean = appBuildConfig("DEBUG") as? Boolean ?: false

/**
 * The field [name] of the app's BuildConfig, or `null` where there is none (a host test). The app's
 * id (`APPLICATION_ID`) and its version (`VERSION_NAME`) are read the same way.
 */
internal fun appBuildConfig(name: String): Any? = runCatching {
    Class.forName("com.spiraclestudios.autoskola.BuildConfig").getField(name).get(null)
}.getOrNull()

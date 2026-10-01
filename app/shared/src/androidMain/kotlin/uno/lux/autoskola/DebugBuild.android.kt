package uno.lux.autoskola

/**
 * The app's build type, which is what "a debug build" means on Android. The shared module has no
 * build type of its own - the multiplatform library plugin offers no BuildConfig - so the app's is
 * read by name. A release build that no longer carries the field, or a host test, answers false.
 */
actual fun isDebugBuild(): Boolean = runCatching {
    val buildConfig = Class.forName("uno.lux.autoskola.BuildConfig")
    buildConfig.getField("DEBUG").getBoolean(null)
}.getOrDefault(false)

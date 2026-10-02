package com.spiraclestudios.autoskola

/**
 * The version name the application module declares, read out of its `BuildConfig` by name. The
 * shared module is a library and generates none of its own, so a host test has nothing to read and
 * answers `null`.
 */
actual fun appVersion(): String? = runCatching {
    Class.forName("com.spiraclestudios.autoskola.BuildConfig")
        .getField("VERSION_NAME")
        .get(null) as? String
}.getOrNull()

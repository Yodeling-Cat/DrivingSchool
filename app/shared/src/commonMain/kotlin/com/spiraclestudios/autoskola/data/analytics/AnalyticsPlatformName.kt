package com.spiraclestudios.autoskola.data.analytics

/**
 * The platform an event came from, in lowercase, as a super property (see
 * [ANALYTICS_PLATFORM_PROPERTY]).
 *
 * Each target has one file answering for itself, the same way `appVersion` and `isDebugBuild` do.
 * The value is a constant per target rather than something read at runtime: the source set a file
 * is compiled into *is* the platform, and the JS and Wasm builds are the same deployment, so both
 * answer "web".
 */
internal expect fun platformName(): String

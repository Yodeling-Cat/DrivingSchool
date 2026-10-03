package com.spiraclestudios.autoskola.data.ads

/**
 * The ads implementation for the target the app is running on.
 *
 * It is an `expect` rather than a Koin condition because the difference is a build target rather
 * than a runtime choice: Android has Google's SDK, iOS has nothing until step 13, and the web has
 * nothing and cannot have anything from this SDK. Making it a target difference means a new target
 * does not compile until somebody says what its ads are, which is the same reason [ReleaseAdUnits]
 * is one.
 *
 * What each one answers is a *source*, not a decision: whether anything is ever asked of it is
 * [AdRepository]'s, and every one of these is behind that.
 */
expect fun platformAdDataSource(): AdDataSource

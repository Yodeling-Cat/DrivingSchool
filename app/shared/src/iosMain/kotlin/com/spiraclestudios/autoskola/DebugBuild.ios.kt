package com.spiraclestudios.autoskola

import kotlin.experimental.ExperimentalNativeApi

/**
 * Kotlin/Native knows whether this binary was built for debugging. The object is written out in
 * full, so it is clear this is kotlin.native.Platform and not one of the `platform.*` types.
 */
@OptIn(ExperimentalNativeApi::class)
actual fun isDebugBuild(): Boolean = kotlin.native.Platform.isDebugBinary

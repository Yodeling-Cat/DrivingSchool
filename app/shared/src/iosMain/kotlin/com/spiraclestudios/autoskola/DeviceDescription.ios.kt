package com.spiraclestudios.autoskola

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.UIKit.UIDevice
import platform.posix.uname
import platform.posix.utsname

/** The hardware identifier the kernel reports and the iOS version it runs, e.g. "iPhone15,2 · iOS 17.4". */
actual fun deviceDescription(): String? = listOf(
    machineName() ?: UIDevice.currentDevice.model,
    "${UIDevice.currentDevice.systemName} ${UIDevice.currentDevice.systemVersion}",
)
    .filter { it.isNotBlank() }
    .joinToString(" · ")
    .takeIf { it.isNotBlank() }

/**
 * The phone's own name for itself - `iPhone15,2` - which is the one thing that says which model
 * this is: `UIDevice.model` is only ever "iPhone", and the device's *name* is whatever its owner
 * called it, which is not ours to read.
 */
@OptIn(ExperimentalForeignApi::class)
private fun machineName(): String? = memScoped {
    val info = alloc<utsname>()
    if (uname(info.ptr) != 0) return@memScoped null
    info.machine.toKString().takeIf { it.isNotBlank() }
}

package com.spiraclestudios.autoskola

import android.os.Build

/**
 * Who made the phone, what it is, and which Android it runs. The application module is not on
 * Android, and a host test has none of it to read: the fields then come back blank and the line
 * is left out, rather than the screen failing to draw.
 */
actual fun deviceDescription(): String? {
    val brand = Build.MANUFACTURER?.trim().orEmpty()
    val model = Build.MODEL?.trim().orEmpty()
    val device = when {
        model.isEmpty() -> brand

        // Some makers put their own name in the model already, and "samsung SM-A536B" is not
        // what the phone calls itself.
        brand.isEmpty() || model.startsWith(brand, ignoreCase = true) -> model

        else -> "$brand $model"
    }
    val release = Build.VERSION.RELEASE?.trim().orEmpty()
    val system = listOf("Android", release).filter { it.isNotEmpty() }.joinToString(" ")

    return listOf(device, "$system (API ${Build.VERSION.SDK_INT})")
        .filter { it.isNotEmpty() }
        .joinToString(" · ")
        .takeIf { it.isNotEmpty() }
}

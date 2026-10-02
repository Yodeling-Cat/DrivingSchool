package com.spiraclestudios.autoskola.about

import com.mikepenz.aboutlibraries.entity.Library

/** One library as AboutLibraries describes it, with the fields a search or a list reads. */
internal fun testLibrary(name: String, uniqueId: String, version: String? = null) = Library(
    uniqueId = uniqueId,
    artifactVersion = version,
    name = name,
    description = null,
    website = null,
    developers = emptyList(),
    organization = null,
    scm = null,
)

/** Three libraries of the app's own stack, as the generated document lists them. */
internal val testLibraries = listOf(
    testLibrary("Koin", "io.insert-koin:koin-core", "4.2.2"),
    testLibrary("coil", "io.coil-kt.coil3:coil", "3.4.0"),
    testLibrary("Compose UI", "androidx.compose.ui:ui", "1.12.0"),
)

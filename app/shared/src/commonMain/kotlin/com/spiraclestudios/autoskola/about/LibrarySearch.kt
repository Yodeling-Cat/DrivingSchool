package com.spiraclestudios.autoskola.about

import com.mikepenz.aboutlibraries.entity.Library

/**
 * The libraries a search for [query] is showing: all of them while the query is blank, and
 * otherwise the ones it names. A library is matched on its name and on its Gradle coordinate, so
 * "coil" and "coil3" both find Coil, and case is not part of the query.
 */
fun librariesMatching(libraries: List<Library>, query: String): List<Library> {
    val needle = query.trim()
    if (needle.isEmpty()) return libraries
    return libraries.filter {
        it.name.contains(needle, ignoreCase = true) || it.uniqueId.contains(needle, ignoreCase = true)
    }
}

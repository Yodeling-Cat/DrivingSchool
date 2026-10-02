package com.spiraclestudios.autoskola.about

import kotlin.test.Test
import kotlin.test.assertEquals

class LibrarySearchTest {

    private val all = testLibraries

    @Test
    fun noSearchShowsEveryLibrary() {
        assertEquals(all, librariesMatching(all, ""))
        // Spaces are not a search for anything either.
        assertEquals(all, librariesMatching(all, "   "))
    }

    @Test
    fun aSearchMatchesTheNameOfALibrary() {
        assertEquals(listOf("Compose UI"), librariesMatching(all, "Compose").map { it.name })
    }

    @Test
    fun aSearchIgnoresCase() {
        assertEquals(listOf("Koin"), librariesMatching(all, "KOIN").map { it.name })
    }

    @Test
    fun aSearchAlsoMatchesTheCoordinateOfALibrary() {
        // The name is "coil"; only the coordinate says which pocket of the graph it is.
        assertEquals(listOf("coil"), librariesMatching(all, "coil3").map { it.name })
    }

    @Test
    fun spaceAroundASearchIsNotPartOfIt() {
        assertEquals(listOf("coil"), librariesMatching(all, "  coil  ").map { it.name })
    }

    @Test
    fun aSearchNothingMatchesShowsNothing() {
        assertEquals(emptyList(), librariesMatching(all, "ktor"))
    }
}

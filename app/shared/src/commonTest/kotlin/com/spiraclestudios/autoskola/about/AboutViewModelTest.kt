package com.spiraclestudios.autoskola.about

import com.mikepenz.aboutlibraries.entity.Library
import com.spiraclestudios.autoskola.testing.ViewModelTest
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AboutViewModelTest : ViewModelTest() {

    private fun viewModel(libraries: List<Library> = testLibraries) = AboutViewModel(readLibraries = { libraries })

    /** The screen once the list has been read, which in the app is a blink. */
    private suspend fun StateFlow<AboutUiState>.opened(): AboutUiState = first { !it.loading }

    @Test
    fun theScreenOpensWithEveryLibraryTheAppIsBuiltFrom() = runTest {
        val state = observe(viewModel().uiState).opened()

        assertEquals(listOf("Koin", "coil", "Compose UI"), state.all.map { it.name })
        assertEquals(state.all, state.shown)
        assertFalse(state.nothingFound)
    }

    @Test
    fun aSearchLeavesOnlyTheLibrariesItNames() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel.uiState)
        val opened = state.opened()

        viewModel.onEvent(AboutUiEvent.Search("coil"))

        assertEquals(listOf("coil"), state.first { it.query == "coil" }.shown.map { it.name })
        // What the search left out is still there for the next one.
        assertEquals(3, opened.all.size)
    }

    @Test
    fun aSearchThatMatchesNothingSaysSo() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel.uiState)
        state.opened()

        viewModel.onEvent(AboutUiEvent.Search("zzz"))

        val searched = state.first { it.query == "zzz" }
        assertTrue(searched.shown.isEmpty())
        assertTrue(searched.nothingFound)
    }

    @Test
    fun clearingTheSearchBringsTheWholeListBack() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel.uiState)
        state.opened()

        viewModel.onEvent(AboutUiEvent.Search("zzz"))
        state.first { it.query == "zzz" }
        viewModel.onEvent(AboutUiEvent.Search(""))

        assertEquals(3, state.first { !it.loading && it.query.isEmpty() }.shown.size)
    }

    @Test
    fun aBuildWhoseListCannotBeReadShowsNothingAndIsNotAFailedSearch() = runTest {
        val viewModel = AboutViewModel(readLibraries = { throw IllegalStateException("no list bundled") })
        val state = observe(viewModel.uiState).opened()

        assertTrue(state.all.isEmpty())
        assertTrue(state.shown.isEmpty())
        // Nothing was searched for, so this is a build with no list rather than a search that failed.
        assertFalse(state.nothingFound)
    }
}

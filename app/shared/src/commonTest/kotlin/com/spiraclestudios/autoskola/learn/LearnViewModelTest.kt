package com.spiraclestudios.autoskola.learn

import com.spiraclestudios.autoskola.data.signs.FakeSignCatalogDataSource
import com.spiraclestudios.autoskola.data.signs.SignCatalogRepository
import com.spiraclestudios.autoskola.data.signs.testSignCatalog
import com.spiraclestudios.autoskola.testing.ViewModelTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LearnViewModelTest : ViewModelTest() {

    @Test
    fun anAppThatBundlesARoadSignCatalogOffersTheRoadSigns() = runTest {
        val viewModel = LearnViewModel(SignCatalogRepository(FakeSignCatalogDataSource.of(testSignCatalog())))

        val state = observe(viewModel.uiState)

        assertEquals(LearnUiState.Ready(listOf(LearnSection.RoadSigns, LearnSection.Intersections)), state.value)
    }

    @Test
    fun anAppThatBundlesNoRoadSignCatalogLeavesThemOut() = runTest {
        // The Czech app has no road signs yet, and every country's signs are its own.
        val viewModel = LearnViewModel(SignCatalogRepository(FakeSignCatalogDataSource(emptyList(), emptyList())))

        val state = observe(viewModel.uiState)

        assertEquals(LearnUiState.Ready(listOf(LearnSection.Intersections)), state.value)
    }
}

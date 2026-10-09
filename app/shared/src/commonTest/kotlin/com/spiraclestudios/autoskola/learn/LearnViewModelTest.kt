package com.spiraclestudios.autoskola.learn

import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.data.banks.testBank
import com.spiraclestudios.autoskola.data.preferences.TestSelection
import com.spiraclestudios.autoskola.data.signs.FakeSignCatalogDataSource
import com.spiraclestudios.autoskola.data.signs.SignCatalogRepository
import com.spiraclestudios.autoskola.data.signs.testSignCatalog
import com.spiraclestudios.autoskola.testing.TestRepositories
import com.spiraclestudios.autoskola.testing.ViewModelTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LearnViewModelTest : ViewModelTest() {

    /** The Learn hub of [bankId]'s exam, with only the Slovak sign catalog bundled. */
    private fun viewModel(repositories: TestRepositories, bankId: String): LearnViewModel {
        repositories.preferences.select(TestSelection(bankId, LicenseCategory.B))
        val catalogs = SignCatalogRepository(FakeSignCatalogDataSource.of(testSignCatalog()))
        return LearnViewModel(repositories.exams, catalogs)
    }

    @Test
    fun anExamWhoseCountryHasASignCatalogOffersTheRoadSigns() = runTest {
        val state = observe(viewModel(TestRepositories(), "sk-sk").uiState)

        assertEquals(LearnUiState.Ready(listOf(LearnSection.RoadSigns, LearnSection.Intersections)), state.value)
    }

    @Test
    fun anExamWhoseCountryHasNoSignCatalogLeavesThemOut() = runTest {
        // Every country's signs are its own: a Czech driver is not shown the Slovak ones.
        val czech = TestRepositories(testBank(id = "cz-cs").copy(country = "CZ", language = "cs"))

        val state = observe(viewModel(czech, "cz-cs").uiState)

        assertEquals(LearnUiState.Ready(listOf(LearnSection.Intersections)), state.value)
    }
}

package uno.lux.autoskola.learn

import kotlinx.coroutines.test.runTest
import uno.lux.autoskola.data.signs.FakeSignCatalogueDataSource
import uno.lux.autoskola.data.signs.SignCatalogueRepository
import uno.lux.autoskola.data.signs.testSignCatalogue
import uno.lux.autoskola.signs.SignCatalogue
import uno.lux.autoskola.testing.ViewModelTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SignsViewModelTest : ViewModelTest() {

    private fun viewModel(catalogue: SignCatalogue = testSignCatalogue()) =
        SignsViewModel(SignCatalogueRepository(FakeSignCatalogueDataSource.of(catalogue)))

    @Test
    fun noBundledCatalogueLeavesTheScreenLoading() = runTest {
        val viewModel = SignsViewModel(SignCatalogueRepository(FakeSignCatalogueDataSource(emptyList(), emptyList())))

        val state = observe(viewModel.uiState)

        assertEquals(SignsUiState.Loading, state.value)
    }

    @Test
    fun everySignIsListedWithItsPicture() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel.uiState)

        val ready = assertIs<SignsUiState.Ready>(state.value)
        assertEquals(listOf("140", "142", "601"), ready.signs.map { it.number })
        assertEquals(3, ready.total)
        assertEquals("test://minv-sk/obr3/dz/140.jpg", ready.signs.first().imageUri)
        assertEquals("Chodci", ready.signs.first().name)
    }

    @Test
    fun aSearchKeepsOnlyTheMatchingSigns() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SignsUiEvent.Search("vozovk"))

        val ready = assertIs<SignsUiState.Ready>(state.value)
        assertEquals(listOf("140", "601"), ready.signs.map { it.number })
        assertEquals(3, ready.total)
        assertEquals("vozovk", ready.query)
    }

    @Test
    fun aSearchThatMatchesNothingLeavesTheScreenReady() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SignsUiEvent.Search("vrtuľník"))

        val ready = assertIs<SignsUiState.Ready>(state.value)
        assertTrue(ready.signs.isEmpty())
        assertEquals(3, ready.total)
    }

    @Test
    fun clearingTheSearchBringsEverySignBack() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SignsUiEvent.Search("vozovk"))
        viewModel.onEvent(SignsUiEvent.Search(""))

        assertEquals(3, assertIs<SignsUiState.Ready>(state.value).signs.size)
    }
}

package com.spiraclestudios.autoskola.learn

import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.data.banks.testBank
import com.spiraclestudios.autoskola.data.preferences.TestSelection
import com.spiraclestudios.autoskola.data.signs.FakeSignCatalogDataSource
import com.spiraclestudios.autoskola.data.signs.SignCatalogRepository
import com.spiraclestudios.autoskola.data.signs.testSignCatalog
import com.spiraclestudios.autoskola.data.signs.testSignCatalogInfo
import com.spiraclestudios.autoskola.premium.PremiumStatus
import com.spiraclestudios.autoskola.premium.PurchaseSource
import com.spiraclestudios.autoskola.signs.SignCatalog
import com.spiraclestudios.autoskola.testing.TestRepositories
import com.spiraclestudios.autoskola.testing.ViewModelTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SignsViewModelTest : ViewModelTest() {

    private val repositories = TestRepositories()

    /** The signs of the exam [repositories] has selected, the Slovak group B one unless a test says otherwise. */
    private fun viewModel(
        catalogs: FakeSignCatalogDataSource = FakeSignCatalogDataSource.of(testSignCatalog()),
        repositories: TestRepositories = this.repositories,
        bankId: String = "sk-sk",
    ): SignsViewModel {
        repositories.preferences.select(TestSelection(bankId, LicenseCategory.B))
        return SignsViewModel(SignCatalogRepository(catalogs), repositories.exams, repositories.ads)
    }

    private fun viewModel(catalog: SignCatalog) = viewModel(FakeSignCatalogDataSource.of(catalog))

    @Test
    fun aCountryWithoutACatalogLeavesTheScreenLoading() = runTest {
        // The Learn hub offers no road signs for such an exam, so the screen is only reached by
        // coming back to it after changing the exam.
        val czech = TestRepositories(testBank(id = "cz-cs").copy(country = "CZ", language = "cs"))

        val state = observe(viewModel(repositories = czech, bankId = "cz-cs").uiState)

        assertEquals(SignsUiState.Loading, state.value)
    }

    @Test
    fun theSignsAreTheCatalogOfTheExamsCountry() = runTest {
        val czech = testSignCatalog(id = "cz", country = "CZ")
        val slovak = testSignCatalog()
        val catalogs = FakeSignCatalogDataSource(
            index = listOf(testSignCatalogInfo(czech), testSignCatalogInfo(slovak)),
            catalogs = listOf(czech, slovak),
        )

        val state = observe(viewModel(catalogs).uiState)

        assertIs<SignsUiState.Ready>(state.value)
        assertEquals(listOf("sk-v1"), catalogs.catalogReads)
    }

    @Test
    fun everySignIsListedWithItsPicture() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel.uiState)

        val ready = assertIs<SignsUiState.Ready>(state.value)
        assertEquals(listOf("140", "142", "601"), ready.groups.flatMap { group -> group.signs.map { it.number } })
        assertEquals(3, ready.total)
        assertEquals(3, ready.shown)
        assertEquals("test://minv-sk/obr3/dz/140.jpg", ready.groups.first().signs.first().imageUri)
        assertEquals("Chodci", ready.groups.first().signs.first().name)
    }

    @Test
    fun theSignsAreListedUnderTheHeadingsTheyBelongTo() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel.uiState)

        val ready = assertIs<SignsUiState.Ready>(state.value)

        assertEquals(2, ready.groups.size)
        assertEquals("VÝSTRAŽNÉ ZNAČKY", ready.groups[0].kind)
        assertEquals("I. 4. Pohyb na ceste", ready.groups[0].group)
        assertEquals(listOf("140", "142"), ready.groups[0].signs.map { it.number })
        assertEquals("VODOROVNÉ ZNAČKY", ready.groups[1].kind)
        assertEquals(listOf("601"), ready.groups[1].signs.map { it.number })
    }

    @Test
    fun aSearchKeepsTheHeadingsOfTheSignsItLeaves() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SignsUiEvent.Search("vozovk"))

        val ready = assertIs<SignsUiState.Ready>(state.value)
        assertEquals(listOf("140", "601"), ready.groups.flatMap { group -> group.signs.map { it.number } })
        assertEquals(listOf("I. 4. Pohyb na ceste", "I. Pozdĺžne čiary"), ready.groups.map { it.group })
        assertEquals(3, ready.total)
        assertEquals("vozovk", ready.query)
    }

    @Test
    fun aSearchThatMatchesNothingLeavesTheScreenReady() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SignsUiEvent.Search("vrtuľník"))

        val ready = assertIs<SignsUiState.Ready>(state.value)
        assertTrue(ready.groups.isEmpty())
        assertEquals(0, ready.shown)
        assertEquals(3, ready.total)
    }

    @Test
    fun clearingTheSearchBringsEverySignBack() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SignsUiEvent.Search("vozovk"))
        viewModel.onEvent(SignsUiEvent.Search(""))

        val ready = assertIs<SignsUiState.Ready>(state.value)
        assertEquals(3, ready.shown)
        assertEquals(2, ready.groups.size)
    }

    @Test
    fun everyGroupStartsOpen() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel.uiState)

        val ready = assertIs<SignsUiState.Ready>(state.value)

        assertTrue(ready.allExpanded)
        assertTrue(ready.groups.none { it.folded })
    }

    @Test
    fun foldingTheActionFoldsEveryGroupAndKeepsItsSigns() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SignsUiEvent.ToggleAllGroups)

        val ready = assertIs<SignsUiState.Ready>(state.value)
        assertTrue(ready.groups.all { it.folded })
        assertEquals(false, ready.allExpanded)
        // Folding hides the signs; it is not a search, so the count stays.
        assertEquals(3, ready.shown)
        assertEquals(listOf("140", "142", "601"), ready.groups.flatMap { group -> group.signs.map { it.number } })
    }

    @Test
    fun theActionOpensEverythingAgainWhenAnythingIsFolded() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SignsUiEvent.ToggleGroup("VÝSTRAŽNÉ ZNAČKY", "I. 4. Pohyb na ceste"))
        viewModel.onEvent(SignsUiEvent.ToggleAllGroups)

        val ready = assertIs<SignsUiState.Ready>(state.value)
        assertTrue(ready.allExpanded)
        assertTrue(ready.groups.none { it.folded })
    }

    @Test
    fun aHeadingOpensAndFoldsItsOwnGroup() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SignsUiEvent.ToggleGroup("VÝSTRAŽNÉ ZNAČKY", "I. 4. Pohyb na ceste"))

        val folded = assertIs<SignsUiState.Ready>(state.value)
        assertEquals(listOf(true, false), folded.groups.map { it.folded })
        assertEquals(false, folded.allExpanded)

        viewModel.onEvent(SignsUiEvent.ToggleGroup("VÝSTRAŽNÉ ZNAČKY", "I. 4. Pohyb na ceste"))

        assertTrue(assertIs<SignsUiState.Ready>(state.value).allExpanded)
    }

    @Test
    fun aSearchOpensEveryGroupInItsResults() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SignsUiEvent.ToggleAllGroups)
        viewModel.onEvent(SignsUiEvent.Search("vozovk"))

        val ready = assertIs<SignsUiState.Ready>(state.value)
        assertTrue(ready.allExpanded)
        assertEquals(listOf("140", "601"), ready.groups.flatMap { group -> group.signs.map { it.number } })
    }

    @Test
    fun foldingEverythingFoldsTheGroupsASearchLeft() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SignsUiEvent.Search("vozovk"))
        viewModel.onEvent(SignsUiEvent.ToggleAllGroups)

        val ready = assertIs<SignsUiState.Ready>(state.value)
        assertTrue(ready.groups.all { it.folded })
        assertEquals(2, ready.groups.size)
    }

    @Test
    fun theBannerIsShownUntilTheAdsAreBoughtAway() = runTest {
        val state = observe(viewModel().uiState)

        assertTrue(assertIs<SignsUiState.Ready>(state.value).showAds)

        repositories.premium.update(PremiumStatus.AdsRemoved(PurchaseSource.Play))

        assertFalse(assertIs<SignsUiState.Ready>(state.value).showAds)
    }
}

package uno.lux.autoskola.onboarding

import kotlinx.coroutines.test.runTest
import uno.lux.autoskola.i18n.AppLanguage
import uno.lux.autoskola.testing.TestRepositories
import uno.lux.autoskola.testing.ViewModelTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DisclaimerViewModelTest : ViewModelTest() {

    private val repositories = TestRepositories()
    private val viewModel by lazy { DisclaimerViewModel(repositories.preferences) }

    @Test
    fun languageCanBeChangedBeforeAccepting() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(DisclaimerUiEvent.SelectLanguage(AppLanguage.Czech))

        assertEquals(AppLanguage.Czech, state.value.language)
        assertEquals(AppLanguage.Czech, repositories.preferences.preferences.value.language)
    }

    @Test
    fun acceptingIsRemembered() = runTest {
        viewModel.onEvent(DisclaimerUiEvent.Accept)

        assertTrue(repositories.preferences.preferences.value.disclaimerAccepted)
    }
}

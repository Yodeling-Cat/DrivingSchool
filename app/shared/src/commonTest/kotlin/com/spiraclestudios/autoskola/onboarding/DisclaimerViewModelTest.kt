package com.spiraclestudios.autoskola.onboarding

import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.testing.TestRepositories
import com.spiraclestudios.autoskola.testing.ViewModelTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DisclaimerViewModelTest : ViewModelTest() {

    private val repositories = TestRepositories()
    private val viewModel by lazy { DisclaimerViewModel(repositories.preferences) }

    @Test
    fun languageCanBeChangedBeforeContinuing() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(DisclaimerUiEvent.SelectLanguage(AppLanguage.Czech))

        assertEquals(AppLanguage.Czech, state.value.language)
        assertEquals(AppLanguage.Czech, repositories.preferences.preferences.value.language)
    }

    @Test
    fun readingTheDisclaimerAgreesToNothing() = runTest {
        observe(viewModel.uiState)

        assertEquals(0, repositories.preferences.preferences.value.legalAcceptedVersion)
        assertEquals(emptyList(), repositories.analyticsSink.events)
    }
}

package com.spiraclestudios.autoskola

import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.data.preferences.CURRENT_LEGAL_VERSION
import com.spiraclestudios.autoskola.data.preferences.TestSelection
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.testing.TestRepositories
import com.spiraclestudios.autoskola.testing.ViewModelTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AppViewModelTest : ViewModelTest() {

    private val repositories = TestRepositories()
    private fun viewModel() = AppViewModel(repositories.preferences, repositories.exams)

    @Test
    fun disclaimerComesFirst() = runTest {
        val state = observe(viewModel().uiState)

        assertEquals(AppDestination.Disclaimer, state.value.destination)
    }

    @Test
    fun afterTheDisclaimerTheUserChoosesAnExam() = runTest {
        repositories.preferences.acceptLegal(CURRENT_LEGAL_VERSION)

        assertEquals(AppDestination.ChooseExam, observe(viewModel().uiState).value.destination)
    }

    @Test
    fun selectedExamOpens() = runTest {
        val state = observe(viewModel().uiState)
        repositories.preferences.acceptLegal(CURRENT_LEGAL_VERSION)
        repositories.preferences.select(TestSelection("sk-sk", LicenseCategory.B))

        assertEquals(AppDestination.Exam, state.value.destination)
    }

    @Test
    fun examWhoseBankIsGoneIsChosenAgain() = runTest {
        repositories.preferences.acceptLegal(CURRENT_LEGAL_VERSION)
        repositories.preferences.select(TestSelection("gone", LicenseCategory.B))

        assertEquals(AppDestination.ChooseExam, observe(viewModel().uiState).value.destination)
    }

    @Test
    fun languageFollowsPreferences() = runTest {
        val state = observe(viewModel().uiState)
        repositories.preferences.selectLanguage(AppLanguage.Hungarian)

        assertEquals(AppLanguage.Hungarian, state.value.language)
    }

    @Test
    fun anAcceptedVersionBelowTheCurrentOneAsksAgain() = runTest {
        repositories.preferences.acceptLegal(CURRENT_LEGAL_VERSION - 1)

        assertEquals(AppDestination.Disclaimer, observe(viewModel().uiState).value.destination)
    }
}

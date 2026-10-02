package com.spiraclestudios.autoskola.onboarding

import com.russhwolf.settings.MapSettings
import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.bank.QuestionBankInfo
import com.spiraclestudios.autoskola.data.analytics.AnalyticsEvent
import com.spiraclestudios.autoskola.data.analytics.AnalyticsRepository
import com.spiraclestudios.autoskola.data.analytics.RecordingAnalyticsDataSource
import com.spiraclestudios.autoskola.data.banks.FakeQuestionBankDataSource
import com.spiraclestudios.autoskola.data.banks.QuestionBankRepository
import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository
import com.spiraclestudios.autoskola.data.preferences.TestSelection
import com.spiraclestudios.autoskola.testing.ViewModelTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ExamSelectionViewModelTest : ViewModelTest() {

    private val skSk = QuestionBankInfo(
        id = "sk-sk",
        version = 1,
        country = "SK",
        language = "sk",
        questionCounts = mapOf(LicenseCategory.B to 10, LicenseCategory.C to 5),
    )
    private val skEn = QuestionBankInfo(
        id = "sk-en",
        version = 1,
        country = "SK",
        language = "en",
        questionCounts = mapOf(LicenseCategory.B to 9),
    )
    private val preferences = PreferencesRepository(MapSettings())
    private val banks = QuestionBankRepository(FakeQuestionBankDataSource(listOf(skSk, skEn), emptyList()))
    private val analyticsSink = RecordingAnalyticsDataSource()
    private val analytics = AnalyticsRepository(analyticsSink, preferences)
    private val viewModel by lazy { ExamSelectionViewModel(preferences, banks, analytics) }

    @Test
    fun startsOnTheCountryStep() = runTest {
        val state = observe(viewModel.uiState).value

        assertEquals(ExamSelectionStep.ChooseCountry, state.step)
        assertEquals(
            listOf(CountryOption("SK", available = true), CountryOption("CZ", available = false)),
            state.countryOptions,
        )
    }

    @Test
    fun choosingAnUnavailableCountryDoesNothing() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(ExamSelectionUiEvent.ChooseCountry("CZ"))

        assertEquals(ExamSelectionStep.ChooseCountry, state.value.step)
    }

    @Test
    fun choosingACountryOffersItsLanguages() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(ExamSelectionUiEvent.ChooseCountry("SK"))

        val step = assertIs<ExamSelectionStep.ChooseBankLanguage>(state.value.step)
        assertEquals("SK", step.country)
        assertEquals(listOf(skSk, skEn), step.banks)
    }

    @Test
    fun choosingALanguageOffersItsCategories() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(ExamSelectionUiEvent.ChooseCountry("SK"))
        viewModel.onEvent(ExamSelectionUiEvent.ChooseBank(skSk))

        val step = assertIs<ExamSelectionStep.ChooseCategory>(state.value.step)
        assertEquals(skSk, step.bank)
    }

    @Test
    fun choosingACategorySavesTheSelectionAndFinishes() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(ExamSelectionUiEvent.ChooseCountry("SK"))
        viewModel.onEvent(ExamSelectionUiEvent.ChooseBank(skSk))
        viewModel.onEvent(ExamSelectionUiEvent.ChooseCategory(LicenseCategory.C))

        assertEquals(TestSelection("sk-sk", LicenseCategory.C), preferences.preferences.value.selection)
        assertTrue(state.value.done)
    }

    @Test
    fun choosingACategoryRecordsTheExam() = runTest {
        preferences.setAnalyticsEnabled(true)
        analytics.syncConsent()
        val state = observe(viewModel.uiState)

        viewModel.onEvent(ExamSelectionUiEvent.ChooseCountry("SK"))
        viewModel.onEvent(ExamSelectionUiEvent.ChooseBank(skSk))
        viewModel.onEvent(ExamSelectionUiEvent.ChooseCategory(LicenseCategory.C))

        assertEquals(listOf(AnalyticsEvent.ExamSelected), analyticsSink.events)
        assertEquals(mapOf("bank_id" to "sk-sk", "category" to "C"), analyticsSink.properties.single())
        assertTrue(state.value.done)
    }

    @Test
    fun choosingACategoryRecordsNothingWithoutConsent() = runTest {
        observe(viewModel.uiState)

        viewModel.onEvent(ExamSelectionUiEvent.ChooseCountry("SK"))
        viewModel.onEvent(ExamSelectionUiEvent.ChooseBank(skSk))
        viewModel.onEvent(ExamSelectionUiEvent.ChooseCategory(LicenseCategory.C))

        assertEquals(emptyList(), analyticsSink.events)
    }

    @Test
    fun stepBackRetracesTheWizard() = runTest {
        val state = observe(viewModel.uiState)
        viewModel.onEvent(ExamSelectionUiEvent.ChooseCountry("SK"))
        viewModel.onEvent(ExamSelectionUiEvent.ChooseBank(skSk))

        viewModel.onEvent(ExamSelectionUiEvent.StepBack)
        assertIs<ExamSelectionStep.ChooseBankLanguage>(state.value.step)

        viewModel.onEvent(ExamSelectionUiEvent.StepBack)
        assertEquals(ExamSelectionStep.ChooseCountry, state.value.step)
        assertFalse(state.value.done)
    }
}

package com.spiraclestudios.autoskola.onboarding

import com.russhwolf.settings.MapSettings
import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.bank.QuestionBankInfo
import com.spiraclestudios.autoskola.data.analytics.AnalyticsEvent
import com.spiraclestudios.autoskola.data.analytics.AnalyticsRepository
import com.spiraclestudios.autoskola.data.analytics.RecordingAnalyticsDataSource
import com.spiraclestudios.autoskola.data.banks.FakeQuestionBankDataSource
import com.spiraclestudios.autoskola.data.banks.QuestionBankRepository
import com.spiraclestudios.autoskola.data.preferences.AgeBand
import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository
import com.spiraclestudios.autoskola.data.preferences.TestSelection
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.testing.ViewModelTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    private val czCs = QuestionBankInfo("cz-cs", 1, "CZ", "cs", mapOf(LicenseCategory.B to 8))

    private val preferences = PreferencesRepository(MapSettings()).apply {
        // An adult: the age question decides whether the analytics switch may be answered at all.
        setAgeBand(AgeBand.Adult)
    }
    private val analyticsSink = RecordingAnalyticsDataSource()
    private val analytics = AnalyticsRepository(analyticsSink, preferences)

    /** The wizard of an app that bundles [bundled]: one country's banks, as every app does. */
    private fun viewModelOf(vararg bundled: QuestionBankInfo) = ExamSelectionViewModel(
        preferences,
        QuestionBankRepository(FakeQuestionBankDataSource(bundled.toList(), emptyList())),
        analytics,
    )

    /** The Slovak app: its test in two languages. */
    private val viewModel by lazy { viewModelOf(skSk, skEn) }

    @Test
    fun aTestInSeveralLanguagesOpensOnTheLanguages() = runTest {
        val state = observe(viewModel.uiState)

        assertEquals(ExamSelectionStep.ChooseBankLanguage(listOf(skSk, skEn)), state.value.step)
        assertFalse(state.value.hasPreviousStep)
    }

    @Test
    fun aTestInOneLanguageOpensOnItsCategories() = runTest {
        // The Czech app: its test is in Czech alone, so there is no language to choose.
        val viewModel = viewModelOf(czCs)
        val state = observe(viewModel.uiState)

        assertEquals(ExamSelectionStep.ChooseCategory(czCs), state.value.step)
        assertFalse(state.value.hasPreviousStep)

        // Nothing is behind the first step, so back stays on it.
        viewModel.onEvent(ExamSelectionUiEvent.StepBack)
        assertEquals(ExamSelectionStep.ChooseCategory(czCs), state.value.step)
    }

    @Test
    fun choosingALanguageOffersItsCategoriesAndBackReturnsToTheLanguages() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(ExamSelectionUiEvent.ChooseBank(skSk))
        assertEquals(ExamSelectionStep.ChooseCategory(skSk), state.value.step)
        assertTrue(state.value.hasPreviousStep)

        viewModel.onEvent(ExamSelectionUiEvent.StepBack)
        assertEquals(ExamSelectionStep.ChooseBankLanguage(listOf(skSk, skEn)), state.value.step)
        assertFalse(state.value.done)
    }

    @Test
    fun choosingACategorySavesTheSelectionAndFinishes() = runTest {
        val state = observe(viewModel.uiState)

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

        viewModel.onEvent(ExamSelectionUiEvent.ChooseBank(skSk))
        viewModel.onEvent(ExamSelectionUiEvent.ChooseCategory(LicenseCategory.C))

        assertEquals(listOf(AnalyticsEvent.ExamSelected), analyticsSink.events)
        assertEquals(mapOf("bank_id" to "sk-sk", "category" to "C"), analyticsSink.properties.single())
        assertTrue(state.value.done)
    }

    @Test
    fun choosingACategoryRecordsNothingWithoutConsent() = runTest {
        observe(viewModel.uiState)

        viewModel.onEvent(ExamSelectionUiEvent.ChooseBank(skSk))
        viewModel.onEvent(ExamSelectionUiEvent.ChooseCategory(LicenseCategory.C))

        assertEquals(emptyList(), analyticsSink.events)
    }

    @Test
    fun theProgressCountsTheStepsTheWizardWalks() = runTest {
        val state = observe(viewModel.uiState)
        assertEquals(StepProgress(current = 1, total = 2), state.value.progress)

        viewModel.onEvent(ExamSelectionUiEvent.ChooseBank(skSk))
        assertEquals(StepProgress(current = 2, total = 2), state.value.progress)
    }

    @Test
    fun aWizardOfOneStepHasNoProgressToShow() = runTest {
        val state = observe(viewModelOf(czCs).uiState)

        assertEquals(StepProgress(current = 1, total = 1), state.value.progress)
    }

    @Test
    fun pickingADifferentLanguageRecordsIt() = runTest {
        preferences.setAnalyticsEnabled(true)
        observe(viewModel.uiState)

        viewModel.onEvent(ExamSelectionUiEvent.SelectLanguage(AppLanguage.Hungarian))

        assertEquals(listOf(AnalyticsEvent.LanguageChanged), analyticsSink.events)
        assertEquals(mapOf("to" to "hu"), analyticsSink.properties.single())
    }
}

package com.spiraclestudios.autoskola

import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.data.banks.FakeQuestionBankDataSource
import com.spiraclestudios.autoskola.data.banks.QuestionBankRepository
import com.spiraclestudios.autoskola.data.banks.testBank
import com.spiraclestudios.autoskola.data.banks.testBankInfo
import com.spiraclestudios.autoskola.data.exam.ExamRepository
import com.spiraclestudios.autoskola.data.preferences.CURRENT_LEGAL_VERSION
import com.spiraclestudios.autoskola.data.preferences.TestSelection
import com.spiraclestudios.autoskola.data.premium.PurchaseQuery
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.premium.PremiumStatus
import com.spiraclestudios.autoskola.premium.PurchaseSource
import com.spiraclestudios.autoskola.testing.TestRepositories
import com.spiraclestudios.autoskola.testing.ViewModelTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest : ViewModelTest() {

    private val repositories = TestRepositories()
    private fun viewModel() =
        AppViewModel(repositories.preferences, repositories.exams, repositories.ads, repositories.premiumSync)

    /** The app over two banks, so the exam can be moved from one to another, neither read yet. */
    private val exams = ExamRepository(
        preferences = repositories.preferences,
        banks = QuestionBankRepository(
            FakeQuestionBankDataSource(
                index = listOf(testBankInfo("sk-sk", 1), testBankInfo("sk-en", 1)),
                banks = listOf(testBank("sk-sk"), testBank("sk-en")),
            ),
        ),
        attempts = repositories.attempts,
        analytics = repositories.analytics,
    )

    @Test
    fun disclaimerComesFirst() = runTest {
        val state = observe(viewModel().uiState)

        assertEquals(AppDestination.Disclaimer, state.value.destination)
    }

    // ── Google's consent form comes after the app's own ──

    @Test
    fun theAdsAreNotPreparedWhileTheUserIsStillOnboarding() = runTest {
        val viewModel = viewModel()
        observe(viewModel.uiState)

        // Neither the disclaimer nor the consent screen has been through, so nobody has been told what
        // the app is yet, and Google's form must not be the first thing they are asked.
        assertEquals(emptyList(), repositories.adConsent.gathered)
        viewModel.onEvent(AppUiEvent.DisclaimerRead)
        assertEquals(emptyList(), repositories.adConsent.gathered)
    }

    @Test
    fun theAdsArePreparedOnceTheDocumentsAreAccepted() = runTest {
        val viewModel = viewModel()
        observe(viewModel.uiState)

        repositories.preferences.acceptLegal(CURRENT_LEGAL_VERSION)

        assertEquals(1, repositories.adConsent.gathered.size)
        assertEquals(1, repositories.adDataSource.initializations)
    }

    @Test
    fun aReturningUserHasTheirAdsPreparedAtOnce() = runTest {
        repositories.preferences.acceptLegal(CURRENT_LEGAL_VERSION)

        observe(viewModel().uiState)

        // Every launch asks, because Google's answer can expire and its vendor list changes.
        assertEquals(1, repositories.adConsent.gathered.size)
    }

    @Test
    fun theStoreIsAskedWhatTheUserOwnsAtLaunchAndEveryTimeTheAppComesBack() = runTest {
        val viewModel = viewModel()
        observe(viewModel.uiState)
        assertEquals(1, repositories.billing.queries)

        viewModel.onEvent(AppUiEvent.Resumed)

        assertEquals(2, repositories.billing.queries)
    }

    @Test
    fun aBuyerOfRemoveAdsIsNeverAskedAboutAdsOrHasTheSdkStarted() = runTest {
        repositories.billing.answer = PurchaseQuery.Answered(PremiumStatus.AdsRemoved(PurchaseSource.Play))
        repositories.preferences.acceptLegal(CURRENT_LEGAL_VERSION)

        observe(viewModel().uiState)

        // The entitlement is asked for first, so by the time the ads are prepared the user is known to
        // have paid: no consent form, and the SDK is not started for them.
        assertEquals(PremiumStatus.AdsRemoved(PurchaseSource.Play), repositories.premium.entitlement.value)
        assertEquals(emptyList(), repositories.adConsent.gathered)
        assertEquals(0, repositories.adDataSource.initializations)
        assertFalse(repositories.ads.mayRequestAds())
    }

    @Test
    fun aUserWhoBoughtNothingStillHasTheirAdsPrepared() = runTest {
        repositories.preferences.acceptLegal(CURRENT_LEGAL_VERSION)

        observe(viewModel().uiState)

        assertEquals(1, repositories.adConsent.gathered.size)
        assertTrue(repositories.ads.mayRequestAds())
    }

    @Test
    fun readingTheDisclaimerLeadsToTheConsentScreen() = runTest {
        val viewModel = viewModel()
        observe(viewModel.uiState)

        viewModel.onEvent(AppUiEvent.DisclaimerRead)

        assertEquals(AppDestination.Consent, viewModel.uiState.value.destination)
    }

    /**
     * A rotation recreates the activity and builds this ViewModel again over the same stored
     * preferences. Before the fact that the disclaimer had been read was stored, that read as a first
     * install and sent the user back to the first screen, losing the consent screen they were on.
     */
    @Test
    fun rotatingOnTheConsentScreenDoesNotGoBackToTheDisclaimer() = runTest {
        val viewModel = viewModel()
        observe(viewModel.uiState)
        viewModel.onEvent(AppUiEvent.DisclaimerRead)

        val afterRotation = viewModel()

        assertEquals(AppDestination.Consent, observe(afterRotation.uiState).value.destination)
    }

    @Test
    fun readingTheDisclaimerIsStored() = runTest {
        viewModel().onEvent(AppUiEvent.DisclaimerRead)

        assertEquals(true, repositories.preferences.preferences.value.disclaimerRead)
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
    fun changingTheExamNeverLeavesIt() = runTest {
        repositories.preferences.acceptLegal(CURRENT_LEGAL_VERSION)
        repositories.preferences.select(TestSelection("sk-sk", LicenseCategory.B))
        val viewModel = AppViewModel(repositories.preferences, exams, repositories.ads, repositories.premiumSync)
        val seen = mutableListOf<AppDestination>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { seen += it.destination }
        }
        assertEquals(AppDestination.Exam, viewModel.uiState.value.destination)

        // The other category of the same bank, then a bank nothing has read yet: both re-resolve
        // the exam, and neither is a reason to leave it.
        repositories.preferences.select(TestSelection("sk-sk", LicenseCategory.C))
        repositories.preferences.select(TestSelection("sk-en", LicenseCategory.B))

        // Once the app shows the exam it stays there while the exam it was asked for loads: a
        // destination of Loading takes the screens the choice is made from - settings, and the
        // wizard opened on top of it - down with it, so the wizard starts again from the top.
        // From the first exam on, the exam is the only destination the app is handed.
        assertEquals(listOf(AppDestination.Exam), seen.dropWhile { it != AppDestination.Exam })
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

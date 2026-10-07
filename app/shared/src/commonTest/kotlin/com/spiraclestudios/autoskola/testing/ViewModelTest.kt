package com.spiraclestudios.autoskola.testing

import com.russhwolf.settings.MapSettings
import com.spiraclestudios.autoskola.bank.QuestionBank
import com.spiraclestudios.autoskola.data.ads.AdRepository
import com.spiraclestudios.autoskola.data.ads.FakeAdConsent
import com.spiraclestudios.autoskola.data.ads.FakeAdDataSource
import com.spiraclestudios.autoskola.data.analytics.AnalyticsRepository
import com.spiraclestudios.autoskola.data.analytics.RecordingAnalyticsDataSource
import com.spiraclestudios.autoskola.data.answers.PracticeAnswerRepository
import com.spiraclestudios.autoskola.data.answers.SettingsPracticeAnswerDataSource
import com.spiraclestudios.autoskola.data.attempts.AttemptRepository
import com.spiraclestudios.autoskola.data.attempts.SettingsAttemptDataSource
import com.spiraclestudios.autoskola.data.banks.FakeQuestionBankDataSource
import com.spiraclestudios.autoskola.data.banks.QuestionBankRepository
import com.spiraclestudios.autoskola.data.banks.testBank
import com.spiraclestudios.autoskola.data.exam.CurrentExam
import com.spiraclestudios.autoskola.data.exam.ExamRepository
import com.spiraclestudios.autoskola.data.exam.ExamSession
import com.spiraclestudios.autoskola.data.preferences.AgeBand
import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository
import com.spiraclestudios.autoskola.data.premium.FakeBillingDataSource
import com.spiraclestudios.autoskola.data.premium.PremiumRepository
import com.spiraclestudios.autoskola.data.premium.PremiumSync
import com.spiraclestudios.autoskola.data.review.ReviewRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * Base for ViewModel tests: runs `viewModelScope` on a test dispatcher that executes
 * immediately, so state is up to date as soon as an event returns.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class ViewModelTest {
    @BeforeTest
    fun setMainDispatcher() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMainDispatcher() = Dispatchers.resetMain()

    /** Keeps [state] subscribed for the rest of the test, as a screen would, and returns it. */
    fun <T> TestScope.observe(state: StateFlow<T>): StateFlow<T> {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
        return state
    }
}

/**
 * Real repositories over in-memory storage and [bank].
 *
 * [ageBand] is 16 or older by default, because the age question is asked by the consent screen
 * and a test that is not about it is about a user who has answered it. The three answers are only an
 * adult's to give — or an under-16's whose parent or guardian agreed — so a test that expects anything
 * to be sent or requested needs this answered. Pass `null` for the state nothing has asked yet.
 */
class TestRepositories(bank: QuestionBank = testBank(), ageBand: AgeBand? = AgeBand.Adult) {
    private var clock = 0L
    val settings = MapSettings()
    val preferences = PreferencesRepository(settings).apply { setAgeBand(ageBand) }
    val attempts = AttemptRepository(SettingsAttemptDataSource(settings), now = { ++clock })
    val answers = PracticeAnswerRepository(SettingsPracticeAnswerDataSource(settings))
    val banks = QuestionBankRepository(FakeQuestionBankDataSource.of(bank))

    /** How far the app has got in asking the user to rate it, over the same in-memory storage. */
    val review = ReviewRepository(settings)

    /** What the user owns. Nothing is bought in a test, so this answers Free like the app ships. */
    val premium = PremiumRepository()

    /** The store, answering "nothing bought" until a test says it knows better. */
    val billing = FakeBillingDataSource()
    val premiumSync = PremiumSync(billing, premium, settings)

    /**
     * The ads, over a [FakeAdDataSource] rather than the no-op one. The no-op source answers "this
     * target cannot serve ads", which is a different thing from "the app decided not to ask", and
     * only the second is what these tests are about.
     */
    val adDataSource = FakeAdDataSource()

    /** Google's consent form, answering that ads may be requested: the gate is shut only where a test says so. */
    val adConsent = FakeAdConsent()
    val ads = AdRepository(adDataSource, preferences, premium, settings, adConsent)

    /** What the tests asserted was sent, and the consent gate in front of it. */
    val analyticsSink = RecordingAnalyticsDataSource()
    val analytics = AnalyticsRepository(analyticsSink, preferences)

    val exams = ExamRepository(preferences, banks, attempts, analytics)
}

/** The session of the selected exam, once it has loaded. */
suspend fun ExamRepository.currentSession(): ExamSession = current.filterIsInstance<CurrentExam.Ready>().first().session

package uno.lux.autoskola.testing

import com.russhwolf.settings.MapSettings
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
import uno.lux.autoskola.bank.QuestionBank
import uno.lux.autoskola.data.attempts.AttemptRepository
import uno.lux.autoskola.data.attempts.SettingsAttemptDataSource
import uno.lux.autoskola.data.banks.FakeQuestionBankDataSource
import uno.lux.autoskola.data.banks.QuestionBankRepository
import uno.lux.autoskola.data.banks.testBank
import uno.lux.autoskola.data.exam.CurrentExam
import uno.lux.autoskola.data.exam.ExamRepository
import uno.lux.autoskola.data.exam.ExamSession
import uno.lux.autoskola.data.preferences.PreferencesRepository
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

/** Real repositories over in-memory storage and [bank]. */
class TestRepositories(bank: QuestionBank = testBank()) {
    private var clock = 0L
    val settings = MapSettings()
    val preferences = PreferencesRepository(settings)
    val attempts = AttemptRepository(SettingsAttemptDataSource(settings), now = { ++clock })
    val banks = QuestionBankRepository(FakeQuestionBankDataSource.of(bank))
    val exams = ExamRepository(preferences, banks, attempts)
}

/** The session of the selected exam, once it has loaded. */
suspend fun ExamRepository.currentSession(): ExamSession =
    current.filterIsInstance<CurrentExam.Ready>().first().session

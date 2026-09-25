package uno.lux.autoskola.data.exam

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import uno.lux.autoskola.bank.LicenseCategory
import uno.lux.autoskola.data.attempts.AttemptRepository
import uno.lux.autoskola.data.attempts.SettingsAttemptDataSource
import uno.lux.autoskola.data.banks.FakeQuestionBankDataSource
import uno.lux.autoskola.data.banks.QuestionBankRepository
import uno.lux.autoskola.data.banks.testBank
import uno.lux.autoskola.data.preferences.PreferencesRepository
import uno.lux.autoskola.data.preferences.TestSelection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ExamRepositoryTest {

    private val preferences = PreferencesRepository(MapSettings())
    private val repository = ExamRepository(
        preferences = preferences,
        banks = QuestionBankRepository(FakeQuestionBankDataSource.of(testBank("sk-sk"))),
        attempts = AttemptRepository(SettingsAttemptDataSource(MapSettings())),
    )

    @Test
    fun nothingIsSelectedOnFirstLaunch() = runTest {
        assertEquals(CurrentExam.NotSelected, repository.current.first())
    }

    @Test
    fun selectedExamIsLoaded() = runTest {
        preferences.select(TestSelection("sk-sk", LicenseCategory.C))

        val states = repository.current.take(2).toList()

        assertEquals(CurrentExam.Loading, states.first())
        val ready = assertIs<CurrentExam.Ready>(states.last())
        assertEquals("sk-sk", ready.session.bank.id)
        assertEquals(LicenseCategory.C, ready.session.category)
    }

    @Test
    fun selectedBankThatNoLongerExistsIsMissing() = runTest {
        preferences.select(TestSelection("gone", LicenseCategory.B))

        assertEquals(CurrentExam.Missing, repository.current.take(2).last())
    }
}

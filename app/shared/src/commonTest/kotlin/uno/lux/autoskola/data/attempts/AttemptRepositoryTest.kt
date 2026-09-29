package uno.lux.autoskola.data.attempts

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import uno.lux.autoskola.TestResult
import uno.lux.autoskola.bank.LicenseCategory
import uno.lux.autoskola.bank.Question
import uno.lux.autoskola.stats.AttemptKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AttemptRepositoryTest {

    private var clock = 1_000L
    private val settings = MapSettings()
    private fun repository() = AttemptRepository(SettingsAttemptDataSource(settings), now = { clock++ })

    private val questions = (1..3).map {
        Question(
            id = it,
            section = 1,
            text = "Q$it",
            answers = listOf("a", "b", "c"),
            correct = 0,
            points = 1,
            groups = listOf(LicenseCategory.B),
        )
    }

    private suspend fun AttemptRepository.startTest(checkImmediately: Boolean = false) = start(
        bankId = "sk-sk",
        bankVersion = 1,
        category = LicenseCategory.B,
        kind = AttemptKind.PracticeTest,
        testNumber = 5,
        questions = questions,
        checkImmediately = checkImmediately,
    )

    private suspend fun AttemptRepository.statsB() = statistics("sk-sk", 1, LicenseCategory.B).first()

    @Test
    fun answersAreRecordedAndSurviveRestart() = runTest {
        val repository = repository()
        val attempt = repository.startTest()
        repository.answer(attempt.id, questions[0], chosen = 2)
        repository.answer(attempt.id, questions[0], chosen = 0) // changed their mind
        repository.answer(attempt.id, questions[1], chosen = 1)

        val restored = repository().get(attempt.id)!!
        assertEquals(listOf(1 to 0, 2 to 1), restored.answers.map { it.questionId to it.chosen })
        assertEquals(listOf(true, false), restored.answers.map { it.correct })
        assertEquals(1_000L, restored.startedAt)
        assertTrue(restored.isOpen)
    }

    @Test
    fun unfinishedTestCanBeResumedUntilFinished() = runTest {
        val repository = repository()
        val attempt = repository.startTest()
        repository.answer(attempt.id, questions[0], chosen = 1)

        assertEquals(attempt.id, repository().statsB().resumable?.id)

        repository.finish(attempt.id, TestResult(0, 3))
        val stats = repository().statsB()
        assertNull(stats.resumable)
        assertEquals(mapOf(5 to TestResult(0, 3)), stats.latestResults)
        assertEquals(listOf(1), stats.mistakes)
    }

    @Test
    fun statisticsFollowChanges() = runTest {
        val repository = repository()
        val statistics = repository.statistics("sk-sk", 1, LicenseCategory.B)
        assertNull(statistics.first().resumable)

        val attempt = repository.startTest()
        assertEquals(attempt.id, statistics.first().resumable?.id)

        repository.discard(attempt.id)
        assertNull(statistics.first().resumable)
    }

    @Test
    fun attemptFollowsChanges() = runTest {
        val repository = repository()
        val attempt = repository.startTest()
        val observed = repository.attempt(attempt.id)

        repository.answer(attempt.id, questions[0], chosen = 1)

        assertEquals(1, observed.first()!!.answerFor(1)!!.chosen)
        assertNull(repository.attempt("missing").first())
    }

    @Test
    fun checkedAnswersCannotBeChanged() = runTest {
        val repository = repository()
        val attempt = repository.startTest(checkImmediately = true)
        repository.answer(attempt.id, questions[0], chosen = 1)
        repository.answer(attempt.id, questions[0], chosen = 0)

        assertEquals(1, repository.get(attempt.id)!!.answerFor(1)!!.chosen)
    }

    @Test
    fun statisticsAreScopedToBankVersionAndGroup() = runTest {
        val repository = repository()
        repository.startTest()

        assertNull(repository.statistics("sk-sk", 2, LicenseCategory.B).first().resumable)
        assertNull(repository.statistics("sk-sk", 1, LicenseCategory.C).first().resumable)
    }

    @Test
    fun elapsedSecondsCanBeUpdatedWhileOpen() = runTest {
        val repository = repository()
        val attempt = repository.startTest()
        assertEquals(0L, attempt.elapsedSeconds)

        repository.updateElapsed(attempt.id, 45L)
        assertEquals(45L, repository.get(attempt.id)!!.elapsedSeconds)

        repository.finish(attempt.id, TestResult(0, 3), elapsedSeconds = 60L)
        assertEquals(60L, repository.get(attempt.id)!!.elapsedSeconds)

        // Once closed, updateElapsed does nothing
        repository.updateElapsed(attempt.id, 99L)
        assertEquals(60L, repository.get(attempt.id)!!.elapsedSeconds)
    }
}

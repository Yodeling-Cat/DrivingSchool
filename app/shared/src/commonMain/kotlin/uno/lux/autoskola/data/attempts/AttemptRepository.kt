package uno.lux.autoskola.data.attempts

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import uno.lux.autoskola.TestResult
import uno.lux.autoskola.bank.LicenseCategory
import uno.lux.autoskola.bank.Question
import uno.lux.autoskola.stats.AnswerRecord
import uno.lux.autoskola.stats.AttemptKind
import uno.lux.autoskola.stats.Statistics
import uno.lux.autoskola.stats.TestAttempt
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Records every test attempt with each answer as it's given, so progress survives the app
 * being killed and statistics can be derived later (see [Statistics]). Decides what may
 * change: closed attempts are final, and checked answers are locked.
 */
@OptIn(ExperimentalTime::class)
class AttemptRepository(
    private val dataSource: AttemptDataSource,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    /** Serialises writes, so each read-modify-write sees the previous one. */
    private val writes = Mutex()

    /** Statistics for one bank version and license group, updated whenever an attempt changes. */
    fun statistics(bankId: String, bankVersion: Int, category: LicenseCategory): Flow<Statistics> =
        dataSource.observe(bankId, bankVersion, category).map(Statistics::of)

    /** Attempt [id] as it changes, or `null` if there's no such attempt. */
    fun attempt(id: String): Flow<TestAttempt?> = dataSource.observe(id)

    suspend fun get(id: String): TestAttempt? = attempt(id).first()

    suspend fun start(
        bankId: String,
        bankVersion: Int,
        category: LicenseCategory,
        kind: AttemptKind,
        testNumber: Int?,
        questions: List<Question>,
        checkImmediately: Boolean,
    ): TestAttempt = write {
        val startedAt = now()
        val attempt = TestAttempt(
            id = "$startedAt-${Random.nextInt(0, Int.MAX_VALUE).toString(36)}",
            bankId = bankId,
            bankVersion = bankVersion,
            category = category,
            kind = kind,
            testNumber = testNumber,
            questionIds = questions.map { it.id },
            checkImmediately = checkImmediately,
            startedAt = startedAt,
        )
        dataSource.upsert(attempt)
        attempt
    }

    /** Records (or replaces) the answer to [question]. Ignored once the attempt is closed. */
    suspend fun answer(attemptId: String, question: Question, chosen: Int) = update(attemptId) { attempt ->
        if (!attempt.isOpen) return@update attempt
        if (attempt.checkImmediately && attempt.answerFor(question.id) != null) return@update attempt
        val at = now()
        val record = AnswerRecord(question.id, chosen, correct = chosen == question.correct, answeredAt = at)
        attempt.copy(
            answers = attempt.answers.filter { it.questionId != question.id } + record,
            updatedAt = at,
        )
    }

    suspend fun finish(attemptId: String, result: TestResult) = update(attemptId) { attempt ->
        if (!attempt.isOpen) return@update attempt
        val at = now()
        attempt.copy(finishedAt = at, updatedAt = at, result = result)
    }

    suspend fun discard(attemptId: String) = update(attemptId) { attempt ->
        if (!attempt.isOpen) return@update attempt
        attempt.copy(discardedAt = now())
    }

    private suspend fun update(id: String, change: (TestAttempt) -> TestAttempt) = write {
        val current = get(id) ?: return@write
        val updated = change(current)
        if (updated != current) dataSource.upsert(updated)
    }

    /**
     * Runs [block] after earlier writes, and finishes it even if the caller is cancelled
     * (for example when the user leaves a screen and its ViewModel is cleared).
     */
    private suspend fun <T> write(block: suspend () -> T): T =
        withContext(NonCancellable) { writes.withLock { block() } }
}

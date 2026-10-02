package com.spiraclestudios.autoskola.data.attempts

import com.spiraclestudios.autoskola.TestResult
import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.bank.Question
import com.spiraclestudios.autoskola.stats.AnswerRecord
import com.spiraclestudios.autoskola.stats.AttemptKind
import com.spiraclestudios.autoskola.stats.Statistics
import com.spiraclestudios.autoskola.stats.TestAttempt
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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

    /**
     * Statistics for one bank version and license group, updated whenever an attempt changes.
     * [checkImmediately] is the user's setting: it decides whether the answers of an attempt
     * that is still under way count yet, so it is passed in rather than read here.
     */
    fun statistics(
        bankId: String,
        bankVersion: Int,
        category: LicenseCategory,
        checkImmediately: Boolean,
    ): Flow<Statistics> = dataSource.observe(bankId, bankVersion, category).map { Statistics.of(it, checkImmediately) }

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
            startedAt = startedAt,
        )
        dataSource.upsert(attempt)
        attempt
    }

    /**
     * Records (or replaces) the answer to [question]. Ignored once the attempt is closed, and a
     * [locked] question keeps the answer it already has: an answer that was checked as it was
     * picked is final. Whether answers are checked immediately is the user's setting, so the
     * caller passes it in rather than the attempt remembering it.
     */
    suspend fun answer(
        attemptId: String,
        question: Question,
        chosen: Int,
        locked: Boolean,
    ) = update(attemptId) { attempt ->
        if (!attempt.isOpen) return@update attempt
        if (locked && attempt.answerFor(question.id) != null) return@update attempt
        val at = now()
        val record = AnswerRecord(question.id, chosen, correct = chosen == question.correct, answeredAt = at)
        attempt.copy(
            answers = attempt.answers.filter { it.questionId != question.id } + record,
            updatedAt = at,
        )
    }

    /** Updates the recorded elapsed time for an open attempt. */
    suspend fun updateElapsed(attemptId: String, elapsedSeconds: Long) = update(attemptId) { attempt ->
        if (!attempt.isOpen) return@update attempt
        attempt.copy(elapsedSeconds = elapsedSeconds)
    }

    suspend fun finish(attemptId: String, result: TestResult, elapsedSeconds: Long? = null) =
        update(attemptId) { attempt ->
            if (!attempt.isOpen) return@update attempt
            val at = now()
            attempt.copy(
                finishedAt = at,
                updatedAt = at,
                result = result,
                elapsedSeconds = elapsedSeconds ?: attempt.elapsedSeconds,
            )
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

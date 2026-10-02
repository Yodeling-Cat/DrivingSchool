package com.spiraclestudios.autoskola.data.exam

import com.spiraclestudios.autoskola.TestResult
import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.bank.PracticeTest
import com.spiraclestudios.autoskola.bank.Question
import com.spiraclestudios.autoskola.bank.QuestionBank
import com.spiraclestudios.autoskola.bank.practiceTests
import com.spiraclestudios.autoskola.data.attempts.AttemptRepository
import com.spiraclestudios.autoskola.stats.AttemptKind
import com.spiraclestudios.autoskola.stats.Statistics
import com.spiraclestudios.autoskola.stats.TestAttempt
import com.spiraclestudios.autoskola.stats.score
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** What the home screen needs to know about the user's progress in one exam. */
data class ExamProgress(
    val results: Map<Int, TestResult>,
    /** Numbers of practice tests with an unfinished attempt. */
    val inProgress: Set<Int>,
    val resumable: TestAttempt?,
    val mistakeCount: Int,
    val questionsAnswered: Int,
    val poolSize: Int,
)

/**
 * The user's work on one exam: one bank version and one license group. Holds the decisions
 * the screens act on (what to start or resume, what counts as a mistake, how a test is
 * scored) so they can be unit tested without any UI.
 *
 * Reads are flows that follow changes to the attempts; writes suspend. See AGENTS.md, "Data access".
 */
class ExamSession(val bank: QuestionBank, val category: LicenseCategory, private val attempts: AttemptRepository) {
    val tests: List<PracticeTest> = bank.practiceTests(category)

    private val questionsById = bank.questions.associateBy { it.id }

    /** Ids of the questions that can appear in this exam's tests. */
    val pool: Set<Int> =
        bank.questions.filter { category in it.groups && it.answers.size > 1 }.mapTo(mutableSetOf()) { it.id }

    val statistics: Flow<Statistics> = attempts.statistics(bank.id, bank.version, category)

    /** Questions to retest, grouped by section like a real exam. */
    val mistakes: Flow<List<Question>> = statistics.map { stats ->
        stats.mistakes.filter { it in pool }.mapNotNull(questionsById::get).sortedBy { it.section }
    }

    val progress: Flow<ExamProgress> = statistics.map { stats ->
        ExamProgress(
            results = stats.latestResults,
            inProgress = stats.attempts
                .filter { it.isOpen && it.kind == AttemptKind.PracticeTest }
                .mapNotNullTo(mutableSetOf()) { it.testNumber },
            resumable = stats.resumable,
            mistakeCount = stats.mistakes.count { it in pool },
            questionsAnswered = stats.questions.keys.count { it in pool },
            poolSize = pool.size,
        )
    }

    /** The attempt [id] while it's open and belongs to this exam, otherwise `null`. */
    fun openAttempt(id: String?): Flow<TestAttempt?> = attempt(id).map { it?.takeIf(TestAttempt::isOpen) }

    /** The attempt [id], open or finished, if it belongs to this exam and wasn't discarded. */
    fun attempt(id: String?): Flow<TestAttempt?> = if (id == null) {
        flowOf(null)
    } else {
        attempts.attempt(id).map { attempt ->
            attempt?.takeIf {
                it.discardedAt == null && it.bankId == bank.id && it.bankVersion == bank.version &&
                    it.category == category
            }
        }
    }

    fun questionsOf(attempt: TestAttempt): List<Question> = attempt.questionIds.mapNotNull(questionsById::get)

    /** The attempt for [test] that's still under way, or `null` if the test can simply be started. */
    suspend fun openAttemptFor(test: PracticeTest): TestAttempt? = statistics.first().openAttemptFor(test.number)

    /** Continues [test] if it's already under way, otherwise starts a new attempt. */
    suspend fun startOrResume(test: PracticeTest, checkImmediately: Boolean): TestAttempt = openAttemptFor(test)
        ?: start(AttemptKind.PracticeTest, test.number, test.questions, checkImmediately)

    /** Starts [test] from the beginning, throwing away the attempt that's under way. */
    suspend fun restart(test: PracticeTest, checkImmediately: Boolean): TestAttempt {
        openAttemptFor(test)?.let { attempts.discard(it.id) }
        return start(AttemptKind.PracticeTest, test.number, test.questions, checkImmediately)
    }

    /** Starts a retest of [mistakes], or returns `null` when there's nothing to retest. */
    suspend fun startMistakes(checkImmediately: Boolean): TestAttempt? =
        mistakes.first().takeIf { it.isNotEmpty() }?.let { start(AttemptKind.Mistakes, null, it, checkImmediately) }

    /**
     * Starts a run through [questions] - the situations at intersections, from the learning
     * material - or returns `null` when there's nothing to drill.
     *
     * The drill is not a practice test: it sets no test number, so it can't change a test's result,
     * and the home screen won't offer to continue one that was walked away from. Its answers are
     * answers, though, so what the driver misses here is a mistake to practise like any other.
     */
    suspend fun startSituations(questions: List<Question>, checkImmediately: Boolean): TestAttempt? {
        val drill = questions.filter { it.id in pool }
        return drill.takeIf { it.isNotEmpty() }?.let { start(AttemptKind.Situations, null, it, checkImmediately) }
    }

    /**
     * The run through the situations that is still under way, or `null` when there is none to
     * continue. A driver gets one drill at a time: walking away from one and asking to drill again
     * is offered the same drill back rather than starting a second.
     */
    suspend fun openSituationsAttempt(): TestAttempt? = statistics.first().attempts
        .filter { it.isOpen && it.kind == AttemptKind.Situations }
        .maxByOrNull { it.updatedAt }

    /** Starts a fresh run through [questions], throwing away the drill that's under way. */
    suspend fun restartSituations(questions: List<Question>, checkImmediately: Boolean): TestAttempt? {
        openSituationsAttempt()?.let { attempts.discard(it.id) }
        return startSituations(questions, checkImmediately)
    }

    suspend fun answer(attemptId: String, question: Question, chosen: Int) =
        attempts.answer(attemptId, question, chosen)

    suspend fun updateElapsed(attemptId: String, elapsedSeconds: Long) =
        attempts.updateElapsed(attemptId, elapsedSeconds)

    /** Finishes the attempt, scoring the answers that were recorded. */
    suspend fun finish(attemptId: String, elapsedSeconds: Long? = null) {
        val attempt = openAttempt(attemptId).first() ?: return
        attempts.finish(attemptId, attempt.score(questionsOf(attempt)), elapsedSeconds)
    }

    suspend fun discard(attemptId: String) = attempts.discard(attemptId)

    private suspend fun start(
        kind: AttemptKind,
        testNumber: Int?,
        questions: List<Question>,
        checkImmediately: Boolean,
    ) = attempts.start(
        bankId = bank.id,
        bankVersion = bank.version,
        category = category,
        kind = kind,
        testNumber = testNumber,
        questions = questions,
        checkImmediately = checkImmediately,
    )
}

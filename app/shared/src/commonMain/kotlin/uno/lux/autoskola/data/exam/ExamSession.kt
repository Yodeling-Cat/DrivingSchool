package uno.lux.autoskola.data.exam

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import uno.lux.autoskola.TestResult
import uno.lux.autoskola.bank.LicenseCategory
import uno.lux.autoskola.bank.PracticeTest
import uno.lux.autoskola.bank.Question
import uno.lux.autoskola.bank.QuestionBank
import uno.lux.autoskola.bank.practiceTests
import uno.lux.autoskola.data.attempts.AttemptRepository
import uno.lux.autoskola.stats.AttemptKind
import uno.lux.autoskola.stats.Statistics
import uno.lux.autoskola.stats.TestAttempt
import uno.lux.autoskola.stats.score

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
class ExamSession(
    val bank: QuestionBank,
    val category: LicenseCategory,
    private val attempts: AttemptRepository,
) {
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
    fun attempt(id: String?): Flow<TestAttempt?> =
        if (id == null) flowOf(null) else attempts.attempt(id).map { attempt ->
            attempt?.takeIf {
                it.discardedAt == null && it.bankId == bank.id && it.bankVersion == bank.version &&
                    it.category == category
            }
        }

    fun questionsOf(attempt: TestAttempt): List<Question> = attempt.questionIds.mapNotNull(questionsById::get)

    /** Continues [test] if it's already under way, otherwise starts a new attempt. */
    suspend fun startOrResume(test: PracticeTest, checkImmediately: Boolean): TestAttempt =
        statistics.first().openAttemptFor(test.number) ?: start(AttemptKind.PracticeTest, test.number, test.questions, checkImmediately)

    /** Starts a retest of [mistakes], or returns `null` when there's nothing to retest. */
    suspend fun startMistakes(checkImmediately: Boolean): TestAttempt? =
        mistakes.first().takeIf { it.isNotEmpty() }?.let { start(AttemptKind.Mistakes, null, it, checkImmediately) }

    suspend fun answer(attemptId: String, question: Question, chosen: Int) = attempts.answer(attemptId, question, chosen)

    /** Finishes the attempt, scoring the answers that were recorded. */
    suspend fun finish(attemptId: String) {
        val attempt = openAttempt(attemptId).first() ?: return
        attempts.finish(attemptId, attempt.score(questionsOf(attempt)))
    }

    suspend fun discard(attemptId: String) = attempts.discard(attemptId)

    private suspend fun start(kind: AttemptKind, testNumber: Int?, questions: List<Question>, checkImmediately: Boolean) =
        attempts.start(
            bankId = bank.id,
            bankVersion = bank.version,
            category = category,
            kind = kind,
            testNumber = testNumber,
            questions = questions,
            checkImmediately = checkImmediately,
        )
}

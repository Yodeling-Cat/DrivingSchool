package uno.lux.autoskola.stats

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import uno.lux.autoskola.TestResult
import uno.lux.autoskola.bank.LicenseCategory

/** What kind of test an attempt was. */
@Serializable
enum class AttemptKind {
    /** One of the numbered practice tests; [TestAttempt.testNumber] says which. */
    PracticeTest,

    /** A retest of questions the user previously answered wrongly. */
    Mistakes,

    /** A run through the situations at intersections, from the learning material. */
    Situations,
}

/**
 * One run through a test, from start to finish (or abandonment). Every answer is recorded as
 * it is given, so an unfinished attempt can be resumed and still counts towards statistics.
 *
 * Times are epoch milliseconds. Question ids refer to bank [bankId] at [bankVersion].
 */
@Serializable
data class TestAttempt(
    val id: String,
    val bankId: String,
    val bankVersion: Int,
    val category: LicenseCategory,
    val kind: AttemptKind,
    val testNumber: Int? = null,
    /** Questions in the order they were presented. */
    val questionIds: List<Int>,
    /** Whether answers were checked (and locked) as soon as they were picked. */
    val checkImmediately: Boolean,
    val startedAt: Long,
    /** Last time an answer was given or changed. */
    val updatedAt: Long = startedAt,
    val finishedAt: Long? = null,
    /** Set when the user threw the attempt away instead of finishing it. */
    val discardedAt: Long? = null,
    /** Final answer per question; at most one per question id. */
    val answers: List<AnswerRecord> = emptyList(),
    /** Set once finished. */
    val result: TestResult? = null,
    val elapsedSeconds: Long = 0L,
) {
    val isFinished: Boolean get() = finishedAt != null
    val isOpen: Boolean get() = finishedAt == null && discardedAt == null

    fun answerFor(questionId: Int): AnswerRecord? = answers.firstOrNull { it.questionId == questionId }

    /**
     * Answers that are final: all answers of a finished attempt, and every answer of an
     * attempt in check-immediately mode (those can't be changed once given).
     */
    val finalAnswers: List<AnswerRecord>
        get() = if (isFinished || checkImmediately) answers else emptyList()

    fun encode(): String = AttemptJson.encodeToString(serializer(), this)

    companion object {
        /** Official allotted duration for an exam is 30 minutes (1,800 seconds). */
        const val OFFICIAL_DURATION_SECONDS = 30 * 60L

        fun decode(json: String): TestAttempt = AttemptJson.decodeFromString(serializer(), json)
    }
}

@Serializable
data class AnswerRecord(
    val questionId: Int,
    /** Index into the question's answers. */
    val chosen: Int,
    val correct: Boolean,
    val answeredAt: Long,
)

private val AttemptJson = Json { ignoreUnknownKeys = true }

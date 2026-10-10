package com.spiraclestudios.autoskola.stats

import com.spiraclestudios.autoskola.LenientJson
import com.spiraclestudios.autoskola.TestResult
import com.spiraclestudios.autoskola.bank.LicenseCategory
import kotlinx.serialization.Serializable

/** What kind of test an attempt was. */
@Serializable
enum class AttemptKind {
    /** One of the numbered practice tests; [TestAttempt.testNumber] says which. */
    PracticeTest,

    /** A retest of questions the user previously answered wrongly. */
    Mistakes,

    /** A run through the situations at intersections, from the learning material. */
    Situations,
    ;

    /**
     * Whether an attempt of this kind is given a verdict. A test is passed or failed against the
     * pass mark; a situations drill is a run through the reading material, which is only reported on.
     */
    val hasVerdict: Boolean get() = this != Situations

    /**
     * Whether an attempt of this kind is given the official time. A test is; a situations drill is
     * reading practice with no exam behind it, so its clock only counts how long the driver has been
     * at it.
     */
    val isTimed: Boolean get() = this != Situations
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

    /** [answers] by question id, built the first time one is looked up. */
    private val answersById: Map<Int, AnswerRecord> by lazy {
        buildMap { answers.forEach { if (it.questionId !in this) put(it.questionId, it) } }
    }

    fun answerFor(questionId: Int): AnswerRecord? = answersById[questionId]

    /**
     * Answers that are final, and so count towards statistics: all answers of a finished attempt,
     * and every answer of an open attempt while the user checks answers as they are picked (a
     * checked answer can't be changed once it's given).
     *
     * [checkImmediately] is the user's setting, read as it stands now: an attempt keeps no mode of
     * its own, so a test that is continued goes by what the user has chosen since (see
     * docs/architecture.md, "Statistics").
     */
    fun finalAnswers(checkImmediately: Boolean): List<AnswerRecord> =
        if (isFinished || checkImmediately) answers else emptyList()

    fun encode(): String = LenientJson.encodeToString(serializer(), this)

    companion object {
        /** Official allotted duration for an exam is 30 minutes (1,800 seconds). */
        const val OFFICIAL_DURATION_SECONDS = 30 * 60L

        fun decode(json: String): TestAttempt = LenientJson.decodeFromString(serializer(), json)
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

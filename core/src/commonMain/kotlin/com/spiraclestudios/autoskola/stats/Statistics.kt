package com.spiraclestudios.autoskola.stats

import com.spiraclestudios.autoskola.TestResult

/** How the user has done on one question across all attempts. */
data class QuestionStats(
    val questionId: Int,
    val timesAnswered: Int,
    val timesWrong: Int,
    val lastAnsweredAt: Long,
    /** Whether the most recent final answer was correct. */
    val lastCorrect: Boolean,
)

/**
 * Aggregates for one bank version and license group. Build it from that scope's attempts
 * with [Statistics.of]; every figure is derived, nothing is stored separately.
 */
class Statistics private constructor(val attempts: List<TestAttempt>, val questions: Map<Int, QuestionStats>) {
    /** Latest finished result per practice-test number. */
    val latestResults: Map<Int, TestResult> =
        attempts
            .filter { it.kind == AttemptKind.PracticeTest && it.isFinished && it.testNumber != null }
            .sortedBy { it.finishedAt }
            .associate { it.testNumber!! to it.result!! }

    /**
     * Most recently touched attempt that is neither finished nor discarded, and can be continued:
     * only a numbered practice test. A repeat of the questions the user got wrong is left where it
     * was, so the home screen doesn't offer to continue a drill they walked away from.
     */
    val resumable: TestAttempt? =
        attempts.filter { it.isOpen && it.kind == AttemptKind.PracticeTest }.maxByOrNull { it.updatedAt }

    /** Questions whose most recent final answer was wrong, most recently missed first. */
    val mistakes: List<Int> =
        questions.values.filter { !it.lastCorrect }.sortedByDescending { it.lastAnsweredAt }.map { it.questionId }

    val questionsAnswered: Int get() = questions.size

    fun openAttemptFor(testNumber: Int): TestAttempt? =
        attempts.filter { it.isOpen && it.kind == AttemptKind.PracticeTest && it.testNumber == testNumber }
            .maxByOrNull { it.updatedAt }

    companion object {
        fun of(attempts: List<TestAttempt>): Statistics {
            val byQuestion = attempts
                .flatMap { it.finalAnswers }
                .groupBy { it.questionId }
                .mapValues { (id, answers) ->
                    val last = answers.maxBy { it.answeredAt }
                    QuestionStats(
                        questionId = id,
                        timesAnswered = answers.size,
                        timesWrong = answers.count { !it.correct },
                        lastAnsweredAt = last.answeredAt,
                        lastCorrect = last.correct,
                    )
                }
            return Statistics(attempts, byQuestion)
        }
    }
}

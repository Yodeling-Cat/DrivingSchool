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
     * How many practice tests have been finished, counting every take: a test taken again is
     * another test done, and one finished below the pass mark counts like any other. What the home
     * screen's rating card measures its milestones in.
     */
    val finishedTests: Int get() = attempts.count { it.kind == AttemptKind.PracticeTest && it.isFinished }

    /**
     * How many practice tests have been started, counting every take and every abandoned one. What
     * the ads gate counts its frequency in: the gate is the door into a test, so an attempt left
     * open or thrown away went through that door just the same, and counting only finished tests
     * would let a driver who abandons their work past it for free. Drills are not tests and are not
     * counted, because they are not gated.
     */
    val practiceTestsStarted: Int get() = attempts.count { it.kind == AttemptKind.PracticeTest }

    /**
     * Most recently touched attempt that is neither finished nor discarded, and can be continued:
     * only a numbered practice test. A repeat of the questions the user got wrong is left where it
     * was, so the home screen doesn't offer to continue a drill they walked away from.
     */
    val resumable: TestAttempt? get() = openAttempts(AttemptKind.PracticeTest).firstOrNull()

    /** Numbers of the practice tests with an attempt that is neither finished nor discarded. */
    val inProgressTests: Set<Int>
        get() = openAttempts(AttemptKind.PracticeTest).mapNotNullTo(mutableSetOf()) { it.testNumber }

    /** Questions whose most recent final answer was wrong, most recently missed first. */
    val mistakes: List<Int> =
        questions.values.filter { !it.lastCorrect }.sortedByDescending { it.lastAnsweredAt }.map { it.questionId }

    val questionsAnswered: Int get() = questions.size

    fun openAttemptFor(testNumber: Int): TestAttempt? =
        openAttempts(AttemptKind.PracticeTest).firstOrNull { it.testNumber == testNumber }

    /**
     * The takes of practice test [testNumber] that can be opened again, the most recently started
     * first: every finished one, to be reviewed, and the one under way that [openAttemptFor] would
     * continue. A discarded take is gone, and an older one left open is neither reviewable nor the
     * one to continue, so neither is listed.
     */
    fun history(testNumber: Int): List<TestAttempt> {
        val underWay = openAttemptFor(testNumber)
        return attempts
            .filter { it.kind == AttemptKind.PracticeTest && it.testNumber == testNumber }
            .filter { it.isFinished || it == underWay }
            .sortedByDescending { it.startedAt }
    }

    /** Attempts of [kind] that are neither finished nor discarded, the most recently touched first. */
    fun openAttempts(kind: AttemptKind): List<TestAttempt> =
        attempts.filter { it.isOpen && it.kind == kind }.sortedByDescending { it.updatedAt }

    companion object {
        /**
         * Builds the aggregates for [attempts]. [checkImmediately] is the user's current setting:
         * the answers of an unfinished attempt only count while answers are checked as they are
         * picked, because only then are they final (see [TestAttempt.finalAnswers]).
         */
        fun of(attempts: List<TestAttempt>, checkImmediately: Boolean): Statistics {
            val byQuestion = attempts
                .flatMap { it.finalAnswers(checkImmediately) }
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

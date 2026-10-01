package uno.lux.autoskola.stats

import uno.lux.autoskola.TestResult
import uno.lux.autoskola.bank.Question

/** Recorded answers keyed by the question's position in the attempt (position → answer index). */
fun TestAttempt.answersByIndex(): Map<Int, Int> = questionIds.withIndex()
    .mapNotNull { (index, id) -> answerFor(id)?.let { index to it.chosen } }
    .toMap()

/** Where to reopen the attempt: the first unanswered question, or the last one if all are answered. */
fun TestAttempt.resumeIndex(): Int {
    val answered = answers.mapTo(mutableSetOf()) { it.questionId }
    val firstOpen = questionIds.indexOfFirst { it !in answered }
    return if (firstOpen >= 0) firstOpen else questionIds.lastIndex.coerceAtLeast(0)
}

/** Points for correct answers out of the points of all [questions] in the attempt. */
fun TestAttempt.score(questions: List<Question>): TestResult {
    val asked = questions.filter { it.id in questionIds }
    return TestResult(
        score = asked.sumOf { if (answerFor(it.id)?.correct == true) it.points else 0 },
        maxScore = asked.sumOf { it.points },
    )
}

/** Whether every question of the attempt has an answer. */
val TestAttempt.isFullyAnswered: Boolean
    get() = questionIds.all { answerFor(it) != null }

/** How one attempt went, for the results screen. */
data class AttemptSummary(
    val result: TestResult,
    val correct: Int,
    val wrong: Int,
    val unanswered: Int,
    /** Sections in the order their first question was asked. */
    val sections: List<SectionSummary>,
)

data class SectionSummary(
    val section: Int,
    val correct: Int,
    val total: Int,
    val score: Int,
    val maxScore: Int,
)

/** Counts right, wrong and missing answers to [questions], overall and per section. */
fun TestAttempt.summary(questions: List<Question>): AttemptSummary {
    val asked = questions.filter { it.id in questionIds }
    fun isCorrect(question: Question) = answerFor(question.id)?.correct == true
    val answered = asked.count { answerFor(it.id) != null }
    val correct = asked.count(::isCorrect)
    return AttemptSummary(
        result = score(questions),
        correct = correct,
        wrong = answered - correct,
        unanswered = asked.size - answered,
        sections = asked.groupBy { it.section }.map { (section, inSection) ->
            SectionSummary(
                section = section,
                correct = inSection.count(::isCorrect),
                total = inSection.size,
                score = inSection.sumOf { if (isCorrect(it)) it.points else 0 },
                maxScore = inSection.sumOf { it.points },
            )
        },
    )
}

/**
 * The whole minutes [totalSeconds] is, the way the result card reads them: the seconds are dropped,
 * so a test 23:59 long reads as 23 minutes. A count a test never gets to (a negative one) reads as
 * no time at all rather than as a minus.
 */
fun elapsedMinutes(totalSeconds: Long): Int = (totalSeconds.coerceAtLeast(0) / 60).toInt()

/** Formats [totalSeconds] as mm:ss (or h:mm:ss when an hour or more). */
fun formatElapsedTime(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    } else {
        "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    }
}

package uno.lux.autoskola.stats

import uno.lux.autoskola.TestResult
import uno.lux.autoskola.bank.Question

/** Recorded answers keyed by the question's position in the attempt (position → answer index). */
fun TestAttempt.answersByIndex(): Map<Int, Int> =
    questionIds.withIndex()
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

package uno.lux.autoskola.stats

import uno.lux.autoskola.TestResult
import uno.lux.autoskola.bank.LicenseCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StatisticsTest {

    private fun attempt(
        id: String,
        testNumber: Int? = 1,
        startedAt: Long = 0,
        checkImmediately: Boolean = false,
        finishedAt: Long? = null,
        discardedAt: Long? = null,
        vararg answers: AnswerRecord,
    ) = TestAttempt(
        id = id,
        bankId = "sk-sk",
        bankVersion = 1,
        category = LicenseCategory.B,
        kind = if (testNumber == null) AttemptKind.Mistakes else AttemptKind.PracticeTest,
        testNumber = testNumber,
        questionIds = listOf(1, 2, 3),
        checkImmediately = checkImmediately,
        startedAt = startedAt,
        updatedAt = answers.maxOfOrNull { it.answeredAt } ?: startedAt,
        finishedAt = finishedAt,
        discardedAt = discardedAt,
        answers = answers.toList(),
        result = finishedAt?.let { TestResult(answers.count { it.correct }, 3) },
    )

    private fun answer(question: Int, correct: Boolean, at: Long) =
        AnswerRecord(questionId = question, chosen = if (correct) 0 else 1, correct = correct, answeredAt = at)

    @Test
    fun mistakesUseTheMostRecentFinalAnswer() {
        val stats = Statistics.of(
            listOf(
                attempt("a", finishedAt = 10, answers = arrayOf(answer(1, false, 5), answer(2, false, 6))),
                // Question 1 was later answered correctly, so it's no longer a mistake.
                attempt("b", testNumber = null, finishedAt = 20, answers = arrayOf(answer(1, true, 15))),
            )
        )

        assertEquals(listOf(2), stats.mistakes)
        assertEquals(2, stats.questions.getValue(1).timesAnswered)
        assertEquals(1, stats.questions.getValue(1).timesWrong)
    }

    @Test
    fun unfinishedAnswersOnlyCountWhenLocked() {
        val stats = Statistics.of(
            listOf(
                // Editable answers in an unfinished attempt may still change: ignored.
                attempt("editable", answers = arrayOf(answer(1, false, 5))),
                // Checked-immediately answers are final even before the test is finished.
                attempt("locked", checkImmediately = true, answers = arrayOf(answer(2, false, 6))),
            )
        )

        assertEquals(listOf(2), stats.mistakes)
    }

    @Test
    fun resumableIsTheLatestOpenAttempt() {
        val stats = Statistics.of(
            listOf(
                attempt("old", startedAt = 1),
                attempt("newer", testNumber = 2, startedAt = 2, answers = arrayOf(answer(1, true, 9))),
                attempt("discarded", testNumber = 3, startedAt = 10, discardedAt = 11),
                attempt("done", testNumber = 4, startedAt = 12, finishedAt = 13),
            )
        )

        assertEquals("newer", stats.resumable?.id)
        assertEquals("old", stats.openAttemptFor(1)?.id)
        assertNull(stats.openAttemptFor(3))
    }

    @Test
    fun latestResultPerTestWins() {
        val stats = Statistics.of(
            listOf(
                attempt("first", finishedAt = 10, answers = arrayOf(answer(1, true, 5))),
                attempt("second", finishedAt = 20, answers = arrayOf(answer(1, false, 15))),
            )
        )

        assertEquals(mapOf(1 to TestResult(0, 3)), stats.latestResults)
    }

    @Test
    fun attemptsSurviveEncoding() {
        val original = attempt("x", finishedAt = 10, answers = arrayOf(answer(1, true, 5)))
        assertEquals(original, TestAttempt.decode(original.encode()))
    }
}

package com.spiraclestudios.autoskola.stats

import com.spiraclestudios.autoskola.TestResult
import com.spiraclestudios.autoskola.bank.LicenseCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StatisticsTest {

    private fun attempt(
        id: String,
        testNumber: Int? = 1,
        kind: AttemptKind? = null,
        startedAt: Long = 0,
        finishedAt: Long? = null,
        discardedAt: Long? = null,
        vararg answers: AnswerRecord,
    ) = TestAttempt(
        id = id,
        bankId = "sk-sk",
        bankVersion = 1,
        category = LicenseCategory.B,
        kind = kind ?: if (testNumber == null) AttemptKind.Mistakes else AttemptKind.PracticeTest,
        testNumber = testNumber,
        questionIds = listOf(1, 2, 3),
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
    fun aDrillIsNeitherAResumableTestNorATestResult() {
        // A run through the situations is practice, not a test: it has no number to score, and a
        // driver who walks away from one isn't offered it back. Its answers still count, so what
        // they missed is a mistake like any other.
        val stats = Statistics.of(
            listOf(
                attempt(
                    "drill",
                    testNumber = null,
                    kind = AttemptKind.Situations,
                    finishedAt = 10,
                    answers = arrayOf(answer(1, false, 5)),
                ),
                attempt("walked-away", testNumber = null, kind = AttemptKind.Situations),
            ),
            checkImmediately = false,
        )

        assertNull(stats.resumable)
        assertEquals(emptyMap(), stats.latestResults)
        assertEquals(listOf(1), stats.mistakes)
    }

    @Test
    fun mistakesUseTheMostRecentFinalAnswer() {
        val stats = Statistics.of(
            listOf(
                attempt("a", finishedAt = 10, answers = arrayOf(answer(1, false, 5), answer(2, false, 6))),
                // Question 1 was later answered correctly, so it's no longer a mistake.
                attempt("b", testNumber = null, finishedAt = 20, answers = arrayOf(answer(1, true, 15))),
            ),
            checkImmediately = false,
        )

        assertEquals(listOf(2), stats.mistakes)
        assertEquals(2, stats.questions.getValue(1).timesAnswered)
        assertEquals(1, stats.questions.getValue(1).timesWrong)
    }

    @Test
    fun unfinishedAnswersOnlyCountWhileAnswersAreCheckedAsTheyArePicked() {
        val answers = arrayOf(answer(1, false, 5))
        val openAttempt = attempt("open", answers = answers)

        // Checked as they are picked, an answer is locked the moment it's given, so it is final
        // even before the test is finished.
        assertEquals(listOf(1), Statistics.of(listOf(openAttempt), checkImmediately = true).mistakes)

        // With that off, the answer is still the user's to change, so it counts once the test is
        // finished and not before. Which of the two applies is the user's setting as it stands now,
        // not something the attempt remembers from when it was started.
        assertEquals(emptyList(), Statistics.of(listOf(openAttempt), checkImmediately = false).mistakes)
        val finished = attempt("finished", finishedAt = 9, answers = answers)
        assertEquals(listOf(1), Statistics.of(listOf(finished), checkImmediately = false).mistakes)
    }

    @Test
    fun resumableIsTheLatestOpenAttempt() {
        val stats = Statistics.of(
            listOf(
                attempt("old", startedAt = 1),
                attempt("newer", testNumber = 2, startedAt = 2, answers = arrayOf(answer(1, true, 9))),
                attempt("discarded", testNumber = 3, startedAt = 10, discardedAt = 11),
                attempt("done", testNumber = 4, startedAt = 12, finishedAt = 13),
            ),
            checkImmediately = false,
        )

        assertEquals("newer", stats.resumable?.id)
        assertEquals("old", stats.openAttemptFor(1)?.id)
        assertNull(stats.openAttemptFor(3))
    }

    @Test
    fun openAttemptsOfAKindAreTheOpenOnesMostRecentFirst() {
        val stats = Statistics.of(
            listOf(
                attempt("old-drill", testNumber = null, kind = AttemptKind.Situations, startedAt = 1),
                attempt("new-drill", testNumber = null, kind = AttemptKind.Situations, startedAt = 5),
                attempt("done-drill", testNumber = null, kind = AttemptKind.Situations, finishedAt = 9),
                attempt("test-1", testNumber = 1, startedAt = 3),
                attempt("test-2", testNumber = 2, startedAt = 4, discardedAt = 6),
                attempt("test-4", testNumber = 4, startedAt = 2),
            ),
            checkImmediately = false,
        )

        assertEquals(listOf("new-drill", "old-drill"), stats.openAttempts(AttemptKind.Situations).map { it.id })
        assertEquals(setOf(1, 4), stats.inProgressTests)
    }

    @Test
    fun aMistakesTestIsNotOfferedToContinue() {
        val stats = Statistics.of(
            listOf(
                attempt("test", startedAt = 1),
                attempt("mistakes", testNumber = null, startedAt = 2, answers = arrayOf(answer(1, false, 9))),
            ),
            checkImmediately = false,
        )

        assertEquals("test", stats.resumable?.id)
    }

    @Test
    fun onlyAMistakesTestLeftOpenLeavesNothingToContinue() {
        val stats = Statistics.of(
            listOf(attempt("mistakes", testNumber = null, startedAt = 1)),
            checkImmediately = false,
        )

        assertNull(stats.resumable)
    }

    @Test
    fun latestResultPerTestWins() {
        val stats = Statistics.of(
            listOf(
                attempt("first", finishedAt = 10, answers = arrayOf(answer(1, true, 5))),
                attempt("second", finishedAt = 20, answers = arrayOf(answer(1, false, 15))),
            ),
            checkImmediately = false,
        )

        assertEquals(mapOf(1 to TestResult(0, 3)), stats.latestResults)
    }

    @Test
    fun everyFinishedTestCountsTowardsTheOnesDone() {
        val stats = Statistics.of(
            listOf(
                attempt("first", finishedAt = 10),
                // The same test taken again is another test done.
                attempt("again", finishedAt = 20, answers = arrayOf(answer(1, false, 15))),
                // A drill is practice, not a test, and neither is an attempt left open or thrown away.
                attempt("drill", testNumber = null, kind = AttemptKind.Situations, finishedAt = 30),
                attempt("open", startedAt = 35),
                attempt("thrown-away", testNumber = 2, discardedAt = 40),
            ),
            checkImmediately = false,
        )

        assertEquals(2, stats.finishedTests)
    }

    @Test
    fun everyStartedTestCountsTowardsTheOnesStarted() {
        // What the ads gate counts. It has to be tests *started*, because the gate is the door into
        // a test: an attempt left open or thrown away went through it just the same, and counting
        // only finished tests would let a driver who abandons their work through the door for free.
        val stats = Statistics.of(
            listOf(
                attempt("first", finishedAt = 10),
                attempt("again", finishedAt = 20),
                attempt("open", startedAt = 35),
                attempt("thrown-away", testNumber = 2, discardedAt = 40),
                // A drill and a mistakes retest are practice rather than tests, and neither is
                // gated, so neither may push a user towards the next one.
                attempt("drill", testNumber = null, kind = AttemptKind.Situations, finishedAt = 30),
                attempt("mistakes", testNumber = null, finishedAt = 31),
            ),
            checkImmediately = false,
        )

        assertEquals(4, stats.practiceTestsStarted)
        assertEquals(2, stats.finishedTests)
    }

    @Test
    fun attemptsSurviveEncoding() {
        val original = attempt("x", finishedAt = 10, answers = arrayOf(answer(1, true, 5)))
        assertEquals(original, TestAttempt.decode(original.encode()))
    }
}

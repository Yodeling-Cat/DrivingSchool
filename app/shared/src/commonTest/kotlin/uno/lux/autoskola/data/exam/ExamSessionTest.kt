package uno.lux.autoskola.data.exam

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import uno.lux.autoskola.TestResult
import uno.lux.autoskola.data.attempts.AttemptRepository
import uno.lux.autoskola.data.attempts.SettingsAttemptDataSource
import uno.lux.autoskola.bank.LicenseCategory
import uno.lux.autoskola.bank.Question
import uno.lux.autoskola.data.banks.testBank
import uno.lux.autoskola.stats.AttemptKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExamSessionTest {

    private var clock = 0L
    private val attempts = AttemptRepository(SettingsAttemptDataSource(MapSettings()), now = { ++clock })

    private val bank = testBank()

    private fun session(category: LicenseCategory = LicenseCategory.B) = ExamSession(bank, category, attempts)

    /** Answers every question of the attempt, wrongly for [wrong] question ids. */
    private suspend fun ExamSession.answerAll(attemptId: String, wrong: Set<Int> = emptySet()) {
        questionsOf(attempts.get(attemptId)!!).forEach { answer(attemptId, it, if (it.id in wrong) 1 else 0) }
    }

    @Test
    fun poolOnlyContainsQuestionsForTheGroup() {
        assertEquals(setOf(1, 2, 3, 4), session().pool)
        assertEquals(setOf(1, 2, 3, 4, 5), session(LicenseCategory.C).pool)
    }

    @Test
    fun startingATestRecordsItsQuestionsAndMode() = runTest {
        val session = session()
        val test = session.tests.first()

        val attempt = session.startOrResume(test, checkImmediately = true)

        assertEquals(AttemptKind.PracticeTest, attempt.kind)
        assertEquals(test.number, attempt.testNumber)
        assertEquals(test.questions.map { it.id }, attempt.questionIds)
        assertTrue(attempt.checkImmediately)
    }

    @Test
    fun startingATestThatIsUnderWayResumesIt() = runTest {
        val session = session()
        val test = session.tests.first()
        val first = session.startOrResume(test, checkImmediately = false)

        val again = session.startOrResume(test, checkImmediately = true)

        assertEquals(first.id, again.id)
        assertEquals(false, again.checkImmediately) // the mode is fixed when the attempt starts
    }

    @Test
    fun finishingScoresTheRecordedAnswers() = runTest {
        val session = session()
        val test = session.tests.first()
        val attempt = session.startOrResume(test, checkImmediately = false)
        val wrongQuestion = test.questions.first()
        session.answerAll(attempt.id, wrong = setOf(wrongQuestion.id))

        session.finish(attempt.id)

        val maxScore = test.questions.sumOf { it.points }
        assertEquals(TestResult(maxScore - wrongQuestion.points, maxScore), session.progress.first().results[test.number])
        assertNull(session.progress.first().resumable)
    }

    @Test
    fun startingAfterFinishingCreatesANewAttempt() = runTest {
        val session = session()
        val test = session.tests.first()
        val first = session.startOrResume(test, checkImmediately = false)
        session.finish(first.id)

        assertNotEquals(first.id, session.startOrResume(test, checkImmediately = false).id)
    }

    @Test
    fun mistakesRetestOnlyWrongQuestionsOfTheGroupInSectionOrder() = runTest {
        val c = session(LicenseCategory.C)
        // Get questions 4 and 5 (5 is C only) wrong in group C. Each test has only one
        // section-2 question, so go through all of them.
        c.tests.forEach { test ->
            val cAttempt = c.startOrResume(test, checkImmediately = false)
            c.answerAll(cAttempt.id, wrong = setOf(4, 5))
            c.finish(cAttempt.id)
        }
        // And question 1 wrong in a group B test.
        val b = session()
        val bAttempt = b.startOrResume(b.tests.first { t -> t.questions.any { it.id == 1 } }, checkImmediately = false)
        b.answerAll(bAttempt.id, wrong = setOf(1))
        b.finish(bAttempt.id)

        // Stats are per group, so group B only sees its own mistake.
        assertEquals(listOf(1), b.mistakes.first().map { it.id })
        val retest = assertNotNull(b.startMistakes(checkImmediately = true))
        assertEquals(AttemptKind.Mistakes, retest.kind)
        assertEquals(listOf(1), retest.questionIds)

        val cMistakes = c.mistakes.first().map { it.id }
        assertTrue(cMistakes.containsAll(listOf(4, 5)))
        assertEquals(cMistakes.sortedBy { id -> bank.questions.first { it.id == id }.section }, cMistakes)
    }

    @Test
    fun noMistakesMeansNoRetest() = runTest {
        assertNull(session().startMistakes(checkImmediately = false))
    }

    @Test
    fun progressSummarisesOpenAttemptsAndAnsweredQuestions() = runTest {
        val session = session()
        val test = session.tests.first()
        val attempt = session.startOrResume(test, checkImmediately = true)
        session.answer(attempt.id, test.questions.first(), 0)

        val progress = session.progress.first()

        assertEquals(setOf(test.number), progress.inProgress)
        assertEquals(attempt.id, progress.resumable?.id)
        assertEquals(1, progress.questionsAnswered) // locked answer counts
        assertEquals(4, progress.poolSize)
    }

    @Test
    fun openAttemptIgnoresClosedAttemptsAndOtherBanks() = runTest {
        val session = session()
        val attempt = session.startOrResume(session.tests.first(), checkImmediately = false)
        assertEquals(attempt.id, session.openAttempt(attempt.id).first()?.id)

        attempts.discard(attempt.id)
        assertNull(session.openAttempt(attempt.id).first())

        val otherVersion = ExamSession(bank.copy(version = 2), LicenseCategory.B, attempts)
        val v2Attempt = otherVersion.startOrResume(otherVersion.tests.first(), checkImmediately = false)
        assertNull(session.openAttempt(v2Attempt.id).first())
        assertNull(session.openAttempt(null).first())
    }

    @Test
    fun attemptIncludesFinishedButNotDiscardedAttempts() = runTest {
        val session = session()
        val finished = session.startOrResume(session.tests.first(), checkImmediately = false)
        session.finish(finished.id)
        val discarded = session.startOrResume(session.tests.first(), checkImmediately = false)
        attempts.discard(discarded.id)

        assertEquals(finished.id, session.attempt(finished.id).first()?.id)
        assertNull(session.attempt(discarded.id).first())
        assertNull(session.openAttempt(finished.id).first())

        val otherGroup = session(LicenseCategory.C)
        assertNull(otherGroup.attempt(finished.id).first())
    }

    @Test
    fun questionsOfAnAttemptKeepTheirOrder() = runTest {
        val session = session()
        val attempt = session.startOrResume(session.tests.first(), checkImmediately = false)
        assertEquals(attempt.questionIds, session.questionsOf(attempt).map { it.id })
    }
}

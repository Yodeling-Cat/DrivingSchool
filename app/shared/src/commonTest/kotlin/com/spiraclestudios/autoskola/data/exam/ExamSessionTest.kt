package com.spiraclestudios.autoskola.data.exam

import com.russhwolf.settings.MapSettings
import com.spiraclestudios.autoskola.TestResult
import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.bank.Question
import com.spiraclestudios.autoskola.data.attempts.AttemptRepository
import com.spiraclestudios.autoskola.data.attempts.SettingsAttemptDataSource
import com.spiraclestudios.autoskola.data.banks.testBank
import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository
import com.spiraclestudios.autoskola.stats.AttemptKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExamSessionTest {

    private var clock = 0L
    private val attempts = AttemptRepository(SettingsAttemptDataSource(MapSettings()), now = { ++clock })
    private val preferences = PreferencesRepository(MapSettings())

    private val bank = testBank()

    private fun session(category: LicenseCategory = LicenseCategory.B) =
        ExamSession(bank, category, attempts, preferences)

    /** Answers every question of the attempt, wrongly for [wrong] question ids. */
    private suspend fun ExamSession.answerAll(attemptId: String, wrong: Set<Int> = emptySet()) {
        questionsOf(attempts.get(attemptId)!!).forEach { answer(attemptId, it, if (it.id in wrong) 1 else 0) }
    }

    @Test
    fun aSituationsDrillRecordsTheQuestionsItRunsAndMarksItselfAsOne() = runTest {
        val session = session()
        val questions = bank.questions.filter { it.id in listOf(1, 2) }

        val drill = session.startSituations(questions)

        val started = assertNotNull(drill)
        assertEquals(AttemptKind.Situations, started.kind)
        // No test number: a drill is not one of the numbered tests, so it can't score one.
        assertNull(started.testNumber)
        assertEquals(listOf(1, 2), started.questionIds)
    }

    @Test
    fun aDrillLeavesOutQuestionsTheExamMayNotAsk() = runTest {
        // Question 5 is asked of group C alone, so a B exam has nothing to drill in it.
        val session = session(LicenseCategory.B)
        val questionOfAnotherGroup = bank.questions.filter { it.id == 5 }

        assertNull(session.startSituations(questionOfAnotherGroup))
    }

    @Test
    fun aDrillWithNoQuestionsStartsNothing() = runTest {
        assertNull(session().startSituations(emptyList()))
    }

    @Test
    fun poolOnlyContainsQuestionsForTheGroup() {
        assertEquals(setOf(1, 2, 3, 4), session().pool)
        assertEquals(setOf(1, 2, 3, 4, 5), session(LicenseCategory.C).pool)
    }

    @Test
    fun startingATestRecordsItsQuestions() = runTest {
        val session = session()
        val test = session.tests.first()

        val attempt = session.startOrResume(test)

        assertEquals(AttemptKind.PracticeTest, attempt.kind)
        assertEquals(test.number, attempt.testNumber)
        assertEquals(test.questions.map { it.id }, attempt.questionIds)
    }

    @Test
    fun resumingATestKeepsItAndGoesByTheSettingTheUserHasNow() = runTest {
        // An attempt remembers nothing about the settings it was started under: continuing it goes
        // by what the user has chosen since.
        val session = session()
        val test = session.tests.first()
        val first = session.startOrResume(test)
        val question = test.questions.first()

        preferences.setCheckAnswersImmediately(false)
        assertEquals(first.id, session.startOrResume(test).id)
        session.answer(first.id, question, chosen = 0)
        session.answer(first.id, question, chosen = 1)
        assertEquals(1, attempts.get(first.id)!!.answerFor(question.id)!!.chosen)

        // Checking answers as they are picked again locks what is there: the pick can't be taken back.
        preferences.setCheckAnswersImmediately(true)
        session.answer(first.id, question, chosen = 2)
        assertEquals(1, attempts.get(first.id)!!.answerFor(question.id)!!.chosen)
    }

    @Test
    fun restartingThrowsAwayTheAttemptUnderWayAndStartsAFreshOne() = runTest {
        val session = session()
        val test = session.tests.first()
        val started = session.startOrResume(test)
        session.answer(started.id, test.questions.first(), 0)

        val restarted = session.restart(test)

        assertNotEquals(started.id, restarted.id)
        assertEquals(test.questions.map { it.id }, restarted.questionIds)
        assertTrue(restarted.answers.isEmpty())
        assertNotNull(attempts.get(started.id)!!.discardedAt)
        assertEquals(restarted.id, session.openAttemptFor(test)?.id)
    }

    @Test
    fun restartingATestThatWasNeverStartedJustStartsIt() = runTest {
        val session = session()
        val test = session.tests.first()

        val restarted = session.restart(test)

        assertEquals(test.number, restarted.testNumber)
        assertEquals(restarted.id, session.openAttemptFor(test)?.id)
    }

    @Test
    fun aTestThatWasNeverStartedHasNoAttemptUnderWay() = runTest {
        val session = session()
        assertNull(session.openAttemptFor(session.tests.first()))
    }

    @Test
    fun finishingScoresTheRecordedAnswers() = runTest {
        val session = session()
        val test = session.tests.first()
        val attempt = session.startOrResume(test)
        val wrongQuestion = test.questions.first()
        session.answerAll(attempt.id, wrong = setOf(wrongQuestion.id))

        session.finish(attempt.id)

        val maxScore = test.questions.sumOf { it.points }
        assertEquals(
            TestResult(maxScore - wrongQuestion.points, maxScore),
            session.progress.first().results[test.number],
        )
        assertNull(session.progress.first().resumable)
    }

    @Test
    fun startingAfterFinishingCreatesANewAttempt() = runTest {
        val session = session()
        val test = session.tests.first()
        val first = session.startOrResume(test)
        session.finish(first.id)

        assertNotEquals(first.id, session.startOrResume(test).id)
    }

    @Test
    fun mistakesRetestOnlyWrongQuestionsOfTheGroupInSectionOrder() = runTest {
        val c = session(LicenseCategory.C)
        // Get questions 4 and 5 (5 is C only) wrong in group C. Each test has only one
        // section-2 question, so go through all of them.
        c.tests.forEach { test ->
            val cAttempt = c.startOrResume(test)
            c.answerAll(cAttempt.id, wrong = setOf(4, 5))
            c.finish(cAttempt.id)
        }
        // And question 1 wrong in a group B test.
        val b = session()
        val bAttempt = b.startOrResume(b.tests.first { t -> t.questions.any { it.id == 1 } })
        b.answerAll(bAttempt.id, wrong = setOf(1))
        b.finish(bAttempt.id)

        // Stats are per group, so group B only sees its own mistake.
        assertEquals(listOf(1), b.mistakes.first().map { it.id })
        val retest = assertNotNull(b.startMistakes())
        assertEquals(AttemptKind.Mistakes, retest.kind)
        assertEquals(listOf(1), retest.questionIds)

        val cMistakes = c.mistakes.first().map { it.id }
        assertTrue(cMistakes.containsAll(listOf(4, 5)))
        assertEquals(cMistakes.sortedBy { id -> bank.questions.first { it.id == id }.section }, cMistakes)
    }

    @Test
    fun noMistakesMeansNoRetest() = runTest {
        assertNull(session().startMistakes())
    }

    @Test
    fun progressSummarisesOpenAttemptsAndAnsweredQuestions() = runTest {
        val session = session()
        val test = session.tests.first()
        preferences.setCheckAnswersImmediately(true)
        val attempt = session.startOrResume(test)
        session.answer(attempt.id, test.questions.first(), 0)

        val progress = session.progress.first()

        assertEquals(setOf(test.number), progress.inProgress)
        assertEquals(attempt.id, progress.resumable?.id)
        assertEquals(1, progress.questionsAnswered) // locked answer counts
        assertEquals(4, progress.poolSize)

        // Turning checking off makes that answer editable again, and what is editable isn't final:
        // the statistics follow the setting, without the attempt having to be restarted.
        preferences.setCheckAnswersImmediately(false)
        assertEquals(0, session.progress.first().questionsAnswered)
    }

    @Test
    fun openAttemptIgnoresClosedAttemptsAndOtherBanks() = runTest {
        val session = session()
        val attempt = session.startOrResume(session.tests.first())
        assertEquals(attempt.id, session.openAttempt(attempt.id).first()?.id)

        attempts.discard(attempt.id)
        assertNull(session.openAttempt(attempt.id).first())

        val otherVersion = ExamSession(bank.copy(version = 2), LicenseCategory.B, attempts, preferences)
        val v2Attempt = otherVersion.startOrResume(otherVersion.tests.first())
        assertNull(session.openAttempt(v2Attempt.id).first())
        assertNull(session.openAttempt(null).first())
    }

    @Test
    fun attemptIncludesFinishedButNotDiscardedAttempts() = runTest {
        val session = session()
        val finished = session.startOrResume(session.tests.first())
        session.finish(finished.id)
        val discarded = session.startOrResume(session.tests.first())
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
        val attempt = session.startOrResume(session.tests.first())
        assertEquals(attempt.questionIds, session.questionsOf(attempt).map { it.id })
    }
}

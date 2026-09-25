package uno.lux.autoskola.stats

import uno.lux.autoskola.TestResult
import uno.lux.autoskola.bank.LicenseCategory
import uno.lux.autoskola.bank.Question
import kotlin.test.Test
import kotlin.test.assertEquals

class AttemptProgressTest {

    private fun question(id: Int, points: Int = 1, section: Int = 1) = Question(
        id = id,
        section = section,
        text = "Q$id",
        answers = listOf("a", "b", "c"),
        correct = 0,
        points = points,
        groups = listOf(LicenseCategory.B),
    )

    private val questions =
        listOf(question(10, points = 3), question(20, points = 2), question(30, points = 1, section = 2))

    private fun attempt(vararg answers: Pair<Int, Int>) = TestAttempt(
        id = "a",
        bankId = "sk-sk",
        bankVersion = 1,
        category = LicenseCategory.B,
        kind = AttemptKind.PracticeTest,
        testNumber = 1,
        questionIds = questions.map { it.id },
        checkImmediately = false,
        startedAt = 0,
        answers = answers.map { (id, chosen) ->
            AnswerRecord(questionId = id, chosen = chosen, correct = chosen == 0, answeredAt = 1)
        },
    )

    @Test
    fun answersAreMappedToQuestionPositions() {
        assertEquals(mapOf(0 to 2, 2 to 0), attempt(10 to 2, 30 to 0).answersByIndex())
    }

    @Test
    fun resumesAtFirstUnansweredQuestion() {
        assertEquals(0, attempt().resumeIndex())
        assertEquals(1, attempt(10 to 0, 30 to 1).resumeIndex())
    }

    @Test
    fun resumesAtLastQuestionWhenEverythingIsAnswered() {
        assertEquals(2, attempt(10 to 0, 20 to 0, 30 to 0).resumeIndex())
    }

    @Test
    fun scoreCountsPointsOfCorrectAnswersAgainstAllQuestions() {
        // Correct on 10 (3 points) and 30 (1 point), wrong on 20, nothing else missing.
        assertEquals(TestResult(score = 4, maxScore = 6), attempt(10 to 0, 20 to 1, 30 to 0).score(questions))
    }

    @Test
    fun unansweredQuestionsScoreNothing() {
        assertEquals(TestResult(score = 0, maxScore = 6), attempt().score(questions))
    }

    @Test
    fun isFullyAnsweredOnlyOnceEveryQuestionHasAnAnswer() {
        assertEquals(false, attempt().isFullyAnswered)
        assertEquals(false, attempt(10 to 0, 30 to 1).isFullyAnswered)
        assertEquals(true, attempt(10 to 0, 20 to 2, 30 to 1).isFullyAnswered)
    }

    @Test
    fun summaryCountsCorrectWrongAndUnansweredQuestions() {
        val summary = attempt(10 to 0, 30 to 1).summary(questions)

        assertEquals(TestResult(score = 3, maxScore = 6), summary.result)
        assertEquals(1, summary.correct)
        assertEquals(1, summary.wrong)
        assertEquals(1, summary.unanswered)
    }

    @Test
    fun summaryBreaksResultsDownBySectionInQuestionOrder() {
        val summary = attempt(10 to 0, 20 to 0, 30 to 1).summary(questions)

        assertEquals(
            listOf(
                SectionSummary(section = 1, correct = 2, total = 2, score = 5, maxScore = 5),
                SectionSummary(section = 2, correct = 0, total = 1, score = 0, maxScore = 1),
            ),
            summary.sections,
        )
    }
}

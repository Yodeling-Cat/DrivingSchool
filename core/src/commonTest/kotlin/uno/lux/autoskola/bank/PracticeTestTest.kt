package uno.lux.autoskola.bank

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PracticeTestTest {

    private fun question(id: Int, section: Int, groups: List<LicenseCategory> = LicenseCategory.entries) =
        Question(
            id = id,
            section = section,
            text = "Q$id",
            answers = listOf("a", "b", "c"),
            correct = 0,
            points = section,
            groups = groups,
        )

    private val bank = QuestionBank(
        id = "test",
        version = 1,
        country = "SK",
        language = "sk",
        imageDir = "test",
        sections = listOf(
            Section(id = 1, title = "One", questionsPerTest = 2, points = 1),
            Section(id = 2, title = "Two", questionsPerTest = 1, points = 2),
        ),
        questions = (1..7).map { question(it, section = 1) } +
            (8..9).map { question(it, section = 2) } +
            question(10, section = 1, groups = listOf(LicenseCategory.C)) +
            question(11, section = 1).copy(answers = listOf("only the correct one")),
    )

    @Test
    fun everyEligibleQuestionAppearsAndTestsFollowSectionStructure() {
        val tests = bank.practiceTests(LicenseCategory.B)

        assertEquals(4, tests.size) // ceil(7 / 2)
        tests.forEach { test ->
            assertEquals(2, test.questions.count { it.section == 1 })
            assertEquals(1, test.questions.count { it.section == 2 })
            assertEquals(test.questions.size, test.questions.distinct().size)
        }
        assertEquals((1..9).toSet(), tests.flatMap { it.questions }.map { it.id }.toSet())
    }

    @Test
    fun excludesOtherGroupsAndQuestionsWithoutWrongAnswers() {
        val ids = bank.practiceTests(LicenseCategory.B).flatMap { it.questions }.map { it.id }
        assertTrue(10 !in ids)
        assertTrue(11 !in ids)
        assertTrue(10 in bank.practiceTests(LicenseCategory.C).flatMap { it.questions }.map { it.id })
    }

    @Test
    fun isDeterministic() {
        assertEquals(bank.practiceTests(LicenseCategory.B), bank.practiceTests(LicenseCategory.B))
    }

    @Test
    fun decodesImporterOutput() {
        val json = """
            {"id":"sk-sk","version":1,"country":"SK","language":"sk","imageDir":"minv-sk",
             "sections":[{"id":1,"title":"Pravidlá","questionsPerTest":8,"points":3}],
             "questions":[{"id":1125,"section":1,"text":"Otázka","answers":["a","b","c"],
                           "correct":1,"points":3,"image":null,"groups":["A","BE","T"]}]}
        """.trimIndent()

        val decoded = QuestionBank.decode(json)
        assertEquals(listOf(LicenseCategory.A, LicenseCategory.BE, LicenseCategory.T), decoded.questions.single().groups)
    }
}

package uno.lux.autoskola.situations

import uno.lux.autoskola.bank.LicenseCategory
import uno.lux.autoskola.bank.Question
import uno.lux.autoskola.bank.QuestionBank
import uno.lux.autoskola.bank.Section
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SituationsTest {

    @Test
    fun theSituationsAreTheOnesTheExamAsksAbout() {
        val bank = bank(
            question(1, section = SITUATIONS, image = "obr3/ds/01.png"),
            // A situation drawing, but asked about in another topic: not one of these situations.
            question(2, section = 1, image = "obr3/ds/02.png"),
            question(3, section = 1, image = "obr3/dz/110.jpg"),
        )

        val situations = situations(bank, LicenseCategory.B)

        assertEquals(listOf("obr3/ds/01.png"), situations.map { it.image })
    }

    @Test
    fun theQuestionsAboutOneDrawingAreOneSituation() {
        val bank = bank(
            question(1, section = SITUATIONS, image = "obr3/ds/01.png", text = "Ako prvé prejde"),
            question(2, section = SITUATIONS, image = "obr3/ds/01.png", text = "Ako posledné prejde"),
        )

        val situations = situations(bank, LicenseCategory.B)

        assertEquals(1, situations.size)
        assertEquals(listOf(1, 2), situations[0].questions.map { it.id })
    }

    @Test
    fun theSituationsComeInTheBanksOrder() {
        val bank = bank(
            question(1, section = SITUATIONS, image = "obr3/ds/07.png"),
            question(2, section = SITUATIONS, image = "obr3/ds/01.png"),
            question(3, section = SITUATIONS, image = "obr3/ds/07.png"),
        )

        assertEquals(
            listOf("obr3/ds/07.png", "obr3/ds/01.png"),
            situations(bank, LicenseCategory.B).map { it.image },
        )
    }

    @Test
    fun aQuestionWithoutADrawingIsNotASituation() {
        val bank = bank(
            question(1, section = SITUATIONS, image = null),
            question(2, section = SITUATIONS, image = "obr3/ds/01.png"),
        )

        assertEquals(listOf("obr3/ds/01.png"), situations(bank, LicenseCategory.B).map { it.image })
    }

    @Test
    fun onlyTheSituationsTheLicenseGroupIsAskedAreListed() {
        val bank = bank(
            question(1, section = SITUATIONS, image = "obr3/ds/01.png", groups = listOf(LicenseCategory.B)),
            question(2, section = SITUATIONS, image = "obr3/ds/02.png", groups = listOf(LicenseCategory.A)),
        )

        assertEquals(listOf("obr3/ds/01.png"), situations(bank, LicenseCategory.B).map { it.image })
    }

    @Test
    fun aQuestionWithNothingToChooseBetweenIsNotDrilled() {
        // The exam's pool holds the questions a driver can answer; a statement is not one.
        val bank = bank(
            question(1, section = SITUATIONS, image = "obr3/ds/01.png", answers = listOf("Áno")),
            question(2, section = SITUATIONS, image = "obr3/ds/02.png"),
        )

        assertEquals(listOf("obr3/ds/02.png"), situations(bank, LicenseCategory.B).map { it.image })
    }

    @Test
    fun aBankWithoutThatTopicHasNoSituations() {
        val bank = bank(question(1, section = 1, image = "obr3/dz/110.jpg"))

        assertTrue(situations(bank, LicenseCategory.B).isEmpty())
    }

    @Test
    fun everySituationHoldsTheDrawingItIsAbout() {
        val bank = bank(
            question(1, section = SITUATIONS, image = "obr3/ds/03.png"),
            question(2, section = SITUATIONS, image = "obr3/ds/03.png"),
            question(3, section = SITUATIONS, image = "obr3/ds/04.png"),
        )

        val situations = situations(bank, LicenseCategory.B)

        assertEquals(2, situations.size)
        assertTrue(situations.all { it.questions.isNotEmpty() })
        assertTrue(situations.all { situation -> situation.questions.all { it.image == situation.image } })
    }

    private fun bank(vararg questions: Question) = QuestionBank(
        id = "sk",
        version = 1,
        country = "SK",
        language = "sk",
        imageDir = "minv-sk",
        sections = listOf(Section(SITUATIONS, "Dopravné situácie na križovatkách", questionsPerTest = 4, points = 4)),
        questions = questions.toList(),
    )

    private fun question(
        id: Int,
        section: Int,
        image: String?,
        text: String = "Vozidlá prejdú cez križovatku v tomto poradí:",
        answers: List<String> = listOf("červené vozidlo.", "modré vozidlo.", "vaše vozidlo."),
        groups: List<LicenseCategory> = listOf(LicenseCategory.A, LicenseCategory.B),
    ) = Question(
        id = id,
        section = section,
        text = text,
        answers = answers,
        correct = 0,
        points = 4,
        image = image,
        groups = groups,
    )

    private companion object {
        const val SITUATIONS = 4
    }
}

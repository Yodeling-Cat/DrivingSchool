package com.spiraclestudios.autoskola.data.banks

import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.bank.Question
import com.spiraclestudios.autoskola.bank.QuestionBank
import com.spiraclestudios.autoskola.bank.QuestionBankInfo
import com.spiraclestudios.autoskola.bank.Section

/** Serves [banks] from memory and records what was read. */
class FakeQuestionBankDataSource(private val index: List<QuestionBankInfo>, private val banks: List<QuestionBank>) :
    QuestionBankDataSource {
    val bankReads = mutableListOf<String>()
    var indexReads = 0

    override suspend fun readIndex(): List<QuestionBankInfo> = index.also { indexReads++ }

    override suspend fun readBank(id: String, version: Int): QuestionBank? {
        bankReads += "$id-v$version"
        return banks.firstOrNull { it.id == id && it.version == version }
    }

    override fun imageUri(imageDir: String, path: String): String = "test://$imageDir/$path"

    companion object {
        /** A data source with just [bank], listed as the latest version. */
        fun of(bank: QuestionBank) =
            FakeQuestionBankDataSource(listOf(testBankInfo(bank.id, bank.version)), listOf(bank))
    }
}

fun testBankInfo(id: String, version: Int) = QuestionBankInfo(
    id = id,
    version = version,
    country = "SK",
    language = "sk",
    questionCounts = emptyMap(),
)

/** A small bank: questions 1-3 in section 1 and 4 in section 2 for B and C, and 5 in section 2 for C only. */
fun testBank(id: String = "sk-sk", version: Int = 1) = QuestionBank(
    id = id,
    version = version,
    country = "SK",
    language = "sk",
    imageDir = "minv-sk",
    sections = listOf(
        Section(id = 1, title = "One", questionsPerTest = 2, points = 1),
        Section(id = 2, title = "Two", questionsPerTest = 1, points = 2),
    ),
    questions = listOf(
        testQuestion(1, section = 1, image = "q1.png"),
        testQuestion(2, section = 1),
        testQuestion(3, section = 1),
        testQuestion(4, section = 2),
        testQuestion(5, section = 2, groups = listOf(LicenseCategory.C)),
    ),
)

/** Answer 0 is right; 1 and 2 are wrong. */
fun testQuestion(
    id: Int,
    section: Int,
    groups: List<LicenseCategory> = listOf(LicenseCategory.B, LicenseCategory.C),
    image: String? = null,
) = Question(
    id = id,
    section = section,
    text = "Q$id",
    answers = listOf("right", "wrong", "wrong"),
    correct = 0,
    points = section,
    image = image,
    groups = groups,
)

/**
 * A bank whose topic holds situations: two drawings, the first asked about twice, and a third one
 * the drivers of group A alone are asked.
 */
fun testSituationsBank(id: String = "sk-sk", version: Int = 1) = QuestionBank(
    id = id,
    version = version,
    country = "SK",
    language = "sk",
    imageDir = "minv-sk",
    sections = listOf(
        Section(id = 4, title = "Dopravné situácie na križovatkách", questionsPerTest = 4, points = 4),
    ),
    questions = listOf(
        testQuestion(1813, section = 4, image = "obr3/ds/01.png"),
        testQuestion(1814, section = 4, image = "obr3/ds/01.png"),
        testQuestion(1815, section = 4, image = "obr3/ds/02.png"),
        testQuestion(1816, section = 4, image = "obr3/ds/03.png", groups = listOf(LicenseCategory.A)),
    ),
)

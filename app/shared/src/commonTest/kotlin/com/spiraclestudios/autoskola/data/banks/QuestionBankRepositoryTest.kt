package com.spiraclestudios.autoskola.data.banks

import com.spiraclestudios.autoskola.bank.QuestionBank
import com.spiraclestudios.autoskola.bank.QuestionBankInfo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QuestionBankRepositoryTest {

    @Test
    fun latestBankUsesTheVersionListedInTheIndex() = runTest {
        val source = FakeQuestionBankDataSource(
            index = listOf(testBankInfo("sk-sk", 2)),
            banks = listOf(testBank("sk-sk", 1), testBank("sk-sk", 2)),
        )

        assertEquals(2, QuestionBankRepository(source).latestBank("sk-sk")?.version)
    }

    @Test
    fun unknownBankIsNull() = runTest {
        val repository =
            QuestionBankRepository(
                FakeQuestionBankDataSource(index = listOf(testBankInfo("sk-sk", 1)), banks = emptyList()),
            )

        assertNull(repository.latestBank("cz-cz"))
        assertNull(repository.latestBank("sk-sk")) // listed, but its file is missing
    }

    @Test
    fun indexAndBanksAreReadOnce() = runTest {
        val source =
            FakeQuestionBankDataSource(index = listOf(testBankInfo("sk-sk", 1)), banks = listOf(testBank("sk-sk", 1)))
        val repository = QuestionBankRepository(source)

        repeat(3) { repository.latestBank("sk-sk") }
        repository.bank("sk-sk", 1)

        assertEquals(1, source.indexReads)
        assertEquals(listOf("sk-sk-v1"), source.bankReads)
    }

    @Test
    fun aPictureIsAddressedByItsPath() = runTest {
        // A situation's drawing is not a question's picture, but the path is relative to the same
        // image directory, so the loader is handed the same kind of URI.
        val bank = testBank("sk-sk", 1)
        val repository = QuestionBankRepository(FakeQuestionBankDataSource.of(bank))

        assertEquals("test://minv-sk/obr3/ds/01.png", repository.imageUri(bank, "obr3/ds/01.png"))
    }

    @Test
    fun aQuestionWithoutAPictureHasNoImage() = runTest {
        val bank = testBank("sk-sk", 1)
        val repository = QuestionBankRepository(FakeQuestionBankDataSource.of(bank))

        assertEquals("test://minv-sk/q1.png", repository.imageUri(bank, testQuestion(1, section = 1, image = "q1.png")))
        assertNull(repository.imageUri(bank, testQuestion(2, section = 1)))
    }

    @Test
    fun bankIsReadAgainAfterAFailedRead() = runTest {
        // A read can fail once - or be canceled - without the bank being gone. Remembering the
        // failure would keep the exam "missing" for the rest of the process, which drops the user
        // into onboarding and never lets them out.
        val repository = QuestionBankRepository(FailingOnceBankDataSource(testBank("sk-sk")))

        assertNull(repository.bank("sk-sk", 1))
        assertEquals("sk-sk", repository.bank("sk-sk", 1)?.id)
    }
}

/** Fails the first read of [bank], then serves it, as an interrupted read would. */
private class FailingOnceBankDataSource(private val bank: QuestionBank) : QuestionBankDataSource {
    private var failuresLeft = 1

    override suspend fun readIndex(): List<QuestionBankInfo> = listOf(testBankInfo(bank.id, bank.version))

    override suspend fun readBank(id: String, version: Int): QuestionBank? {
        if (failuresLeft > 0) {
            failuresLeft--
            return null
        }
        return bank.takeIf { it.id == id && it.version == version }
    }

    override fun imageUri(imageDir: String, path: String): String = "test://$imageDir/$path"
}

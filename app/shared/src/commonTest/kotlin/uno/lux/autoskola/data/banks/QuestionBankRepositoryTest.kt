package uno.lux.autoskola.data.banks

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
}

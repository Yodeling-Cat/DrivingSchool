package uno.lux.autoskola.data.answers

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import uno.lux.autoskola.answers.PracticeAnswers

/**
 * The answers given while reading, kept per version of a bank.
 *
 * Writes are serialized and run to the end even if the screen that asked for them goes away, so two
 * answers picked in quick succession can't lose one another. See AGENTS.md, "Data access".
 */
class PracticeAnswerRepository(private val dataSource: PracticeAnswerDataSource) {

    private val writes = Mutex()

    fun answers(bankId: String, bankVersion: Int): Flow<PracticeAnswers> = dataSource.answers(bankId, bankVersion)

    /** Records the option the driver picked, replacing the answer they gave before, if any. */
    suspend fun answer(
        bankId: String,
        bankVersion: Int,
        questionId: Int,
        chosen: Int,
    ) = write { dataSource.answer(bankId, bankVersion, questionId, chosen) }

    /** Records that the driver checked the answer to [questionId], which is remembered with it. */
    suspend fun reveal(bankId: String, bankVersion: Int, questionId: Int) =
        write { dataSource.reveal(bankId, bankVersion, questionId) }

    private suspend fun write(change: suspend () -> Unit) = withContext(NonCancellable) { writes.withLock { change() } }
}

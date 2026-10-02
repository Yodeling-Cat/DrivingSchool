package com.spiraclestudios.autoskola.data.answers

import com.spiraclestudios.autoskola.answers.PracticeAnswers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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
    ) = withContext(NonCancellable) {
        writes.withLock { dataSource.answer(bankId, bankVersion, questionId, chosen) }
    }

    /**
     * Clears every answer of bank [bankId] at [bankVersion], so the material is read from the start
     * again - the way out of a run through the situations that went wrong from the beginning.
     */
    suspend fun reset(bankId: String, bankVersion: Int) = withContext(NonCancellable) {
        writes.withLock { dataSource.reset(bankId, bankVersion) }
    }
}

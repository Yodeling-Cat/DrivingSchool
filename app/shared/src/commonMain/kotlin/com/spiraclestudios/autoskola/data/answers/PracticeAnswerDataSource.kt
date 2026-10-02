package com.spiraclestudios.autoskola.data.answers

import com.spiraclestudios.autoskola.answers.PracticeAnswers
import kotlinx.coroutines.flow.Flow

/**
 * Where the answers given while reading are kept, one set per version of a bank.
 *
 * Each implementation is a backend: [SettingsPracticeAnswerDataSource] keeps them in the app's
 * settings today, and a database would be another one. Reads follow changes, writes suspend.
 */
interface PracticeAnswerDataSource {
    /** The answers given to the questions of bank [bankId] at [bankVersion]. */
    fun answers(bankId: String, bankVersion: Int): Flow<PracticeAnswers>

    /** Records the option the driver picked for [questionId], replacing the answer that was there. */
    suspend fun answer(
        bankId: String,
        bankVersion: Int,
        questionId: Int,
        chosen: Int,
    )
}

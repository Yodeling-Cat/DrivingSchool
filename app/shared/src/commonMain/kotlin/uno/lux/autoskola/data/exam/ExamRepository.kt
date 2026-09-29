package uno.lux.autoskola.data.exam

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import uno.lux.autoskola.data.attempts.AttemptRepository
import uno.lux.autoskola.data.banks.QuestionBankRepository
import uno.lux.autoskola.data.preferences.PreferencesRepository

sealed interface CurrentExam {
    data object NotSelected : CurrentExam
    data object Loading : CurrentExam

    /** The selected bank no longer exists, e.g. it was removed in an update. */
    data object Missing : CurrentExam
    data class Ready(val session: ExamSession) : CurrentExam
}

/** The exam the user has selected, on the latest version of its question bank. */
class ExamRepository(
    private val preferences: PreferencesRepository,
    private val banks: QuestionBankRepository,
    private val attempts: AttemptRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val current: Flow<CurrentExam> = preferences.preferences
        .map { it.selection }
        .distinctUntilChanged()
        .transformLatest { selection ->
            if (selection == null) {
                emit(CurrentExam.NotSelected)
                return@transformLatest
            }
            emit(CurrentExam.Loading)
            val bank = banks.latestBank(selection.bankId)
            if (bank == null) {
                emit(CurrentExam.Missing)
                return@transformLatest
            }
            emit(CurrentExam.Ready(ExamSession(bank, selection.category, attempts)))
        }
}

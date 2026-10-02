package com.spiraclestudios.autoskola.data.exam

import com.spiraclestudios.autoskola.data.analytics.AnalyticsEvent
import com.spiraclestudios.autoskola.data.analytics.AnalyticsRepository
import com.spiraclestudios.autoskola.data.analytics.bankMissingProperties
import com.spiraclestudios.autoskola.data.attempts.AttemptRepository
import com.spiraclestudios.autoskola.data.banks.QuestionBankRepository
import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository
import com.spiraclestudios.autoskola.data.preferences.TestSelection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest

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
    private val analytics: AnalyticsRepository,
) {
    /**
     * The selection whose missing bank has already been reported, so one occurrence is reported
     * once rather than on every collector of [current] - it is a cold flow, and each screen that
     * reads it would otherwise re-report the same missing bank as the app recomposes.
     */
    private var reportedMissing: TestSelection? = null

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
                reportMissing(selection)
                emit(CurrentExam.Missing)
                return@transformLatest
            }
            // A bank that resolves is the end of the occurrence: a later loss of it is a new one.
            reportedMissing = null
            emit(CurrentExam.Ready(ExamSession(bank, selection.category, attempts, preferences)))
        }

    /** Reports the selected bank being gone, once per selection that is found missing. */
    private fun reportMissing(selection: TestSelection) {
        if (reportedMissing == selection) return
        reportedMissing = selection
        analytics.capture(AnalyticsEvent.BankMissing, bankMissingProperties(selection.bankId))
    }
}

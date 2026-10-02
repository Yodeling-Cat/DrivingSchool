package com.spiraclestudios.autoskola

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spiraclestudios.autoskola.data.exam.CurrentExam
import com.spiraclestudios.autoskola.data.exam.ExamRepository
import com.spiraclestudios.autoskola.data.preferences.Preferences
import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository
import com.spiraclestudios.autoskola.i18n.AppLanguage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Which part of the app is shown. Inside [Exam], the navigation back stack takes over. */
enum class AppDestination { Loading, Disclaimer, ChooseExam, Exam }

data class AppUiState(val language: AppLanguage, val destination: AppDestination)

/** Decides what the app shows at the top level: onboarding until it's done, then the exam. */
class AppViewModel(preferences: PreferencesRepository, exams: ExamRepository) : ViewModel() {

    val uiState: StateFlow<AppUiState> =
        combine(preferences.preferences, exams.current, ::state)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                state(preferences.preferences.value, CurrentExam.Loading),
            )

    private fun state(preferences: Preferences, exam: CurrentExam) = AppUiState(
        language = preferences.language,
        destination = when {
            !preferences.disclaimerAccepted -> AppDestination.Disclaimer
            exam is CurrentExam.Loading -> AppDestination.Loading
            exam is CurrentExam.Ready -> AppDestination.Exam
            else -> AppDestination.ChooseExam
        },
    )
}

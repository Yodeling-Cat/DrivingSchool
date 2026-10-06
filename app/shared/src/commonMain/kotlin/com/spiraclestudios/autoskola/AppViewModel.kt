package com.spiraclestudios.autoskola

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spiraclestudios.autoskola.data.exam.CurrentExam
import com.spiraclestudios.autoskola.data.exam.ExamRepository
import com.spiraclestudios.autoskola.data.preferences.CURRENT_LEGAL_VERSION
import com.spiraclestudios.autoskola.data.preferences.Preferences
import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository
import com.spiraclestudios.autoskola.i18n.AppLanguage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Which part of the app is shown. Inside [Exam], the navigation back stack takes over. */
enum class AppDestination { Loading, Disclaimer, Consent, ChooseExam, Exam }

/** What the screens above the exam can tell the app. */
sealed interface AppUiEvent {
    /** The first screen has been read, so the consent screen comes next. */
    data object DisclaimerRead : AppUiEvent
}

data class AppUiState(val language: AppLanguage, val destination: AppDestination)

/** Decides what the app shows at the top level: onboarding until it's done, then the exam. */
class AppViewModel(private val preferences: PreferencesRepository, exams: ExamRepository) : ViewModel() {

    /**
     * Whether the exam has been shown at least once. Choosing another exam (settings → change exam)
     * re-resolves it — `ExamRepository.current` emits [CurrentExam.Loading] for every new selection
     * — and from then on the app stays inside the exam while that happens, instead of going back to
     * [AppDestination.Loading]. The loading screen would take the screens the choice was made from
     * down with it: they are entries on the back stack under the exam, and the wizard that was
     * still running on top of them would start again from its first step.
     */
    private var examShown = false

    val uiState: StateFlow<AppUiState> =
        combine(preferences.preferences, exams.current, ::state)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                state(preferences.preferences.value, CurrentExam.Loading),
            )

    private fun state(preferences: Preferences, exam: CurrentExam): AppUiState {
        val destination = when {
            // Onboarding, in the order it is walked, and from stored facts only: the disclaimer is
            // read once, and until the documents are accepted at their current version the consent
            // screen follows it. Stored, because a screen that depends on the frame that drew it is a
            // screen a rotation takes away — this one has to survive both that and a killed process.
            preferences.legalAcceptedVersion < CURRENT_LEGAL_VERSION && preferences.disclaimerRead ->
                AppDestination.Consent

            preferences.legalAcceptedVersion < CURRENT_LEGAL_VERSION -> AppDestination.Disclaimer

            exam is CurrentExam.Ready -> AppDestination.Exam

            // The exam being read again, after it has been shown once: stay where the user is.
            exam is CurrentExam.Loading && examShown -> AppDestination.Exam

            exam is CurrentExam.Loading -> AppDestination.Loading

            else -> AppDestination.ChooseExam
        }
        if (destination == AppDestination.Exam) examShown = true
        return AppUiState(preferences.language, destination)
    }

    fun onEvent(event: AppUiEvent) {
        when (event) {
            AppUiEvent.DisclaimerRead -> preferences.setDisclaimerRead(true)
        }
    }
}

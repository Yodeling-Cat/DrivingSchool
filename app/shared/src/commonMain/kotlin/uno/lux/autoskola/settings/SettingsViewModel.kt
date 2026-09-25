package uno.lux.autoskola.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import uno.lux.autoskola.data.exam.CurrentExam
import uno.lux.autoskola.data.exam.ExamRepository
import uno.lux.autoskola.data.preferences.Preferences
import uno.lux.autoskola.data.preferences.PreferencesRepository
import uno.lux.autoskola.i18n.AppLanguage
import uno.lux.autoskola.onboarding.ExamLabel

data class SettingsUiState(
    val language: AppLanguage,
    val checkAnswersImmediately: Boolean,
    /** `null` while the exam loads. */
    val exam: ExamLabel?,
)

sealed interface SettingsUiEvent {
    data class SelectLanguage(val language: AppLanguage) : SettingsUiEvent
    data class SetCheckAnswersImmediately(val enabled: Boolean) : SettingsUiEvent
}

class SettingsViewModel(private val preferences: PreferencesRepository, exams: ExamRepository) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> =
        combine(preferences.preferences, exams.current, ::state)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                state(preferences.preferences.value, CurrentExam.Loading),
            )

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.SelectLanguage -> preferences.selectLanguage(event.language)
            is SettingsUiEvent.SetCheckAnswersImmediately -> preferences.setCheckAnswersImmediately(event.enabled)
        }
    }

    private fun state(preferences: Preferences, exam: CurrentExam) = SettingsUiState(
        language = preferences.language,
        checkAnswersImmediately = preferences.checkAnswersImmediately,
        exam = (exam as? CurrentExam.Ready)?.session?.let { ExamLabel(it.category, it.bank.language) },
    )
}

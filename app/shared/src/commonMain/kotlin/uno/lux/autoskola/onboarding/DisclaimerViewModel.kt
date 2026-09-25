package uno.lux.autoskola.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import uno.lux.autoskola.data.preferences.PreferencesRepository
import uno.lux.autoskola.i18n.AppLanguage

data class DisclaimerUiState(val language: AppLanguage)

sealed interface DisclaimerUiEvent {
    data class SelectLanguage(val language: AppLanguage) : DisclaimerUiEvent
    data object Accept : DisclaimerUiEvent
}

class DisclaimerViewModel(private val preferences: PreferencesRepository) : ViewModel() {

    val uiState: StateFlow<DisclaimerUiState> = preferences.preferences
        .map { DisclaimerUiState(it.language) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DisclaimerUiState(preferences.preferences.value.language))

    fun onEvent(event: DisclaimerUiEvent) {
        when (event) {
            is DisclaimerUiEvent.SelectLanguage -> preferences.selectLanguage(event.language)
            DisclaimerUiEvent.Accept -> preferences.acceptDisclaimer()
        }
    }
}

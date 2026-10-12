package com.spiraclestudios.autoskola.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spiraclestudios.autoskola.data.analytics.AnalyticsRepository
import com.spiraclestudios.autoskola.data.analytics.languageChanged
import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository
import com.spiraclestudios.autoskola.i18n.AppLanguage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DisclaimerUiState(
    val language: AppLanguage,
    /** The country the app is built for, whose ministry the questions are credited to. */
    val country: String,
)

sealed interface DisclaimerUiEvent {
    data class SelectLanguage(val language: AppLanguage) : DisclaimerUiEvent
}

class DisclaimerViewModel(
    private val preferences: PreferencesRepository,
    private val analytics: AnalyticsRepository,
    private val country: String,
) : ViewModel() {

    val uiState: StateFlow<DisclaimerUiState> = preferences.preferences
        .map { DisclaimerUiState(it.language, country) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DisclaimerUiState(preferences.preferences.value.language, country),
        )

    fun onEvent(event: DisclaimerUiEvent) {
        when (event) {
            is DisclaimerUiEvent.SelectLanguage -> {
                analytics.languageChanged(preferences.preferences.value.language, event.language)
                preferences.selectLanguage(event.language)
            }
        }
    }
}

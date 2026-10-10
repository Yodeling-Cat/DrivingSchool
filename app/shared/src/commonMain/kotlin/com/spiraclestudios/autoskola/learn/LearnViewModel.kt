package com.spiraclestudios.autoskola.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spiraclestudios.autoskola.data.signs.SignCatalogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

sealed interface LearnUiState {
    data object Loading : LearnUiState

    /** The subjects the hub offers, in the order it offers them. */
    data class Ready(val sections: List<LearnSection>) : LearnUiState
}

/**
 * Which subjects the Learn hub offers. The road signs are offered only where the app bundles a
 * catalog of them: every country's signs are its own, and the Czech app has none yet.
 */
class LearnViewModel(catalogs: SignCatalogRepository) : ViewModel() {

    val uiState: StateFlow<LearnUiState> = flow { emit(LearnUiState.Ready(learnSections(catalogs.hasBundled()))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LearnUiState.Loading)
}

/** The subjects the hub offers, given whether the app has road signs to learn. */
internal fun learnSections(hasRoadSigns: Boolean): List<LearnSection> = LearnSection.entries
    // Legislation isn't ready yet: drop this filter to bring its card back.
    .filter { it != LearnSection.Legislation }
    .filter { it != LearnSection.RoadSigns || hasRoadSigns }

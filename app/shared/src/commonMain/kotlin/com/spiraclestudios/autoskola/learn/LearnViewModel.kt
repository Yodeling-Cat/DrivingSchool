package com.spiraclestudios.autoskola.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spiraclestudios.autoskola.data.exam.ExamRepository
import com.spiraclestudios.autoskola.data.signs.SignCatalogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface LearnUiState {
    data object Loading : LearnUiState

    /** The subjects the hub offers for the exam being taken, in the order it offers them. */
    data class Ready(val sections: List<LearnSection>) : LearnUiState
}

/**
 * Which subjects the Learn hub offers. They follow the exam: its country's road signs are offered
 * only when the app has a catalog of them, since every country's signs are its own.
 */
class LearnViewModel(exams: ExamRepository, catalogs: SignCatalogRepository) : ViewModel() {

    val uiState: StateFlow<LearnUiState> = exams.session.filterNotNull()
        .map { it.bank.country }
        .distinctUntilChanged()
        .map { country -> LearnUiState.Ready(learnSections(hasRoadSigns = catalogs.forCountry(country) != null)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LearnUiState.Loading)
}

/** The subjects the hub offers, given whether the exam's country has road signs to learn. */
internal fun learnSections(hasRoadSigns: Boolean): List<LearnSection> = LearnSection.entries
    // Legislation isn't ready yet: drop this filter to bring its card back.
    .filter { it != LearnSection.Legislation }
    .filter { it != LearnSection.RoadSigns || hasRoadSigns }

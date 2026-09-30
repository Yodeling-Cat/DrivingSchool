package uno.lux.autoskola.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uno.lux.autoskola.data.signs.SignCatalogueRepository
import uno.lux.autoskola.signs.Sign
import uno.lux.autoskola.signs.SignCatalogue
import uno.lux.autoskola.signs.searchSigns

/** One sign as the list draws it: the catalogue's text, and the picture's URI. */
data class SignItem(
    val number: String,
    val name: String,
    val description: String,
    val imageUri: String,
)

sealed interface SignsUiState {
    data object Loading : SignsUiState

    /**
     * The signs matching the search. [total] is how many the catalogue holds altogether, which is
     * what the screen reports while a search is running.
     */
    data class Ready(val signs: List<SignItem>, val query: String, val total: Int) : SignsUiState
}

sealed interface SignsUiEvent {
    /** Filters the list; an empty query shows every sign. */
    data class Search(val query: String) : SignsUiEvent
}

class SignsViewModel(private val catalogues: SignCatalogueRepository) : ViewModel() {

    private val query = MutableStateFlow("")

    private val catalogue = MutableStateFlow<SignCatalogue?>(null)

    init {
        viewModelScope.launch { catalogue.value = catalogues.bundled() }
    }

    val uiState: StateFlow<SignsUiState> = combine(catalogue, query) { catalogue, query ->
        if (catalogue == null) {
            SignsUiState.Loading
        } else {
            SignsUiState.Ready(
                signs = searchSigns(catalogue.signs, query).map { it.toItem(catalogue) },
                query = query,
                total = catalogue.signs.size,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SignsUiState.Loading)

    fun onEvent(event: SignsUiEvent) {
        when (event) {
            is SignsUiEvent.Search -> query.value = event.query
        }
    }

    private fun Sign.toItem(catalogue: SignCatalogue) = SignItem(
        number = number,
        name = name,
        description = description,
        imageUri = catalogues.imageUri(catalogue, this),
    )
}

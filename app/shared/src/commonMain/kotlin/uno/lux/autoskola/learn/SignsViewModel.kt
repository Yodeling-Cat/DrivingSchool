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
import uno.lux.autoskola.signs.SignGroup
import uno.lux.autoskola.signs.groupSigns
import uno.lux.autoskola.signs.searchSigns

/** One sign as the list draws it: the catalogue's text, and the picture's URI. */
data class SignItem(
    val number: String,
    val name: String,
    val description: String,
    val imageUri: String,
)

/** One heading of the list and the signs listed under it. */
data class SignGroupItem(
    /** The part of the catalogue the signs come from, e.g. "VÝSTRAŽNÉ ZNAČKY". */
    val kind: String,
    /** The heading inside it, e.g. "I. 4. Pohyb na ceste", or empty where it has none. */
    val group: String,
    val signs: List<SignItem>,
)

sealed interface SignsUiState {
    data object Loading : SignsUiState

    /**
     * What the search left of the catalogue, under the headings the regulation lists the signs in.
     * [total] is how many signs the catalogue holds altogether, which is what the screen reports
     * while a search is running.
     */
    data class Ready(val groups: List<SignGroupItem>, val query: String, val total: Int) : SignsUiState {
        /** How many signs the list is showing, which is all of them while no search is running. */
        val shown: Int get() = groups.sumOf { it.signs.size }
    }
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
                groups = groupSigns(searchSigns(catalogue.signs, query)).map { it.toItem(catalogue) },
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

    private fun SignGroup.toItem(catalogue: SignCatalogue) = SignGroupItem(
        kind = kind,
        group = group,
        signs = signs.map { it.toItem(catalogue) },
    )

    private fun Sign.toItem(catalogue: SignCatalogue) = SignItem(
        number = number,
        name = name,
        description = description,
        imageUri = catalogues.imageUri(catalogue, this),
    )
}

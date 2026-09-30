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
    /** A folded group shows its heading and none of its signs. */
    val folded: Boolean = false,
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

        /** True while no group is folded, which is what the bar's action folds or opens. */
        val allExpanded: Boolean get() = groups.none { it.folded }
    }
}

sealed interface SignsUiEvent {
    /** Filters the list; an empty query shows every sign, every group open. */
    data class Search(val query: String) : SignsUiEvent

    /**
     * Folds the group this heading names away, leaving the heading, or opens it again.
     *
     * A group is named by its kind and its heading rather than by where it sits, so a heading that
     * the catalogue lists twice - the two "Značky o (dávaní) prednosti v jazde" one per kind are
     * told apart by the kind - folds as the reader sees it.
     */
    data class ToggleGroup(val kind: String, val group: String) : SignsUiEvent

    /** Folds every group away, or opens them all when anything is folded. */
    data object ToggleAllGroups : SignsUiEvent
}

class SignsViewModel(private val catalogues: SignCatalogueRepository) : ViewModel() {

    private val query = MutableStateFlow("")

    private val catalogue = MutableStateFlow<SignCatalogue?>(null)

    /** The headings the reader has folded, which a search clears: its results open every group. */
    private val folded = MutableStateFlow(emptySet<GroupKey>())

    init {
        viewModelScope.launch { catalogue.value = catalogues.bundled() }
    }

    val uiState: StateFlow<SignsUiState> = combine(catalogue, query, folded) { catalogue, query, folded ->
        if (catalogue == null) {
            SignsUiState.Loading
        } else {
            SignsUiState.Ready(
                groups = groupsOf(catalogue, query).map { it.toItem(catalogue, folded) },
                query = query,
                total = catalogue.signs.size,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SignsUiState.Loading)

    fun onEvent(event: SignsUiEvent) {
        when (event) {
            is SignsUiEvent.Search -> {
                query.value = event.query
                folded.value = emptySet()
            }

            is SignsUiEvent.ToggleGroup -> {
                val key = GroupKey(event.kind, event.group)
                folded.value = if (key in folded.value) folded.value - key else folded.value + key
            }

            SignsUiEvent.ToggleAllGroups ->
                folded.value = if (folded.value.isEmpty()) everyGroup() else emptySet()
        }
    }

    /** The groups of [catalogue] that a search for [query] leaves. */
    private fun groupsOf(catalogue: SignCatalogue, query: String) = groupSigns(searchSigns(catalogue.signs, query))

    /** Every group the list is showing now, which is what the bar's action folds away. */
    private fun everyGroup(): Set<GroupKey> {
        val catalogue = catalogue.value ?: return emptySet()
        return groupsOf(catalogue, query.value).map { GroupKey(it.kind, it.group) }.toSet()
    }

    private fun SignGroup.toItem(catalogue: SignCatalogue, folded: Set<GroupKey>) = SignGroupItem(
        kind = kind,
        group = group,
        signs = signs.map { it.toItem(catalogue) },
        folded = GroupKey(kind, group) in folded,
    )

    private fun Sign.toItem(catalogue: SignCatalogue) = SignItem(
        number = number,
        name = name,
        description = description,
        imageUri = catalogues.imageUri(catalogue, this),
    )
}

/**
 * Names one heading of the list: the kind of sign it belongs to and the heading itself. The kind is
 * part of it because the catalogue has headings that read almost the same, one per kind.
 */
private data class GroupKey(val kind: String, val group: String)

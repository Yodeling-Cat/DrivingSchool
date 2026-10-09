package com.spiraclestudios.autoskola.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spiraclestudios.autoskola.data.ads.AdRepository
import com.spiraclestudios.autoskola.data.exam.ExamRepository
import com.spiraclestudios.autoskola.data.signs.SignCatalogRepository
import com.spiraclestudios.autoskola.signs.Sign
import com.spiraclestudios.autoskola.signs.SignCatalog
import com.spiraclestudios.autoskola.signs.SignGroup
import com.spiraclestudios.autoskola.signs.groupSigns
import com.spiraclestudios.autoskola.signs.searchSigns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** One sign as the list draws it: the catalog's text, and the picture's URI. */
data class SignItem(
    val number: String,
    val name: String,
    val description: String,
    val imageUri: String,
)

/** One heading of the list and the signs listed under it. */
data class SignGroupItem(
    /** The part of the catalog the signs come from, e.g. "VÝSTRAŽNÉ ZNAČKY". */
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
     * What the search left of the catalog, under the headings the regulation lists the signs in.
     * [total] is how many signs the catalog holds altogether, which is what the screen reports
     * while a search is running.
     */
    data class Ready(
        val groups: List<SignGroupItem>,
        val query: String,
        val total: Int,
        /** Whether the banner pinned under the list is drawn: until the ads are bought away. */
        val showAds: Boolean = false,
    ) : SignsUiState {
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
     * the catalog lists twice - the two "Značky o (dávaní) prednosti v jazde" one per kind are
     * told apart by the kind - folds as the reader sees it.
     */
    data class ToggleGroup(val kind: String, val group: String) : SignsUiEvent

    /** Folds every group away, or opens them all when anything is folded. */
    data object ToggleAllGroups : SignsUiEvent
}

class SignsViewModel(private val catalogs: SignCatalogRepository, exams: ExamRepository, ads: AdRepository) :
    ViewModel() {

    private val query = MutableStateFlow("")

    /**
     * The road signs of the country whose exam the driver is taking. A country with no catalog
     * leaves the screen loading: the Learn hub does not offer its signs at all.
     */
    private val catalog = exams.session.filterNotNull()
        .map { it.bank.country }
        .distinctUntilChanged()
        .map { catalogs.forCountry(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The headings the reader has folded, which a search clears: its results open every group. */
    private val folded = MutableStateFlow(emptySet<GroupKey>())

    val uiState: StateFlow<SignsUiState> = combine(catalog, query, folded, ads.adsAllowed) {
            catalog,
            query,
            folded,
            showAds,
        ->
        if (catalog == null) {
            SignsUiState.Loading
        } else {
            SignsUiState.Ready(
                groups = groupsOf(catalog, query).map { it.toItem(catalog, folded) },
                query = query,
                total = catalog.signs.size,
                showAds = showAds,
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

    /** The groups of [catalog] that a search for [query] leaves. */
    private fun groupsOf(catalog: SignCatalog, query: String) = groupSigns(searchSigns(catalog.signs, query))

    /** Every group the list is showing now, which is what the bar's action folds away. */
    private fun everyGroup(): Set<GroupKey> {
        val catalog = catalog.value ?: return emptySet()
        return groupsOf(catalog, query.value).map { GroupKey(it.kind, it.group) }.toSet()
    }

    private fun SignGroup.toItem(catalog: SignCatalog, folded: Set<GroupKey>) = SignGroupItem(
        kind = kind,
        group = group,
        signs = signs.map { it.toItem(catalog) },
        folded = GroupKey(kind, group) in folded,
    )

    private fun Sign.toItem(catalog: SignCatalog) = SignItem(
        number = number,
        name = name,
        description = description,
        imageUri = catalogs.imageUri(catalog, this),
    )
}

/**
 * Names one heading of the list: the kind of sign it belongs to and the heading itself. The kind is
 * part of it because the catalog has headings that read almost the same, one per kind.
 */
private data class GroupKey(val kind: String, val group: String)

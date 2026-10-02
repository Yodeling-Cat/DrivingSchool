package com.spiraclestudios.autoskola.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.spiraclestudios.autoskola.appVersion
import com.spiraclestudios.autoskola.data.banks.readOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import najautoskola.app.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Where the AboutLibraries Gradle plugin exported what the app is built from. */
internal const val LIBRARIES_RESOURCE = "files/aboutlibraries.json"

data class AboutUiState(
    /** What the platform says this build is; `null` where it says nothing, as the web build does. */
    val version: String?,
    val query: String,
    /** Every library the app is built from, before the search has left any of them out. */
    val all: List<Library>,
    /** Until the generated list has been read there is nothing to show, not an empty list. */
    val loading: Boolean,
) {
    /** The libraries the search is showing - all of them until something is searched for. */
    val shown: List<Library> get() = librariesMatching(all, query)

    /** A search that found nothing, which is not the same as a build that carries no libraries. */
    val nothingFound: Boolean get() = !loading && query.isNotBlank() && shown.isEmpty()
}

sealed interface AboutUiEvent {
    data class Search(val query: String) : AboutUiEvent
}

/**
 * The About screen: which build this is, and the open source libraries it is built from.
 *
 * [readLibraries] is what reads them, so a test can hand the screen a list of its own instead of
 * the bundled one. It also keeps the parsing of that document out of here: on Android the library
 * parses it with `org.json`, which a host test has no implementation of, and a ViewModel whose
 * constructor cannot run under test is one whose decisions cannot be tested at all.
 */
class AboutViewModel(private val readLibraries: suspend () -> List<Library> = ::bundledLibraries) : ViewModel() {

    /** Read once: it is the same build for as long as the screen is up. */
    private val version = appVersion()

    private val query = MutableStateFlow("")

    /** `null` until the list has been read, which is what shows the screen as loading. */
    private val loaded = MutableStateFlow<List<Library>?>(null)

    val uiState: StateFlow<AboutUiState> = combine(query, loaded) { query, libraries ->
        AboutUiState(
            version = version,
            query = query,
            all = libraries.orEmpty(),
            loading = libraries == null,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AboutUiState(version = version, query = "", all = emptyList(), loading = true),
    )

    init {
        viewModelScope.launch { loaded.value = read() }
    }

    fun onEvent(event: AboutUiEvent) {
        when (event) {
            is AboutUiEvent.Search -> query.value = event.query
        }
    }

    /**
     * A list the app could not read is a build that carries none rather than a crash, which is what
     * its file being missing means. Cancellation is not a failure and [readOrNull] rethrows it.
     */
    private suspend fun read(): List<Library> = readOrNull { readLibraries() }.orEmpty()
}

/** Reads the list the Gradle plugin exported into the app's own resources. */
@OptIn(ExperimentalResourceApi::class)
private suspend fun bundledLibraries(): List<Library> {
    val json = Res.readBytes(LIBRARIES_RESOURCE).decodeToString()
    return withContext(Dispatchers.Default) { Libs.Builder().withJson(json).build().libraries }
}

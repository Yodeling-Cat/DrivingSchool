package com.spiraclestudios.autoskola.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.variant.LibrariesDensity
import com.mikepenz.aboutlibraries.ui.compose.variant.LibrariesVariant
import com.mikepenz.aboutlibraries.ui.compose.variant.LibraryActionMode
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.practice.IconArrowBack
import com.spiraclestudios.autoskola.ui.AppPreview
import com.spiraclestudios.autoskola.ui.ContentWidth
import com.spiraclestudios.autoskola.ui.LocalNavBarInset
import com.spiraclestudios.autoskola.ui.NajSearchField
import com.spiraclestudios.autoskola.ui.PreviewBoard
import com.spiraclestudios.autoskola.ui.PreviewLabel
import com.spiraclestudios.autoskola.ui.RoundButton
import com.spiraclestudios.autoskola.ui.previewLibraries
import com.spiraclestudios.autoskola.ui.theme.Naj
import com.spiraclestudios.autoskola.ui.theme.paperBackground
import org.koin.compose.viewmodel.koinViewModel

/** Connects [AboutScreen] to its [AboutViewModel]. */
@Composable
fun AboutRoute(onBack: () -> Unit, viewModel: AboutViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AboutScreen(state, viewModel::onEvent, onBack)
}

/**
 * What this app is: which build it is, that it is not a ministry's own, where its questions come
 * from, and every open source project it is built out of.
 *
 * The list of libraries is AboutLibraries' own container in its dense "refined" shape, with each
 * library's source, website and licence offered as chips. The app hands it the libraries and the
 * words and colours around them, and draws none of it itself.
 */
@Composable
fun AboutScreen(state: AboutUiState, onEvent: (AboutUiEvent) -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().paperBackground()) {
        Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = ContentWidth.Reading).fillMaxWidth()) {
                AboutHeader(state, onBack)
                NajSearchField(
                    query = state.query,
                    placeholder = strings.aboutLibrariesSearch,
                    onQueryChange = { onEvent(AboutUiEvent.Search(it)) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(8.dp))
                when {
                    // The list is a file in the app, so this is a blink rather than a wait - but a
                    // blink of an empty list would read as an app built from nothing.
                    state.loading -> AboutLoading(Modifier.weight(1f))

                    state.nothingFound -> AboutNothingFound(Modifier.weight(1f))

                    else -> LibraryList(state.shown, Modifier.weight(1f))
                }
            }
        }
    }
}

/** Back, the screen's name, and the build the user is running where the platform names one. */
@Composable
private fun AboutHeader(state: AboutUiState, onBack: () -> Unit) {
    val colors = Naj.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RoundButton(icon = IconArrowBack, contentDescription = strings.back, onClick = onBack)
        Column(Modifier.weight(1f)) {
            Text(strings.aboutTitle, style = Naj.type.screenTitle, color = colors.ink)
            state.version?.let { version ->
                Text(strings.aboutVersion(version), style = Naj.type.secondary, color = colors.muted)
            }
        }
    }
}

/**
 * The projects the app is built from, one row each, with their licence and where to read them.
 *
 * The words above them go into the list's own header, so they scroll away with the rows instead of
 * taking a phone's height away from a list that is long by nature.
 */
@Composable
private fun LibraryList(libraries: List<Library>, modifier: Modifier = Modifier) {
    LibrariesContainer(
        libraries = Libs(libraries = libraries, licenses = emptySet()),
        modifier = modifier.fillMaxWidth(),
        variant = LibrariesVariant.Refined,
        density = LibrariesDensity.Cozy,
        actionMode = LibraryActionMode.Chips,
        contentPadding = PaddingValues(bottom = 24.dp + LocalNavBarInset.current),
        header = { item { AboutIntro() } },
    )
}

/**
 * What the app is, and what it is built from: the words the library list sits under, each block
 * opened by the same heading the design gives a group of content.
 */
@Composable
private fun AboutIntro(modifier: Modifier = Modifier) {
    val colors = Naj.colors
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(strings.aboutApp, style = Naj.type.sectionHeading, color = colors.ink)
        Spacer(Modifier.height(10.dp))
        Text(strings.disclaimerIndependent, style = Naj.type.body, color = colors.muted)
        Spacer(Modifier.height(12.dp))
        Text(strings.disclaimerSource, style = Naj.type.body, color = colors.muted)
        Spacer(Modifier.height(22.dp))
        Text(strings.aboutLibraries, style = Naj.type.sectionHeading, color = colors.ink)
        Spacer(Modifier.height(8.dp))
        Text(strings.aboutLibrariesIntro, style = Naj.type.rowSubtitle, color = colors.muted)
    }
}

/** Nothing is listed until the generated file has been read. */
@Composable
private fun AboutLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Naj.colors.primaryDeep)
    }
}

/** A search that found nothing, which is not the same as a build that carries no libraries. */
@Composable
private fun AboutNothingFound(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.TopCenter) {
        Text(strings.aboutLibrariesNothingFound, style = Naj.type.body, color = Naj.colors.muted)
    }
}

// ── Previews ──

/** A state of the screen with the libraries a preview can hold, and the search as it opens. */
private fun previewState(query: String = "", version: String? = "1.0", loading: Boolean = false): AboutUiState =
    AboutUiState(
        version = version,
        query = query,
        all = previewLibraries.libraries,
        loading = loading,
    )

/** The screen on a phone: the open source libraries, in English. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun AboutScreenPreview() {
    AppPreview { AboutScreen(previewState(), onEvent = {}, onBack = {}) }
}

/** The same screen on a desktop browser, where the column is capped rather than stretched. */
@Preview(widthDp = 1100, heightDp = 900)
@Composable
private fun AboutScreenWidePreview() {
    AppPreview { AboutScreen(previewState(), onEvent = {}, onBack = {}) }
}

/** The longest of the four translations, where the search and the rows are wordiest. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun AboutScreenSlovakPreview() {
    AppPreview(language = AppLanguage.Slovak) { AboutScreen(previewState(), onEvent = {}, onBack = {}) }
}

/** The web build, which has no version of its own: the line goes rather than reading "Version ". */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun AboutScreenWithoutVersionPreview() {
    AppPreview { AboutScreen(previewState(version = null), onEvent = {}, onBack = {}) }
}

/** A search that matches nothing, which says so instead of showing an empty list. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun AboutScreenNothingFoundPreview() {
    AppPreview { AboutScreen(previewState(query = "zzz"), onEvent = {}, onBack = {}) }
}

/** The blink before the generated file has been read. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun AboutScreenLoadingPreview() {
    AppPreview { AboutScreen(previewState(loading = true), onEvent = {}, onBack = {}) }
}

/** The words the list sits under, on their own. */
@Preview(widthDp = 400, heightDp = 320)
@Composable
private fun AboutIntroPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("WHAT THE APP IS, AND WHAT IT IS BUILT FROM")
            AboutIntro()
        }
    }
}

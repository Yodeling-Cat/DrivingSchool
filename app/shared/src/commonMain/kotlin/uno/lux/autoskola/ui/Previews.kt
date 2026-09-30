package uno.lux.autoskola.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uno.lux.autoskola.i18n.AppLanguage
import uno.lux.autoskola.i18n.LocalStrings
import uno.lux.autoskola.ui.theme.Naj
import uno.lux.autoskola.ui.theme.NajAutoskolaTheme
import uno.lux.autoskola.ui.theme.paperBackground

/**
 * What every `@Preview` in the app is wrapped in, so a preview is drawn the way the app draws the
 * screen: the theme, one language's strings, and the window size classes that come off the
 * measured window (so the preview's own `widthDp`/`heightDp` pick the layout).
 *
 * A preview never goes through Koin or a ViewModel: it hands the stateless `XScreen` a state built
 * from the fixtures in `PreviewData.kt` and ignores the events it reports.
 */
@Composable
internal fun AppPreview(language: AppLanguage = AppLanguage.English, content: @Composable () -> Unit) {
    NajAutoskolaTheme {
        CompositionLocalProvider(LocalStrings provides language.strings) {
            ProvideWindowSizeClasses { content() }
        }
    }
}

/**
 * The board a component preview sits on: the app's paper, a padded column with a gap between its
 * parts, scrolling when there are more of them than fit. Screens are previewed on their own
 * instead (they bring their own page).
 */
@Composable
internal fun PreviewBoard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxSize().paperBackground()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

/** One line of a component preview, with the components laid out side by side as they are drawn. */
@Composable
internal fun PreviewRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Names one variant of a component in a preview, the way a section label names a group. */
@Composable
internal fun PreviewLabel(text: String) {
    Text(text, style = Naj.type.capsLabel, color = Naj.colors.muted)
}

package uno.lux.autoskola.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uno.lux.autoskola.i18n.strings
import uno.lux.autoskola.ui.theme.Naj
import uno.lux.autoskola.ui.theme.paperBackground

/** Adds one item that spans the whole row, for a header inside a lazy grid. */
internal fun LazyGridScope.fullWidthItem(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

// ── Previews ──

/** A header that spans both columns, over the tiles the grid lays out two at a time. */
@Preview(widthDp = 400, heightDp = 400)
@Composable
private fun FullWidthItemPreview() {
    AppPreview {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().paperBackground(),
            contentPadding = PaddingValues(PagePadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            fullWidthItem {
                Text(strings.allTests, style = Naj.type.sectionHeading, color = Naj.colors.ink)
            }
            items(4) { index ->
                Box(
                    Modifier
                        .height(76.dp)
                        .clip(RoundedCornerShape(TileRadius))
                        .background(Naj.colors.card)
                        .padding(16.dp),
                ) {
                    Text("${index + 1}", style = Naj.type.tileNumber, color = Naj.colors.ink)
                }
            }
        }
    }
}

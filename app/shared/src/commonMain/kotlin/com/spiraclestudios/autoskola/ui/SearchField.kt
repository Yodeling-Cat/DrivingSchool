package com.spiraclestudios.autoskola.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.ui.theme.Naj

/** How tall a search field is, and how round its corners are, wherever one is drawn. */
private val SearchFieldHeight = 48.dp
private val SearchFieldRadius = 16.dp

/**
 * The bar that searches a list: a card-coloured field with a magnifier, the [placeholder] while
 * nothing is typed, and the action that clears it once something is. The query is the caller's, so
 * the screen that searches its content owns the state and the field stays a field.
 */
@Composable
fun NajSearchField(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Naj.colors
    val shape = RoundedCornerShape(SearchFieldRadius)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SearchFieldHeight)
            .clip(shape)
            .background(colors.card)
            .border(BorderStroke(1.5.dp, colors.emptyOutline), shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(20.dp),
        )
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(placeholder, style = Naj.type.option, color = colors.muted)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = Naj.type.option.copy(color = colors.ink),
                cursorBrush = SolidColor(colors.primaryDeep),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onQueryChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = strings.clearSearch,
                    tint = colors.muted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ── Previews ──

/** The field as it opens, and after something has been typed into it. */
@Preview(widthDp = 400, heightDp = 190)
@Composable
private fun NajSearchFieldPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("EMPTY")
            NajSearchField(query = "", placeholder = strings.signsSearchPlaceholder, onQueryChange = {})
            PreviewLabel("WITH A QUERY")
            NajSearchField(query = "zebra", placeholder = strings.signsSearchPlaceholder, onQueryChange = {})
        }
    }
}

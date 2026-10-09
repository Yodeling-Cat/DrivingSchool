package com.spiraclestudios.autoskola.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.ui.theme.Naj

/**
 * The header of a screen that was opened from another one: back, the screen's [title], an optional
 * [subtitle] under it, and an optional [trailing] action at the end of the row.
 */
@Composable
fun BackHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = Naj.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RoundButton(icon = IconArrowBack, contentDescription = strings.back, onClick = onBack)
        Column(Modifier.weight(1f)) {
            Text(title, style = Naj.type.screenTitle, color = colors.ink)
            if (subtitle != null) {
                Text(subtitle, style = Naj.type.secondary, color = colors.muted)
            }
        }
        trailing?.invoke()
    }
}

// ── Previews ──

@Preview(widthDp = 400, heightDp = 320)
@Composable
private fun BackHeaderPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("TITLE ONLY")
            BackHeader(title = strings.feedbackTitle, onBack = {})
            PreviewLabel("WITH A SUBTITLE")
            BackHeader(title = strings.aboutTitle, onBack = {}, subtitle = strings.aboutVersion("1.4.0"))
            PreviewLabel("WITH A TRAILING ACTION")
            BackHeader(
                title = strings.learnRoadSigns,
                onBack = {},
                subtitle = strings.signsCount(120),
                trailing = {
                    RoundButton(Icons.Filled.UnfoldLess, strings.signsCollapseAll, onClick = {})
                },
            )
        }
    }
}

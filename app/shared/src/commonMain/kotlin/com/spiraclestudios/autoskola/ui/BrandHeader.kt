package com.spiraclestudios.autoskola.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.ui.theme.AccentedText
import com.spiraclestudios.autoskola.ui.theme.Naj
import com.spiraclestudios.autoskola.ui.theme.brandParts

/** The "Naj *Autoškola*" wordmark, with the second word in the coral italic the design gives it. */
@Composable
fun BrandTitle(modifier: Modifier = Modifier) {
    AccentedText(brandParts(strings.appName), style = Naj.type.brand, modifier = modifier)
}

/**
 * The header line shared by the phone screens: the wordmark, then [trailing] at the right edge.
 *
 * The wordmark is measured first and keeps the width it needs, and [trailing] gets what is left of
 * the row: an exam named in a language that spells the category out is shortened in the middle
 * rather than squeezing "Naj Autoškola" onto a second line. See [PickerPill].
 */
@Composable
fun ScreenHeader(trailing: @Composable (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BrandTitle()
        if (trailing != null) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { trailing() }
        }
    }
}

// ── Previews ──

@Preview(widthDp = 400, heightDp = 300)
@Composable
private fun ScreenHeaderPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("WORDMARK")
            BrandTitle()
            PreviewLabel("HEADER WITH A TRAILING PILL")
            ScreenHeader(trailing = { PickerPill(text = "Auto · SK", contentDescription = "Change", onClick = {}) })
            PreviewLabel("AN EXAM WHOSE NAME DOES NOT FIT: THE WORDMARK KEEPS ITS LINE")
            ScreenHeader(
                trailing = {
                    PickerPill(
                        text = "Tehergépkocsi pótkocsival (C+E) · HU",
                        contentDescription = "Change",
                        onClick = {},
                    )
                },
            )
            PreviewLabel("AN EMPTY HEADER")
            ScreenHeader()
        }
    }
}

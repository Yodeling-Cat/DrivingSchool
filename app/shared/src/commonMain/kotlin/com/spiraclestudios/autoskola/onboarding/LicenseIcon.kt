package com.spiraclestudios.autoskola.onboarding

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.ui.AppPreview
import com.spiraclestudios.autoskola.ui.IconTile
import com.spiraclestudios.autoskola.ui.PreviewBoard
import com.spiraclestudios.autoskola.ui.PreviewLabel
import com.spiraclestudios.autoskola.ui.PreviewRow

/** The proportions the license icon is drawn at inside its tile. */
private const val ICON_RATIO = 26f / 46f

/**
 * A license category as it is shown wherever one is listed: its own icon on a wash of its own
 * color. The wizard's license step and the exam row in settings both draw it, so they can't drift
 * apart.
 */
@Composable
fun LicenseIcon(category: LicenseCategory, modifier: Modifier = Modifier, size: Dp = 46.dp) {
    IconTile(
        modifier = modifier,
        size = size,
        background = Brush.linearGradient(listOf(category.tint.copy(alpha = 0.16f), category.tint.copy(alpha = 0.16f))),
    ) {
        Icon(
            category.icon,
            contentDescription = null,
            tint = category.tint,
            modifier = Modifier.size(size * ICON_RATIO),
        )
    }
}

// ── Previews ──

/** Every category, so a new one can be checked for an icon and a color that reads on paper. */
@Preview(widthDp = 360, heightDp = 260)
@Composable
private fun LicenseIconPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("EVERY CATEGORY")
            LicenseCategory.entries.chunked(4).forEach { row ->
                PreviewRow { row.forEach { LicenseIcon(it) } }
            }
            PreviewLabel("THE SAME TILE, LARGER")
            PreviewRow {
                LicenseIcon(LicenseCategory.B, size = 64.dp)
                LicenseIcon(LicenseCategory.D, size = 64.dp)
            }
        }
    }
}

package uno.lux.autoskola.onboarding

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uno.lux.autoskola.bank.LicenseCategory
import uno.lux.autoskola.ui.AppPreview
import uno.lux.autoskola.ui.IconTile
import uno.lux.autoskola.ui.PreviewBoard
import uno.lux.autoskola.ui.PreviewLabel
import uno.lux.autoskola.ui.PreviewRow

/** The proportions the licence icon is drawn at inside its tile. */
private const val ICON_RATIO = 26f / 46f

/**
 * A licence category as it is shown wherever one is listed: its own icon on a wash of its own
 * colour. The wizard's licence step and the exam row in settings both draw it, so they can't drift
 * apart.
 */
@Composable
fun LicenceIcon(category: LicenseCategory, modifier: Modifier = Modifier, size: Dp = 46.dp) {
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

/** Every category, so a new one can be checked for an icon and a colour that reads on paper. */
@Preview(widthDp = 360, heightDp = 260)
@Composable
private fun LicenceIconPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("EVERY CATEGORY")
            LicenseCategory.entries.chunked(4).forEach { row ->
                PreviewRow { row.forEach { LicenceIcon(it) } }
            }
            PreviewLabel("THE SAME TILE, LARGER")
            PreviewRow {
                LicenceIcon(LicenseCategory.B, size = 64.dp)
                LicenceIcon(LicenseCategory.D, size = 64.dp)
            }
        }
    }
}

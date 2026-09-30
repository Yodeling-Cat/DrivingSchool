package uno.lux.autoskola.onboarding

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uno.lux.autoskola.bank.LicenseCategory
import uno.lux.autoskola.ui.IconTile

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

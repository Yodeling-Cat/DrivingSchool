package uno.lux.autoskola.learn

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uno.lux.autoskola.ui.AppPreview
import uno.lux.autoskola.ui.PreviewBoard
import uno.lux.autoskola.ui.PreviewLabel
import uno.lux.autoskola.ui.PreviewRow
import uno.lux.autoskola.ui.theme.Naj

/**
 * Two roads crossing, for the learning material about intersections. There is no Material icon for
 * a junction: the filled set offers a pin and a plus, and neither says "crossroads".
 */
internal val IconCrossroads: ImageVector = ImageVector.Builder(
    name = "Crossroads",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(10f, 2f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(8f)
        horizontalLineToRelative(8f)
        verticalLineToRelative(4f)
        horizontalLineToRelative(-8f)
        verticalLineToRelative(8f)
        horizontalLineToRelative(-4f)
        verticalLineToRelative(-8f)
        horizontalLineToRelative(-8f)
        verticalLineToRelative(-4f)
        horizontalLineToRelative(8f)
        close()
    }
}.build()

// ── Previews ──

/** The learning material's own icon, on the tile the hub draws it in. */
@Preview(widthDp = 300, heightDp = 120)
@Composable
private fun LearnIconsPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("CROSSROADS")
            PreviewRow {
                Icon(IconCrossroads, contentDescription = null, tint = Naj.colors.ink, modifier = Modifier.size(24.dp))
                Icon(
                    IconCrossroads,
                    contentDescription = null,
                    tint = Naj.colors.primaryDeep,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

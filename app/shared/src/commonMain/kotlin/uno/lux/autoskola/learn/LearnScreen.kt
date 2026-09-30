package uno.lux.autoskola.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import uno.lux.autoskola.i18n.strings
import uno.lux.autoskola.ui.ContentWidth
import uno.lux.autoskola.ui.LocalNavBarInset
import uno.lux.autoskola.ui.theme.Naj
import uno.lux.autoskola.ui.theme.paperBackground

/**
 * Learning material. Empty for now: the navigation entry and the screen exist so the tab is where
 * the design puts it, and the content lands here later.
 */
@Composable
fun LearnScreen() {
    val colors = Naj.colors
    Box(Modifier.fillMaxSize().paperBackground()) {
        Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = ContentWidth.Narrow)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 52.dp, bottom = 24.dp + LocalNavBarInset.current),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(strings.learnTitle, style = Naj.type.screenTitle, color = colors.ink)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.card.copy(alpha = 0.45f))
                        .border(1.5.dp, colors.emptyOutline, RoundedCornerShape(22.dp))
                        .padding(28.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.mintGlow),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Book,
                                contentDescription = null,
                                tint = colors.primaryDeep,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Text(strings.learnPlaceholder, style = Naj.type.cardTitle, color = colors.muted)
                    }
                }
            }
        }
    }
}

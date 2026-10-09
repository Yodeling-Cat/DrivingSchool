package com.spiraclestudios.autoskola.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.ui.AppPreview
import com.spiraclestudios.autoskola.ui.ContentWidth
import com.spiraclestudios.autoskola.ui.IconArrowBack
import com.spiraclestudios.autoskola.ui.IconTile
import com.spiraclestudios.autoskola.ui.RoundButton
import com.spiraclestudios.autoskola.ui.theme.Naj
import com.spiraclestudios.autoskola.ui.theme.paperBackground

/**
 * A subject of the learning material that has no content yet: the screen exists so the hub's choice
 * leads somewhere, and says so instead of showing an empty page.
 *
 * The screen is not previewed through a ViewModel and holds no state: when a subject is written,
 * its own screen replaces this one.
 */
@Composable
fun LearnPlaceholderScreen(section: LearnSection, onBack: () -> Unit) {
    val colors = Naj.colors
    Box(Modifier.fillMaxSize().paperBackground()) {
        Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = ContentWidth.Narrow)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    RoundButton(icon = IconArrowBack, contentDescription = strings.back, onClick = onBack)
                    Text(section.title(strings), style = Naj.type.screenTitle, color = colors.ink)
                }
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
                        IconTile(
                            size = 46.dp,
                            background = SolidColor(colors.mintGlow),
                        ) {
                            Icon(
                                section.icon,
                                contentDescription = null,
                                tint = colors.primaryDeep,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Column {
                            Text(section.title(strings), style = Naj.type.cardTitle, color = colors.ink)
                            Spacer(Modifier.height(3.dp))
                            Text(strings.learnComingSoon, style = Naj.type.rowSubtitle, color = colors.muted)
                        }
                    }
                }
            }
        }
    }
}

// ── Previews ──

/** The intersection material, which is not written yet. */
@Preview(widthDp = 400, heightDp = 600)
@Composable
private fun LearnPlaceholderScreenPreview() {
    AppPreview { LearnPlaceholderScreen(LearnSection.Intersections, onBack = {}) }
}

/** The same screen in Slovak, whose subject name is longer than the English one. */
@Preview(widthDp = 400, heightDp = 600)
@Composable
private fun LearnPlaceholderScreenSlovakPreview() {
    AppPreview(language = AppLanguage.Slovak) { LearnPlaceholderScreen(LearnSection.Intersections, onBack = {}) }
}

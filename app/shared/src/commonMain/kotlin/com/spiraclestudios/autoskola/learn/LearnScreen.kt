package com.spiraclestudios.autoskola.learn

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.i18n.Strings
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.ui.AppPreview
import com.spiraclestudios.autoskola.ui.ContentWidth
import com.spiraclestudios.autoskola.ui.IconTile
import com.spiraclestudios.autoskola.ui.LocalNavBarInset
import com.spiraclestudios.autoskola.ui.NajCard
import com.spiraclestudios.autoskola.ui.theme.Naj
import com.spiraclestudios.autoskola.ui.theme.paperBackground

/** The subjects the learning material is made of, in the order the hub offers them. */
enum class LearnSection(val icon: ImageVector, val title: (Strings) -> String, val subtitle: (Strings) -> String) {
    RoadSigns(Icons.Filled.Warning, { it.learnRoadSigns }, { it.learnRoadSignsSubtitle }),
    Intersections(IconCrossroads, { it.learnIntersections }, { it.learnIntersectionsSubtitle }),
    Legislation(Icons.AutoMirrored.Filled.List, { it.learnLegislation }, { it.learnLegislationSubtitle }),
}

/**
 * Learning material: the subjects the app teaches, each opening its own screen. The screen decides
 * nothing itself — it reports which subject was picked, and the navigation callback opens it.
 *
 * The hub scrolls: the cards are taller than a phone held sideways, where the navigation bar
 * takes its share of the little height there is.
 */
@Composable
fun LearnScreen(onOpenSection: (LearnSection) -> Unit) {
    val colors = Naj.colors
    Box(Modifier.fillMaxSize().paperBackground()) {
        Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = ContentWidth.Narrow)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 52.dp, bottom = 24.dp + LocalNavBarInset.current),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(strings.learnTitle, style = Naj.type.screenTitle, color = colors.ink)
                Spacer(Modifier.height(4.dp))
                LearnSection.entries
                    // Legislation isn't ready yet: drop this filter to bring its card back.
                    .filter { it != LearnSection.Legislation }
                    .forEach { section ->
                        LearnSectionCard(section, onClick = { onOpenSection(section) })
                    }
            }
        }
    }
}

/** One subject: what it teaches, and the mark that stands for it. */
@Composable
private fun LearnSectionCard(section: LearnSection, onClick: () -> Unit) {
    val colors = Naj.colors
    NajCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconTile(size = 46.dp, background = SolidColor(colors.mintGlow)) {
                Icon(
                    section.icon,
                    contentDescription = null,
                    tint = colors.primaryDeep,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(section.title(strings), style = Naj.type.rowTitle, color = colors.ink)
                Spacer(Modifier.height(3.dp))
                Text(section.subtitle(strings), style = Naj.type.rowSubtitle, color = colors.muted)
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.primaryDeep,
            )
        }
    }
}

// ── Previews ──

/** The hub on a phone. */
@Preview(widthDp = 400, heightDp = 700)
@Composable
private fun LearnScreenPreview() {
    AppPreview { LearnScreen(onOpenSection = {}) }
}

/** The hub on a tablet: the column is capped rather than stretched across the screen. */
@Preview(widthDp = 900, heightDp = 600)
@Composable
private fun LearnScreenWidePreview() {
    AppPreview { LearnScreen(onOpenSection = {}) }
}

/** The subjects in Slovak, whose names are longer than the English ones. */
@Preview(widthDp = 400, heightDp = 700)
@Composable
private fun LearnScreenSlovakPreview() {
    AppPreview(language = AppLanguage.Slovak) { LearnScreen(onOpenSection = {}) }
}

/**
 * A phone held sideways, which is the case the hub scrolls for: the cards are taller than the
 * height there is, so the last one is reached by scrolling rather than not at all.
 */
@Preview(widthDp = 800, heightDp = 420)
@Composable
private fun LearnScreenLandscapePreview() {
    AppPreview { LearnScreen(onOpenSection = {}) }
}

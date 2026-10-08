package com.spiraclestudios.autoskola.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.spiraclestudios.autoskola.i18n.Strings
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.ui.theme.Naj
import com.spiraclestudios.autoskola.ui.theme.paperBackground
import najautoskola.app.shared.generated.resources.Res
import najautoskola.app.shared.generated.resources.rail_mark
import org.jetbrains.compose.resources.painterResource

/** The top-level destinations of the app bar. */
enum class NavTab(val icon: ImageVector, val label: (Strings) -> String) {
    Tests(Icons.Filled.Quiz, { it.navTests }),
    Learn(Icons.Filled.Book, { it.navLearn }),
    Settings(Icons.Filled.Tune, { it.navSettings }),
}

/**
 * The room the floating pill takes at the bottom of a compact screen: its height, the space it
 * floats in, and the home indicator's inset. A screen adds this to its own bottom padding so
 * nothing ends up permanently under the pill.
 */
val LocalNavBarInset = staticCompositionLocalOf { 0.dp }

/** How far the pill floats above the bottom of the screen. */
private val NavPillBottomGap = 20.dp

/** The pill, the room it floats in, and a gap: how much of a compact screen's height it takes. */
private val NavPillRoom = 72.dp + NavPillBottomGap + 16.dp

private val RailWidth = 88.dp

/** A rail item: the tile that holds the icon, and the rounded mask its press ripple is drawn in. */
private val RailTileSize = 48.dp
private val RailItemRadius = 16.dp

/** The app's mark at the top of the rail: a little narrower than the tiles below it. */
private val RailMarkWidth = RailTileSize * 0.9f
private val PillItemWidth = 100.dp
private val PillItemHeight = 56.dp

/**
 * Bottom navigation on phones, navigation rail on tablets and desktop.
 *
 * Phones get the floating navy pill with a yellow pill behind the active tab; wider screens get
 * the navy rail with the same yellow active state, as the design system asks.
 */
@Composable
fun NavigationShell(selected: NavTab, onSelect: (NavTab) -> Unit, content: @Composable () -> Unit) {
    if (LocalWidthClass.current == WidthClass.Compact) {
        // The pill floats over the page rather than sitting in a bar below it, so the screen's own
        // background - glows and grain included - runs behind it and all the way to the bottom.
        // Screens keep their last content clear of it with [LocalNavBarInset].
        Box(Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalNavBarInset provides NavPillRoom) { content() }
            NavPill(selected, onSelect, Modifier.align(Alignment.BottomCenter))
        }
    } else {
        Row(Modifier.fillMaxSize()) {
            NavRail(selected, onSelect)
            Box(Modifier.weight(1f)) { content() }
        }
    }
}

@Composable
private fun NavPill(selected: NavTab, onSelect: (NavTab) -> Unit, modifier: Modifier = Modifier) {
    val colors = Naj.colors
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .padding(start = 24.dp, end = 24.dp, bottom = NavPillBottomGap),
        contentAlignment = Alignment.Center,
    ) {
        // As wide as its items rather than as wide as the screen: stretched across a wide phone or
        // a tablet the bar is mostly empty navy either side of the tabs.
        NavyPill(arrangement = Arrangement.Center) {
            NavTab.entries.forEach { tab ->
                val active = tab == selected
                Column(
                    modifier = Modifier
                        .width(PillItemWidth)
                        .height(PillItemHeight)
                        .clip(RoundedCornerShape(PillItemHeight / 2))
                        .background(if (active) colors.roadYellow else Color.Transparent)
                        .clickable { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = null,
                        tint = if (active) colors.ink else Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        tab.label(strings),
                        style = if (active) Naj.type.navLabelActive else Naj.type.navLabel,
                        color = if (active) colors.ink else Color.White.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }
}

/**
 * One flat navy, not the design's navyLight-to-navy gradient: over the phone's 72dp pill that
 * gradient passes unnoticed, but stretched down a full-height rail it reads as a light leak over
 * the bar rather than as the bar's own color.
 */
@Composable
private fun NavRail(selected: NavTab, onSelect: (NavTab) -> Unit) {
    val colors = Naj.colors
    val active = Color.White.copy(alpha = 0.7f)
    Column(
        modifier = Modifier
            .width(RailWidth)
            .fillMaxHeight()
            .background(colors.navy)
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Image(
            painterResource(Res.drawable.rail_mark),
            contentDescription = strings.appName,
            modifier = Modifier.width(RailMarkWidth),
        )
        NavTab.entries.forEach { tab ->
            val isActive = tab == selected
            Column(
                modifier = Modifier
                    // The press ripple is masked to the item's own rounded shape; left unclipped it
                    // is drawn as a rectangle around the icon and the label.
                    .clip(RoundedCornerShape(RailItemRadius))
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(RailTileSize)
                        .clip(RoundedCornerShape(RailItemRadius))
                        .background(if (isActive) colors.roadYellow else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = null,
                        tint = if (isActive) colors.ink else active,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    tab.label(strings),
                    style = Naj.type.railLabel,
                    color = if (isActive) colors.roadYellow else active,
                )
            }
        }
    }
}

// ── Previews ──

/** The phone bar. The preview's width is what picks it: under 600dp is [WidthClass.Compact]. */
@Preview(widthDp = 400, heightDp = 700)
@Composable
private fun NavigationPillPreview() {
    AppPreview {
        NavigationShell(selected = NavTab.Tests, onSelect = {}) {
            Box(Modifier.fillMaxSize().paperBackground())
        }
    }
}

/** The tablet and desktop rail, with the second tab active. */
@Preview(widthDp = 1000, heightDp = 700)
@Composable
private fun NavigationRailPreview() {
    AppPreview {
        NavigationShell(selected = NavTab.Learn, onSelect = {}) {
            Box(Modifier.fillMaxSize().paperBackground())
        }
    }
}

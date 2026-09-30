package uno.lux.autoskola.ui

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import uno.lux.autoskola.i18n.Strings
import uno.lux.autoskola.i18n.strings
import uno.lux.autoskola.ui.theme.Naj

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

/** The pill, the room it floats in, and a gap: how much of a compact screen's height it takes. */
private val NavPillRoom = 72.dp + 24.dp + 16.dp

private val RailWidth = 88.dp
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
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
    ) {
        NavyPill(Modifier.fillMaxWidth(), arrangement = Arrangement.SpaceAround) {
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

@Composable
private fun NavRail(selected: NavTab, onSelect: (NavTab) -> Unit) {
    val colors = Naj.colors
    val active = Color.White.copy(alpha = 0.7f)
    Column(
        modifier = Modifier
            .width(RailWidth)
            .fillMaxHeight()
            .background(Brush.verticalGradient(listOf(colors.navyLight, colors.navy)))
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(strings.appName.take(1), style = Naj.type.railMark, color = colors.coral)
        NavTab.entries.forEach { tab ->
            val isActive = tab == selected
            Column(
                modifier = Modifier.clickable { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
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

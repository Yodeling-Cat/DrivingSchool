package uno.lux.autoskola.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import uno.lux.autoskola.home.HomeRoute
import uno.lux.autoskola.i18n.strings
import uno.lux.autoskola.onboarding.ExamSelectionRoute
import uno.lux.autoskola.practice.PracticeRoute
import uno.lux.autoskola.settings.SettingsRoute
import uno.lux.autoskola.ui.LocalWidthClass
import uno.lux.autoskola.ui.WidthClass

private val Accent = Color(0xFF5E35B1)

@Serializable
data object HomeKey : NavKey

@Serializable
data object SettingsKey : NavKey

@Serializable
data object ChangeExamKey : NavKey

@Serializable
data class PracticeKey(val attemptId: String) : NavKey

/** Lets the back stack be saved on every platform, not just Android. */
private val navConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(HomeKey::class, HomeKey.serializer())
            subclass(SettingsKey::class, SettingsKey.serializer())
            subclass(ChangeExamKey::class, ChangeExamKey.serializer())
            subclass(PracticeKey::class, PracticeKey.serializer())
        }
    }
}

private enum class Tab { Tests, Settings }

/** Switching tabs is instant, like the bars themselves; other screens use the default animation. */
private val tabMetadata =
    NavDisplay.transitionSpec { EnterTransition.None togetherWith ExitTransition.None } +
        NavDisplay.popTransitionSpec { EnterTransition.None togetherWith ExitTransition.None }

/**
 * Everything after onboarding. The back stack lives here; each entry gets its own
 * ViewModel store, so a screen's ViewModel is cleared when the screen is popped.
 */
@Composable
fun ExamNavigation() {
    val backStack = rememberNavBackStack(navConfiguration, HomeKey)

    val pop: () -> Unit = { backStack.removeLastOrNull() }
    val selectTab: (Tab) -> Unit = { tab ->
        // Settings sits on top of Tests, so back from Settings returns to Tests.
        while (backStack.size > 1) backStack.removeLastOrNull()
        if (tab == Tab.Settings) backStack.add(SettingsKey)
    }

    NavDisplay(
        backStack = backStack,
        onBack = pop,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<HomeKey>(metadata = tabMetadata) {
                NavigationShell(Tab.Tests, selectTab) {
                    HomeRoute(
                        onOpenAttempt = { backStack.add(PracticeKey(it)) },
                        onChangeExam = { backStack.add(ChangeExamKey) },
                    )
                }
            }
            entry<SettingsKey>(metadata = tabMetadata) {
                NavigationShell(Tab.Settings, selectTab) {
                    SettingsRoute(onChangeExam = { backStack.add(ChangeExamKey) })
                }
            }
            entry<PracticeKey> { key ->
                PracticeRoute(key.attemptId, onClose = { backStack.popIfTop(key) })
            }
            entry<ChangeExamKey> {
                ExamSelectionRoute(onBack = pop, onDone = { backStack.popIfTop(ChangeExamKey) })
            }
        },
    )
}

/** Pops [key] only if it's still on top, so a repeated close can't pop the screen below. */
private fun NavBackStack<NavKey>.popIfTop(key: NavKey) {
    if (lastOrNull() == key) removeLastOrNull()
}

/** Bottom navigation bar on phones, navigation rail on tablets and desktop. */
@Composable
private fun NavigationShell(selected: Tab, onSelect: (Tab) -> Unit, content: @Composable () -> Unit) {
    val destinations = listOf(
        Triple(Tab.Tests, Icons.Outlined.Quiz, strings.navTests),
        Triple(Tab.Settings, Icons.Outlined.Settings, strings.navSettings),
    )
    if (LocalWidthClass.current == WidthClass.Compact) {
        Scaffold(
            containerColor = Color.White,
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    destinations.forEach { (tab, icon, label) ->
                        NavigationBarItem(
                            selected = tab == selected,
                            onClick = { onSelect(tab) },
                            icon = { NavIcon(icon) },
                            label = { Text(label) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding).consumeWindowInsets(padding)) { content() }
        }
    } else {
        Row(Modifier.fillMaxSize().background(Color.White)) {
            NavigationRail(containerColor = Color.White) {
                destinations.forEach { (tab, icon, label) ->
                    NavigationRailItem(
                        selected = tab == selected,
                        onClick = { onSelect(tab) },
                        icon = { NavIcon(icon) },
                        label = { Text(label) },
                    )
                }
            }
            VerticalDivider(color = Color(0xFFEEEEEE))
            Box(Modifier.weight(1f)) { content() }
        }
    }
}

@Composable
private fun NavIcon(icon: ImageVector) {
    Icon(icon, contentDescription = null, tint = Accent)
}

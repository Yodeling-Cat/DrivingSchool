package uno.lux.autoskola.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
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
import uno.lux.autoskola.learn.LearnScreen
import uno.lux.autoskola.onboarding.ExamSelectionRoute
import uno.lux.autoskola.practice.PracticeRoute
import uno.lux.autoskola.results.ResultsRoute
import uno.lux.autoskola.settings.SettingsRoute
import uno.lux.autoskola.ui.NavTab
import uno.lux.autoskola.ui.NavigationShell

@Serializable
data object HomeKey : NavKey

@Serializable
data object LearnKey : NavKey

@Serializable
data object SettingsKey : NavKey

@Serializable
data object ChangeExamKey : NavKey

@Serializable
data class PracticeKey(val attemptId: String) : NavKey

@Serializable
data class ResultsKey(val attemptId: String) : NavKey

/** Lets the back stack be saved on every platform, not just Android. */
private val navConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(HomeKey::class, HomeKey.serializer())
            subclass(LearnKey::class, LearnKey.serializer())
            subclass(SettingsKey::class, SettingsKey.serializer())
            subclass(ChangeExamKey::class, ChangeExamKey.serializer())
            subclass(PracticeKey::class, PracticeKey.serializer())
            subclass(ResultsKey::class, ResultsKey.serializer())
        }
    }
}

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
    val selectTab: (NavTab) -> Unit = { tab ->
        // A tab other than Tests sits on top of it, so back from the tab returns to the tests.
        while (backStack.size > 1) backStack.removeLastOrNull()
        when (tab) {
            NavTab.Tests -> Unit
            NavTab.Learn -> backStack.add(LearnKey)
            NavTab.Settings -> backStack.add(SettingsKey)
        }
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
                NavigationShell(NavTab.Tests, selectTab) {
                    HomeRoute(
                        onOpenAttempt = { backStack.add(PracticeKey(it)) },
                        onChangeExam = { backStack.add(ChangeExamKey) },
                    )
                }
            }
            entry<LearnKey>(metadata = tabMetadata) {
                NavigationShell(NavTab.Learn, selectTab) { LearnScreen() }
            }
            entry<SettingsKey>(metadata = tabMetadata) {
                NavigationShell(NavTab.Settings, selectTab) {
                    SettingsRoute(onChangeExam = { backStack.add(ChangeExamKey) })
                }
            }
            entry<PracticeKey> { key ->
                PracticeRoute(
                    key.attemptId,
                    onClose = { backStack.popIfTop(key) },
                    onOpenResults = { if (backStack.lastOrNull() == key) backStack.add(ResultsKey(key.attemptId)) },
                )
            }
            // Sits on top of its test, so going back from the results reviews the answers.
            entry<ResultsKey> { key ->
                ResultsRoute(
                    key.attemptId,
                    onReview = { backStack.popIfTop(key) },
                    onDone = { while (backStack.size > 1) backStack.removeLastOrNull() },
                )
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

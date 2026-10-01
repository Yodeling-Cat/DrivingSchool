package uno.lux.autoskola.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent.Companion.EDGE_LEFT
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.koinInject
import uno.lux.autoskola.home.HomeRoute
import uno.lux.autoskola.learn.IntersectionsRoute
import uno.lux.autoskola.learn.LearnPlaceholderScreen
import uno.lux.autoskola.learn.LearnScreen
import uno.lux.autoskola.learn.LearnSection
import uno.lux.autoskola.learn.SignsRoute
import uno.lux.autoskola.learn.SituationRoute
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

/** The learning subjects, each its own screen on top of the learn tab. */
@Serializable
data object SignsKey : NavKey

@Serializable
data object IntersectionsKey : NavKey

/** One situation of the intersections material, by its place in it. */
@Serializable
data class SituationKey(val number: Int) : NavKey

@Serializable
data object LegislationKey : NavKey

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
            subclass(SignsKey::class, SignsKey.serializer())
            subclass(IntersectionsKey::class, IntersectionsKey.serializer())
            subclass(SituationKey::class, SituationKey.serializer())
            subclass(LegislationKey::class, LegislationKey.serializer())
            subclass(SettingsKey::class, SettingsKey.serializer())
            subclass(ChangeExamKey::class, ChangeExamKey.serializer())
            subclass(PracticeKey::class, PracticeKey.serializer())
            subclass(ResultsKey::class, ResultsKey.serializer())
        }
    }
}

/** Switching tabs is instant, like the bars themselves; other screens use the slide below. */
private val tabMetadata =
    NavDisplay.transitionSpec { EnterTransition.None togetherWith ExitTransition.None } +
        NavDisplay.popTransitionSpec { EnterTransition.None togetherWith ExitTransition.None }

/** How long a screen takes to come in or leave. */
private const val SCREEN_SLIDE_MILLIS = 300

/**
 * One duration and one easing for every part of the slide, so the pop that replays these values
 * backwards is the push, and the two can't drift apart.
 */
private val ScreenSlideSpec =
    tween<IntOffset>(durationMillis = SCREEN_SLIDE_MILLIS, easing = FastOutSlowInEasing)

/**
 * The slide a screen change plays: the incoming screen starts a whole width away and the outgoing
 * one leaves as far the other way, so neither is partly on screen once it has played. [direction]
 * is +1 going deeper into the app - in from the right, out to the left - and -1 coming back out,
 * which is that same slide with both offsets negated. Popping is therefore the push, backwards,
 * rather than a second animation to keep in step with the first.
 *
 * Navigation 3 fades on Android, slides the iOS way on iOS and does nothing in a browser, so the
 * app states its own slide instead of inheriting a different one per platform.
 */
private fun AnimatedContentTransitionScope<Scene<NavKey>>.screenSlide(direction: Int): ContentTransform =
    slideInHorizontally(animationSpec = ScreenSlideSpec) { direction * it } togetherWith
        slideOutHorizontally(animationSpec = ScreenSlideSpec) { -direction * it }

/**
 * Everything after onboarding. The back stack lives here; each entry gets its own
 * ViewModel store, so a screen's ViewModel is cleared when the screen is popped.
 */
@Composable
fun ExamNavigation() {
    val navigation = koinInject<NavigationState>()
    // Where the app was left, unless rememberSaveable kept a stack of its own (a rotation, or a
    // process the system killed with its state intact).
    val startKeys = remember { backStackOf(navigation.initialRoute()) }
    val backStack = rememberNavBackStack(navConfiguration, *startKeys.toTypedArray())

    // Every screen of the stack is in the route, so what is kept - the URL, or the stored route - is
    // the whole way down, not only the screen on top.
    val route = routeOf(backStack)
    LaunchedEffect(route) { navigation.record(route) }

    // A browser's back and forward buttons move the address bar; the app follows them.
    DisposableEffect(navigation) {
        val stopWatching = navigation.observe { moved -> backStack.navigateTo(backStackOf(moved)) }
        onDispose { stopWatching() }
    }

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
        transitionSpec = { screenSlide(direction = 1) },
        popTransitionSpec = { screenSlide(direction = -1) },
        // A back gesture drags the screen out through the edge it came in from, so which way the
        // slide runs follows the finger.
        predictivePopTransitionSpec = { edge -> screenSlide(direction = if (edge == EDGE_LEFT) -1 else 1) },
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
                NavigationShell(NavTab.Learn, selectTab) {
                    LearnScreen(onOpenSection = { section -> backStack.add(section.key()) })
                }
            }
            entry<SignsKey> { SignsRoute(onBack = pop) }
            entry<IntersectionsKey> {
                IntersectionsRoute(
                    onBack = pop,
                    onOpenSituation = { number -> backStack.add(SituationKey(number)) },
                    onOpenAttempt = { attemptId -> backStack.add(PracticeKey(attemptId)) },
                )
            }
            entry<SituationKey> { key ->
                SituationRoute(key.number, onClose = { backStack.popIfTop(key) })
            }
            entry<LegislationKey> { LearnPlaceholderScreen(LearnSection.Legislation, onBack = pop) }
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

/** Popping [key] only if it's still on top, so a repeated close can't pop the screen below. */
private fun NavBackStack<NavKey>.popIfTop(key: NavKey) {
    if (lastOrNull() == key) removeLastOrNull()
}

/** The screen a learning subject opens. Every subject has one, so the hub never hits a dead end. */
internal fun LearnSection.key(): NavKey = when (this) {
    LearnSection.RoadSigns -> SignsKey
    LearnSection.Intersections -> IntersectionsKey
    LearnSection.Legislation -> LegislationKey
}

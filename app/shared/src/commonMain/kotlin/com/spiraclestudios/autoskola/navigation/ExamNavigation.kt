package com.spiraclestudios.autoskola.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.spiraclestudios.autoskola.about.AboutRoute
import com.spiraclestudios.autoskola.data.analytics.AnalyticsRepository
import com.spiraclestudios.autoskola.feedback.FeedbackRoute
import com.spiraclestudios.autoskola.home.HomeRoute
import com.spiraclestudios.autoskola.learn.IntersectionsRoute
import com.spiraclestudios.autoskola.learn.LearnPlaceholderScreen
import com.spiraclestudios.autoskola.learn.LearnRoute
import com.spiraclestudios.autoskola.learn.LearnSection
import com.spiraclestudios.autoskola.learn.SignsRoute
import com.spiraclestudios.autoskola.learn.SituationRoute
import com.spiraclestudios.autoskola.onboarding.ExamSelectionRoute
import com.spiraclestudios.autoskola.practice.PracticeRoute
import com.spiraclestudios.autoskola.results.ResultsRoute
import com.spiraclestudios.autoskola.settings.SettingsRoute
import com.spiraclestudios.autoskola.ui.NavTab
import com.spiraclestudios.autoskola.ui.NavigationShell
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.koinInject

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
data object AboutKey : NavKey

@Serializable
data object FeedbackKey : NavKey

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
            subclass(AboutKey::class, AboutKey.serializer())
            subclass(FeedbackKey::class, FeedbackKey.serializer())
            subclass(PracticeKey::class, PracticeKey.serializer())
            subclass(ResultsKey::class, ResultsKey.serializer())
        }
    }
}

/** How long switching tabs takes: a quick cross-fade, well under the slide's [SCREEN_SLIDE_MILLIS]. */
private const val TAB_FADE_MILLIS = 200

/**
 * Switching tabs cross-fades; other screens use the slide below. Each tab draws its own bar, so only
 * the screen on top fades - in over the old tab when one is picked, out over the tests when going
 * back to them - and the other stays opaque underneath. Fading both would let the page show through
 * the bar halfway; this way the bar stays put and only its active tab changes.
 *
 * Navigation 3 takes these from whichever screen is on top, so they only play between two tabs:
 * opening or leaving any other screen from a tab still slides.
 */
private val tabMetadata =
    NavDisplay.transitionSpec {
        fadeIn(tween(TAB_FADE_MILLIS)) togetherWith ExitTransition.KeepUntilTransitionsFinished
    } + NavDisplay.popTransitionSpec { EnterTransition.None togetherWith fadeOut(tween(TAB_FADE_MILLIS)) }

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
    val analytics = koinInject<AnalyticsRepository>()
    // Where the app was left, unless rememberSaveable kept a stack of its own (a rotation, or a
    // process the system killed with its state intact).
    val startKeys = remember { backStackOf(navigation.initialRoute()) }
    val backStack = rememberNavBackStack(navConfiguration, *startKeys.toTypedArray())

    // Every screen of the stack is in the route, so what is kept - the URL, or the stored route - is
    // the whole way down, not only the screen on top.
    val route = routeOf(backStack)
    LaunchedEffect(route) {
        navigation.record(route)
        // The screen on top is the one being looked at. Reported here rather than from each screen,
        // because every route change passes through this one effect — so a screen cannot be added
        // to the app and quietly go uncounted. Onboarding is not on this stack and reports itself
        // from `App`.
        analytics.screen(backStack.last().analyticsScreenName())
    }

    // A browser's back and forward buttons move the address bar; the app follows them.
    DisposableEffect(navigation) {
        val stopWatching = navigation.observe { moved -> backStack.navigateTo(backStackOf(moved)) }
        onDispose { stopWatching() }
    }

    val pop: () -> Unit = { backStack.removeLastOrNull() }
    val selectTab: (NavTab) -> Unit = { tab ->
        // A tab other than Tests sits on top of it, so back from the tab returns to the tests.
        backStack.popToRoot()
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
                    LearnRoute(onOpenSection = { section -> backStack.add(section.key()) })
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
                    SettingsRoute(
                        onChangeExam = { backStack.add(ChangeExamKey) },
                        onOpenAbout = { backStack.add(AboutKey) },
                        onOpenFeedback = { backStack.add(FeedbackKey) },
                    )
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
                    onDone = { backStack.popToRoot() },
                    onDoneToList = { backStack.popToSituations() },
                )
            }
            entry<ChangeExamKey> {
                ExamSelectionRoute(onBack = pop, onDone = { backStack.popIfTop(ChangeExamKey) })
            }
            entry<AboutKey> { AboutRoute(onBack = pop) }
            entry<FeedbackKey> { FeedbackRoute(onBack = pop) }
        },
    )
}

/** Popping [key] only if it's still on top, so a repeated close can't pop the screen below. */
private fun NavBackStack<NavKey>.popIfTop(key: NavKey) {
    if (lastOrNull() == key) removeLastOrNull()
}

/** Leaving a test behind for the exams it belongs to: the top-level destination at the bottom. */
private fun NavBackStack<NavKey>.popToRoot() {
    while (size > 1) removeLastOrNull()
}

/**
 * Leaving a situations drill behind for the situations it was begun from: a drill is started from
 * that list, so going back through the practice screen is not what "done" means.
 */
private fun NavBackStack<NavKey>.popToSituations() {
    while (size > 1 && last() !is IntersectionsKey) removeLastOrNull()
}

/** The screen a learning subject opens. Every subject has one, so the hub never hits a dead end. */
internal fun LearnSection.key(): NavKey = when (this) {
    LearnSection.RoadSigns -> SignsKey
    LearnSection.Intersections -> IntersectionsKey
    LearnSection.Legislation -> LegislationKey
}

/**
 * What a screen reports itself as. A `when` over the keys rather than a name on each one, because
 * this is already the file that lists them all.
 *
 * The fallback is deliberately visible rather than silent: a key added without a name here shows up
 * in the data as "unknown", which is a number somebody will ask about, instead of quietly going
 * missing. `AnalyticsScreenNameTest` fails on either.
 */
internal fun NavKey.analyticsScreenName(): String = when (this) {
    HomeKey -> "home"
    LearnKey -> "learn"
    SignsKey -> "signs"
    IntersectionsKey -> "intersections"
    is SituationKey -> "situation"
    LegislationKey -> "legislation"
    SettingsKey -> "settings"
    ChangeExamKey -> "change_exam"
    AboutKey -> "about"
    FeedbackKey -> "feedback"
    is PracticeKey -> "practice"
    is ResultsKey -> "results"
    else -> "unknown"
}

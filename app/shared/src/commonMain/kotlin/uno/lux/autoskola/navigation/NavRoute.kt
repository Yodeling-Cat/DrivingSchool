package uno.lux.autoskola.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * How a screen is addressed in a route: `home`, `practice/<attemptId>`, `learn/signs`. A route is
 * the segments of a whole back stack, so it says which screens are under the one on top, and reading
 * it back rebuilds that stack. The web puts it in the address bar and the phones store it, which is
 * what lets a reload or a restart come back to the same screen.
 */
private const val HOME = "home"

/** The path [key] is addressed by, or `null` for a screen the app has no route for. */
fun routeSegment(key: NavKey): String? = when (key) {
    HomeKey -> HOME
    LearnKey -> "learn"
    SignsKey -> "signs"
    IntersectionsKey -> "intersections"
    LegislationKey -> "legislation"
    SettingsKey -> "settings"
    ChangeExamKey -> "change-exam"
    is PracticeKey -> "practice/${key.attemptId}"
    is ResultsKey -> "results/${key.attemptId}"
    is SituationKey -> "situation/${key.number}"
    else -> null
}

/** The route of a whole back stack: every screen's segment, the root first. */
fun routeOf(backStack: List<NavKey>): String = backStack.mapNotNull(::routeSegment).joinToString("/")

/** The route of each screen in [route], the root first: `home`, `home/practice/<id>`. */
fun routePath(route: String): List<String> {
    val backStack = backStackOf(route)
    return backStack.indices.map { routeOf(backStack.take(it + 1)) }
}

/**
 * The back stack [route] names. A part that can't be read ends the route there, and a route that
 * doesn't start at the home screen gets it put in front, so an old or hand-typed address always
 * lands on something the app can show.
 */
fun backStackOf(route: String?): List<NavKey> {
    val segments = route.orEmpty().split('/').filter { it.isNotEmpty() }
    val backStack = mutableListOf<NavKey>()
    var index = 0
    while (index < segments.size) {
        val (key, length) = keyAt(segments, index) ?: break
        backStack += key
        index += length
    }
    if (backStack.firstOrNull() != HomeKey) backStack.add(0, HomeKey)
    return backStack
}

/** The screen the segments from [index] name, and how many of them it takes up. */
private fun keyAt(segments: List<String>, index: Int): Pair<NavKey, Int>? = when (segments[index]) {
    HOME -> HomeKey to 1
    "learn" -> LearnKey to 1
    "signs" -> SignsKey to 1
    "intersections" -> IntersectionsKey to 1
    "legislation" -> LegislationKey to 1
    "settings" -> SettingsKey to 1
    "change-exam" -> ChangeExamKey to 1
    "practice" -> segments.getOrNull(index + 1)?.let { PracticeKey(it) to 2 }
    "results" -> segments.getOrNull(index + 1)?.let { ResultsKey(it) to 2 }
    "situation" -> segments.getOrNull(index + 1)?.toIntOrNull()?.let { SituationKey(it) to 2 }
    else -> null
}

/** Shows exactly [backStack], popping and pushing only what the two stacks don't already share. */
fun NavBackStack<NavKey>.navigateTo(backStack: List<NavKey>) {
    var shared = 0
    while (shared < size && shared < backStack.size && this[shared] == backStack[shared]) shared++
    while (size > shared) removeLastOrNull()
    for (index in shared until backStack.size) add(backStack[index])
}

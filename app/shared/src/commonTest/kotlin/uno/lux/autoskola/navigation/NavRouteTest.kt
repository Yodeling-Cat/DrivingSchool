package uno.lux.autoskola.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** A screen the app has no route for, so the codec has to cope with one. */
private data object UnknownKey : NavKey

class NavRouteTest {

    @Test
    fun everyScreenOfABackStackIsInItsRoute() {
        assertEquals("home", routeOf(listOf(HomeKey)))
        assertEquals("home/practice/1759301234567-9x", routeOf(listOf(HomeKey, PracticeKey("1759301234567-9x"))))
        assertEquals("home/learn/signs", routeOf(listOf(HomeKey, LearnKey, SignsKey)))
        assertEquals(
            "home/learn/intersections/situation/3",
            routeOf(listOf(HomeKey, LearnKey, IntersectionsKey, SituationKey(3))),
        )
    }

    @Test
    fun everyRouteReadsBackAsTheStackItCameFrom() {
        val stacks = listOf(
            listOf(HomeKey),
            listOf(HomeKey, LearnKey),
            listOf(HomeKey, LearnKey, SignsKey),
            listOf(HomeKey, LearnKey, IntersectionsKey),
            listOf(HomeKey, LearnKey, IntersectionsKey, SituationKey(3)),
            listOf(HomeKey, LearnKey, LegislationKey),
            listOf(HomeKey, SettingsKey),
            listOf(HomeKey, ChangeExamKey),
            listOf(HomeKey, PracticeKey("1759301234567-9x")),
            listOf(HomeKey, PracticeKey("1-abc"), ResultsKey("1-abc")),
            listOf(HomeKey, SettingsKey, ChangeExamKey),
        )

        for (stack in stacks) {
            assertEquals(stack, backStackOf(routeOf(stack)), "routing $stack")
        }
    }

    @Test
    fun noTwoScreensShareASegment() {
        val keys = listOf(
            HomeKey,
            LearnKey,
            SignsKey,
            IntersectionsKey,
            SituationKey(3),
            LegislationKey,
            SettingsKey,
            ChangeExamKey,
            PracticeKey("1-abc"),
            ResultsKey("1-abc"),
        )

        val segments = keys.mapNotNull(::routeSegment)
        assertEquals(keys.size, segments.distinct().size)
        assertEquals(keys.size, segments.size)
    }

    @Test
    fun aScreenWithoutARouteHasNone() {
        assertNull(routeSegment(UnknownKey))
        assertEquals("home", routeOf(listOf(HomeKey, UnknownKey)))
    }

    @Test
    fun anEmptyRouteIsTheHomeScreen() {
        assertEquals(listOf(HomeKey), backStackOf(null))
        assertEquals(listOf(HomeKey), backStackOf(""))
    }

    @Test
    fun aRouteThatDoesNotStartAtTheHomeScreenGetsItPutInFront() {
        assertEquals(listOf(HomeKey, SettingsKey), backStackOf("settings"))
    }

    @Test
    fun anUnreadablePartEndsTheRoute() {
        assertEquals(listOf(HomeKey), backStackOf("home/nonsense/practice/1-abc"))
        assertEquals(listOf(HomeKey, PracticeKey("1-abc")), backStackOf("home/practice/1-abc/nonsense"))
    }

    @Test
    fun aScreenMissingWhatItIsAboutEndsTheRoute() {
        assertEquals(listOf(HomeKey), backStackOf("home/practice"))
        assertEquals(listOf(HomeKey), backStackOf("home/situation"))
        // A situation is named by its number, so a part that isn't one names no screen.
        assertEquals(listOf(HomeKey), backStackOf("home/situation/first"))
    }

    @Test
    fun aRouteNamesEveryScreenUnderTheTopOne() {
        assertEquals(listOf("home", "home/practice/1-abc"), routePath("home/practice/1-abc"))
        assertEquals(listOf("home"), routePath("home"))
    }

    @Test
    fun showingABackStackKeepsWhatItAlreadyHolds() {
        val backStack = NavBackStack<NavKey>(HomeKey, LearnKey)

        backStack.navigateTo(listOf(HomeKey, LearnKey, SignsKey))
        assertEquals(listOf(HomeKey, LearnKey, SignsKey), backStack.toList())

        backStack.navigateTo(listOf(HomeKey))
        assertEquals(listOf(HomeKey), backStack.toList())

        backStack.navigateTo(listOf(HomeKey, SettingsKey))
        assertEquals(listOf(HomeKey, SettingsKey), backStack.toList())

        backStack.navigateTo(listOf(HomeKey, SettingsKey))
        assertEquals(listOf(HomeKey, SettingsKey), backStack.toList())
    }

    @Test
    fun aBackStackCanBeShownFromNothing() {
        val backStack = NavBackStack<NavKey>()

        backStack.navigateTo(listOf(HomeKey, PracticeKey("1-abc")))
        assertEquals(listOf(HomeKey, PracticeKey("1-abc")), backStack.toList())
    }
}

package com.spiraclestudios.autoskola.navigation

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StoredNavigationStateTest {

    @Test
    fun aFreshInstallHasNoRoute() {
        assertNull(StoredNavigationState(MapSettings()).initialRoute())
    }

    @Test
    fun theRouteIsStillThereAfterARestart() {
        val settings = MapSettings()

        StoredNavigationState(settings).record("home/practice/1-abc")

        // A new instance stands for the app starting again, which is what has to find the route.
        assertEquals("home/practice/1-abc", StoredNavigationState(settings).initialRoute())
    }

    @Test
    fun theLastRouteWins() {
        val settings = MapSettings()
        val state = StoredNavigationState(settings)
        state.record("home/practice/1-abc")

        state.record("home")

        assertEquals("home", StoredNavigationState(settings).initialRoute())
    }
}

package com.spiraclestudios.autoskola.navigation

import com.russhwolf.settings.Settings

/**
 * Keeps the route in [Settings], which outlives the process: the app is killed to free memory, or
 * swiped away altogether, and still comes back to the screen that was on top. Nothing moves the app
 * on its own here, so there is nothing to watch.
 */
class StoredNavigationState(private val settings: Settings) : NavigationState {

    override fun initialRoute(): String? = settings.getStringOrNull(KEY_ROUTE)

    override fun record(route: String) {
        settings.putString(KEY_ROUTE, route)
    }

    override fun observe(onRoute: (String) -> Unit): () -> Unit = {}

    private companion object {
        const val KEY_ROUTE = "navigation_route"
    }
}

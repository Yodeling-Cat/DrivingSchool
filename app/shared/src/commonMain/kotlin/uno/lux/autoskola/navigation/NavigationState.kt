package uno.lux.autoskola.navigation

import com.russhwolf.settings.Settings

/**
 * Where the app's navigation lives outside the composition, so leaving it doesn't lose it: the
 * address bar in a browser, stored settings on the phones. A reload, or the system killing the app
 * to free memory, then comes back to the screen that was on top - and to every screen under it.
 */
interface NavigationState {

    /** The route to open, or `null` to start at the home screen. */
    fun initialRoute(): String?

    /** The app now shows [route], the whole back stack as a route (see [routeOf]). */
    fun record(route: String)

    /**
     * Calls [onRoute] whenever the platform moves on its own - a browser's back and forward buttons
     * - and returns a function that stops listening. Nothing moves the app on the phones.
     */
    fun observe(onRoute: (String) -> Unit): () -> Unit
}

/** How this platform keeps the navigation state; see [NavigationState]. */
expect fun navigationState(settings: Settings): NavigationState

package uno.lux.autoskola.navigation

/**
 * Keeps the app in the address bar: the back stack's route is the page's URL, so a reload comes back
 * to the same screen and the browser's own back and forward buttons walk the stack. Nothing is told
 * to the browser twice, or the two would chase each other.
 */
class UrlNavigationState(private val browser: BrowserLocation) : NavigationState {

    /** The route the browser was last told about, whether the app or the browser moved. */
    private var shown: String? = null

    override fun initialRoute(): String? = browser.route().takeIf { it.isNotEmpty() }?.let(::canonical)

    override fun record(route: String) {
        if (route == shown) return
        val path = routePath(route)
        val depth = shown?.let { routePath(it).size } ?: 0
        shown = route
        when {
            // Nothing shown yet: this is the loaded page, so its own entry becomes the top screen
            // and the screens under it get the entries they need to be gone back to.
            depth == 0 -> path.forEachIndexed { index, under -> browser.move(under, replace = index == 0) }

            path.size > depth -> path.drop(depth).forEach { browser.move(it, replace = false) }

            path.size < depth -> browser.goBack(depth - path.size)

            // A sibling screen - a tab, say - takes the place of the one it replaced rather than
            // leaving the address bar on a screen the app no longer shows.
            else -> browser.move(route, replace = true)
        }
    }

    override fun observe(onRoute: (String) -> Unit): () -> Unit = browser.watch {
        val route = canonical(browser.route())
        if (route == shown) return@watch
        // The browser has already moved; the app follows, and says nothing back.
        shown = route
        onRoute(route)
    }

    /** What an address bar holds, read as a screen the app can show. */
    private fun canonical(route: String): String = routeOf(backStackOf(route))
}

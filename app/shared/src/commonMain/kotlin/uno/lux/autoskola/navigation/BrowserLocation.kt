package uno.lux.autoskola.navigation

/**
 * A browser's session history and address bar, which [UrlNavigationState] drives. An interface so
 * that what the app asks a browser to do can be pinned down by a test, without a browser.
 */
interface BrowserLocation {

    /** The route the address bar holds, empty when it holds none. */
    fun route(): String

    /** Addresses [route], adding an entry to the session history unless [replace] says otherwise. */
    fun move(route: String, replace: Boolean)

    /** Goes back [depth] entries, as the browser's own back button does. */
    fun goBack(depth: Int)

    /** Calls [onChange] when the address bar moves; returns a function that stops listening. */
    fun watch(onChange: () -> Unit): () -> Unit
}

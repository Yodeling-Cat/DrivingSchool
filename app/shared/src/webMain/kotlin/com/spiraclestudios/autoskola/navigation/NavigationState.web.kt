package com.spiraclestudios.autoskola.navigation

import com.russhwolf.settings.Settings
import kotlinx.browser.window
import org.w3c.dom.events.Event

/**
 * In a browser the app's screens are its URL, which is what the browser's history moves through; see
 * [UrlNavigationState]. Nothing is stored in [Settings], because the address bar already holds it.
 */
actual fun navigationState(settings: Settings): NavigationState = UrlNavigationState(AddressBar)

/** The app's own address bar. The route is the URL's fragment, as `#/home/practice/<id>`. */
private object AddressBar : BrowserLocation {

    override fun route(): String = window.location.hash.removePrefix("#").removePrefix("/")

    override fun move(route: String, replace: Boolean) {
        // A fragment navigation keeps the page loaded and puts the app's screens in the browser's
        // history, so its back button walks them. Replacing leaves the entries as they are.
        if (replace) {
            window.location.replace("#/$route")
        } else {
            window.location.hash = "#/$route"
        }
    }

    override fun goBack(depth: Int) {
        window.history.go(-depth)
    }

    override fun watch(onChange: () -> Unit): () -> Unit {
        val listener: (Event) -> Unit = { onChange() }
        // Back and forward fire `popstate`; moving the address bar fires `hashchange`. A second event
        // about a route the app already shows is ignored by [UrlNavigationState].
        window.addEventListener("popstate", listener)
        window.addEventListener("hashchange", listener)
        return {
            window.removeEventListener("popstate", listener)
            window.removeEventListener("hashchange", listener)
        }
    }
}

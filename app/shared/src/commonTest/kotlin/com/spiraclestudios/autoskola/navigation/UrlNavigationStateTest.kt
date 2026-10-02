package com.spiraclestudios.autoskola.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UrlNavigationStateTest {

    @Test
    fun theFirstRouteTakesOverTheAddressBar() {
        val browser = FakeBrowser()
        val state = UrlNavigationState(browser)

        state.record("home")

        assertEquals(listOf("home"), browser.entries)
        assertEquals(0, browser.index)
    }

    @Test
    fun aDeeperScreenIsPushedOntoTheAddressBar() {
        val browser = FakeBrowser()
        val state = UrlNavigationState(browser)
        state.record("home")

        state.record("home/practice/1-abc")

        assertEquals(listOf("home", "home/practice/1-abc"), browser.entries)
        assertEquals(1, browser.index)
    }

    @Test
    fun leavingAScreenMovesTheBrowserBackWithIt() {
        val browser = FakeBrowser()
        val state = UrlNavigationState(browser)
        state.record("home")
        state.record("home/practice/1-abc")

        state.record("home")

        assertEquals(0, browser.index)
        assertEquals("home", browser.route())
    }

    @Test
    fun theSameRouteTellsTheBrowserNothing() {
        val browser = FakeBrowser()
        val state = UrlNavigationState(browser)
        state.record("home")

        state.record("home")

        assertEquals(listOf("home"), browser.entries)
    }

    @Test
    fun aScreenReplacedByAnotherOfTheSameDepthTakesItsPlace() {
        val browser = FakeBrowser()
        val state = UrlNavigationState(browser)
        state.record("home")
        state.record("home/learn")

        state.record("home/settings")

        assertEquals(listOf("home", "home/settings"), browser.entries)
        assertEquals(1, browser.index)
    }

    @Test
    fun aBrowserBackIsReportedOnceAndNotEchoed() {
        val browser = FakeBrowser()
        val state = UrlNavigationState(browser)
        val moved = mutableListOf<String>()
        state.observe { moved += it }
        state.record("home")
        state.record("home/practice/1-abc")

        browser.goBack(1)

        assertEquals(listOf("home"), moved)
        // The app shows what the browser asked for, which tells the browser nothing.
        state.record("home")
        assertEquals(listOf("home", "home/practice/1-abc"), browser.entries)
        assertEquals(0, browser.index)
    }

    @Test
    fun theAppsOwnBackIsNotReportedBack() {
        val browser = FakeBrowser()
        val state = UrlNavigationState(browser)
        val moved = mutableListOf<String>()
        state.observe { moved += it }
        state.record("home")
        state.record("home/practice/1-abc")

        state.record("home")

        assertEquals(emptyList(), moved)
    }

    @Test
    fun aStoppedObserverIsNotCalled() {
        val browser = FakeBrowser()
        val state = UrlNavigationState(browser)
        val moved = mutableListOf<String>()
        val stop = state.observe { moved += it }
        state.record("home")
        state.record("home/practice/1-abc")

        stop()
        browser.goBack(1)

        assertEquals(emptyList(), moved)
    }

    @Test
    fun aDeepRouteThatWasOnlyReloadedRebuildsTheScreensUnderIt() {
        val browser = FakeBrowser(mutableListOf("home/practice/1-abc"))
        val state = UrlNavigationState(browser)

        state.record("home/practice/1-abc")

        assertEquals(listOf("home", "home/practice/1-abc"), browser.entries)
        assertEquals(1, browser.index)
        // Back from a reloaded test still lands on the home screen, not out of the app.
        state.record("home")
        assertEquals(0, browser.index)
    }

    @Test
    fun theAddressBarIsWhereTheAppStarts() {
        assertNull(UrlNavigationState(FakeBrowser()).initialRoute())
        assertEquals("home", UrlNavigationState(FakeBrowser(mutableListOf("nonsense"))).initialRoute())
        assertEquals(
            "home/practice/1-abc",
            UrlNavigationState(FakeBrowser(mutableListOf("home/practice/1-abc"))).initialRoute(),
        )
    }
}

/** A browser's session history, faithful enough to keep its index and to move when it is told to. */
private class FakeBrowser(val entries: MutableList<String> = mutableListOf("")) : BrowserLocation {

    var index = 0
        private set

    private val watchers = mutableListOf<() -> Unit>()

    override fun route(): String = entries[index]

    override fun move(route: String, replace: Boolean) {
        if (replace) {
            entries[index] = route
        } else {
            while (entries.size > index + 1) entries.removeAt(entries.lastIndex)
            entries += route
            index++
        }
    }

    /** Moves through the history the way the browser's own arrows do, which the app is told about. */
    override fun goBack(depth: Int) {
        index = (index - depth).coerceAtLeast(0)
        watchers.toList().forEach { it() }
    }

    override fun watch(onChange: () -> Unit): () -> Unit {
        watchers += onChange
        return { watchers -= onChange }
    }
}

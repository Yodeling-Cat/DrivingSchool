package com.spiraclestudios.autoskola.screenstate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScreenStateTextTest {

    @Test
    fun theValuesComposeSaversMakeComeBackAsTheyWere() {
        val state: Map<String, List<Any?>> = mapOf(
            // A screen scrolled down, a pager on its fourth question of forty: a pager saves the
            // page, the offset and the page count as one list.
            "scroll" to listOf(1200),
            "pager" to listOf(listOf(3, 0.25f, 40)),
            "drag" to listOf(1.5, false, "expanded", 'x', 7L),
        )

        assertEquals(state, ScreenStateText.read(ScreenStateText.write(state)))
    }

    @Test
    fun aScreenKeyedByNavigation3ComesBackAsItWas() {
        // Navigation 3 keeps what a screen held under a Pair of its route and its key's class.
        val state: Map<String, List<Any?>> = mapOf(
            "backStack" to listOf(
                mapOf(
                    ("home" to "HomeKey") to mapOf("scroll" to listOf(40)),
                    ("practice/1-abc" to "PracticeKey") to mapOf("pager" to listOf(listOf(11, 0f, 40))),
                ),
            ),
        )

        assertEquals(state, ScreenStateText.read(ScreenStateText.write(state)))
    }

    @Test
    fun whatCannotBeWrittenIsLeftOutRatherThanLosingTheRest() {
        // Navigation 3's map of screens also carries the platform's own saved state for each screen,
        // which is none of ours: the screens' positions are worth more than the whole write.
        val state: Map<String, List<Any?>> = mapOf(
            "backStack" to listOf(
                mapOf(
                    ("practice/1-abc" to "PracticeKey") to mapOf(
                        "pager" to listOf(listOf(11, 0f, 40)),
                        "androidx.savedstate.SavedStateRegistry" to listOf(Any()),
                    ),
                    ("home" to "HomeKey") to mapOf("scroll" to listOf(40)),
                ),
            ),
        )
        val written: Map<String, List<Any?>> = mapOf(
            "backStack" to listOf(
                mapOf(
                    ("practice/1-abc" to "PracticeKey") to mapOf("pager" to listOf(listOf(11, 0f, 40))),
                    ("home" to "HomeKey") to mapOf("scroll" to listOf(40)),
                ),
            ),
        )

        assertEquals(written, ScreenStateText.read(ScreenStateText.write(state)))
    }

    @Test
    fun aValueThatCannotBeWrittenIsLeftOutOfTheState() {
        val state: Map<String, List<Any?>> = mapOf("scroll" to listOf(40), "bundle" to listOf(Any()))

        assertEquals(mapOf("scroll" to listOf(40)), ScreenStateText.read(ScreenStateText.write(state)))
    }

    @Test
    fun nothingToWriteIsStillAValueWorthKeeping() {
        // A saver that returns null has nothing to save, which is not a value this refuses.
        assertTrue(ScreenStateText.holds(null))
        assertTrue(ScreenStateText.holds(listOf(1, null, 2)))
    }

    @Test
    fun aValueItCannotWriteIsRefused() {
        // On Android a Bundle, a Parcelable; anything the platform keeps instead - and half a list
        // is not the list that was saved, so a list holding one of them is not worth keeping either.
        assertFalse(ScreenStateText.holds(Any()))
        assertFalse(ScreenStateText.holds(listOf("fine", Any())))
        assertTrue(ScreenStateText.holds(mapOf("fine" to Any())))
    }

    @Test
    fun textItDidNotWriteReadsAsNoState() {
        assertEquals(emptyMap(), ScreenStateText.read(""))
        assertEquals(emptyMap(), ScreenStateText.read("not json at all"))
        assertEquals(emptyMap(), ScreenStateText.read("""{"version":1}"""))
        assertEquals(emptyMap(), ScreenStateText.read("""{"version":1,"state":{"t":"nonsense","v":1}}"""))
        assertEquals(emptyMap(), ScreenStateText.read("""{"version":1,"state":{"t":"map","v":[["a",1]]}}"""))
    }

    @Test
    fun aStateWrittenByAnotherShapeOfTheAppIsNotRead() {
        // Reading an older state as if it were this one is how a screen gets a value it can't use.
        assertEquals(emptyMap(), ScreenStateText.read("""{"version":2,"state":{"t":"map","v":[]}}"""))
    }
}

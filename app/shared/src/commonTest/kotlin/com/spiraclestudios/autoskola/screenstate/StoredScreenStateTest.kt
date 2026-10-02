package com.spiraclestudios.autoskola.screenstate

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class StoredScreenStateTest {

    @Test
    fun nothingIsStoredOnAFreshInstall() {
        assertEquals(emptyMap(), StoredScreenState(MapSettings()).read())
    }

    @Test
    fun aScreenPositionIsStillThereAfterARestart() {
        val settings = MapSettings()
        val state: Map<String, List<Any?>> = mapOf("home" to listOf(40), "practice" to listOf(listOf(11, 0f, 40)))

        StoredScreenState(settings).write(state)

        // A new instance stands for the app starting again, which is what has to read it back.
        assertEquals(state, StoredScreenState(settings).read())
    }

    @Test
    fun whatCannotBeWrittenIsLeftOutOfWhatIsStored() {
        val storage = StoredScreenState(MapSettings())

        storage.write(mapOf("scroll" to listOf(40), "bundle" to listOf(Any())))

        assertEquals(mapOf("scroll" to listOf(40)), storage.read())
    }
}

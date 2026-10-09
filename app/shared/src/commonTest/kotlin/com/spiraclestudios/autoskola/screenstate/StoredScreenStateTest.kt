package com.spiraclestudios.autoskola.screenstate

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StoredScreenStateTest {

    @Test
    fun nothingIsStoredOnAFreshInstall() {
        assertNull(StoredScreenState(MapSettings()).read())
    }

    @Test
    fun aScreenPositionIsStillThereAfterARestart() {
        val settings = MapSettings()
        val text = ScreenStateText.write(mapOf("home" to listOf(40), "practice" to listOf(listOf(11, 0f, 40))))

        StoredScreenState(settings).write(text)

        // A new instance stands for the app starting again, which is what has to read it back.
        assertEquals(text, StoredScreenState(settings).read())
    }
}

package com.spiraclestudios.autoskola.screenstate

import com.russhwolf.settings.Settings

/**
 * Keeps the state of the app's screens in [Settings], which outlives the process: the system kills
 * the app to free memory, or the driver swipes it away from the recents screen, and it comes back
 * to the screen they left and to the place in it - the question they were on, how far they had
 * scrolled. Every platform gets this, not only the ones that restore their own state.
 */
class StoredScreenState(private val settings: Settings) : ScreenStateStorage {

    override fun read(): String? = settings.getStringOrNull(KEY_STATE)

    override fun write(text: String) {
        settings.putString(KEY_STATE, text)
    }

    private companion object {
        const val KEY_STATE = "screen_state"
    }
}

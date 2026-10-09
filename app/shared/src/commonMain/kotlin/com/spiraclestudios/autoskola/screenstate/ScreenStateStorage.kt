package com.spiraclestudios.autoskola.screenstate

/**
 * Where the state of the app's screens is kept between runs: the map Compose's saveable state
 * registry holds, one entry per `rememberSaveable` in the app, as the text [ScreenStateText] turns
 * it into. Plain storage only - what is worth keeping, when, and how it is written down lives in
 * [StoredSaveableState].
 */
interface ScreenStateStorage {

    /** What was stored, or `null` when nothing was. */
    fun read(): String?

    fun write(text: String)
}

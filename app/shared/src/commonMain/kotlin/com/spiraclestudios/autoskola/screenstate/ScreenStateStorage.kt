package com.spiraclestudios.autoskola.screenstate

/**
 * Where the state of the app's screens is kept between runs: the map Compose's saveable state
 * registry holds, one entry per `rememberSaveable` in the app. Plain storage only - what is worth
 * keeping, and when, lives in [StoredSaveableState].
 */
interface ScreenStateStorage {

    /** What was stored, or an empty state when nothing was or it cannot be read. */
    fun read(): Map<String, List<Any?>>

    fun write(state: Map<String, List<Any?>>)
}

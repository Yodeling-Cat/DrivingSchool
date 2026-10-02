package com.spiraclestudios.autoskola.screenstate

import androidx.compose.runtime.saveable.SaveableStateRegistry
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StoredSaveableStateTest {

    @Test
    fun aScreenComesBackToWhereItWasLeft() {
        val state = StoredSaveableState(RecordingStorage(mapOf("scroll" to listOf(1200))), platform = null)

        assertEquals(1200, state.consumeRestored("scroll"))
    }

    @Test
    fun aValueIsRestoredOnlyOnce() {
        val state = StoredSaveableState(RecordingStorage(mapOf("scroll" to listOf(1200))), platform = null)

        assertEquals(1200, state.consumeRestored("scroll"))
        assertNull(state.consumeRestored("scroll"))
    }

    @Test
    fun aPositionThatChangesIsWrittenOnTheNextSave() {
        val storage = RecordingStorage()
        val state = StoredSaveableState(storage, platform = null)
        var scroll = 0
        state.registerProvider("scroll") { scroll }

        assertEquals(0, storage.writes)

        scroll = 1200
        state.save()

        assertEquals(mapOf("scroll" to listOf(1200)), storage.state)
    }

    @Test
    fun aStateThatHasNotChangedIsNotWrittenAgain() {
        val storage = RecordingStorage(mapOf("scroll" to listOf(1200)))
        val state = StoredSaveableState(storage, platform = null)
        state.registerProvider("scroll") { 1200 }

        state.save()
        state.save()

        assertEquals(0, storage.writes)
    }

    @Test
    fun aValueThatIsBuiltAfreshEachTimeIsNotWrittenAgain() {
        // Navigation 3's screens hand over a newly built object every time they are asked, which
        // compares unequal while the state it describes is the same. What is compared is what the
        // storage is given - the text - so a screen doesn't rewrite the whole state every moment.
        val storage = RecordingStorage()
        val state = StoredSaveableState(storage, platform = null)
        state.registerProvider("backStack") { listOf(mapOf("the platform's own saved state" to Any())) }
        state.registerProvider("scroll") { 1200 }

        state.save()
        state.save()
        state.save()

        assertEquals(1, storage.writes)
    }

    @Test
    fun whatIsStillBeingReadIsNotErasedByASave() {
        // The app opens on the screen it was left on, whose question has not been read yet because
        // the test behind it is still loading. A save in that moment must not take it away.
        val stored = mapOf<String, List<Any?>>("practice" to listOf(listOf(11, 0f, 40)))
        val storage = RecordingStorage(stored)
        val state = StoredSaveableState(storage, platform = null)

        state.save()

        assertEquals(stored, storage.state)
    }

    @Test
    fun aValueThatCannotBeWrittenIsStillTakenAndSavesTheRest() {
        // Navigation 3 hands over the whole map of screens, which carries one entry nobody can write
        // on a platform with no registry of its own. Refusing it would take every screen down with
        // it; leaving it out costs the screen nothing that was ever kept there.
        val settings = MapSettings()
        val storage = StoredScreenState(settings)
        val state = StoredSaveableState(storage, platform = null)

        assertTrue(state.canBeSaved(Any()))
        state.registerProvider("pager") { listOf(11, 0f, 40) }
        state.registerProvider("platform saved state") { Any() }
        state.save()

        assertEquals(mapOf("pager" to listOf(listOf(11, 0f, 40))), StoredScreenState(settings).read())
    }

    @Test
    fun whatTheRegistryAboveCanKeepIsGivenToItAsWell() {
        // Android's saved instance state keeps what it always kept, so nothing is lost by this.
        val platform = RecordingRegistry()
        val state = StoredSaveableState(RecordingStorage(), platform)

        val entry = state.registerProvider("scroll") { 1200 }

        assertTrue("scroll" in platform.providers)

        entry.unregister()

        assertFalse("scroll" in platform.providers)
    }

    @Test
    fun whatTheRegistryAboveCannotKeepIsNotForcedOnIt() {
        val platform = RecordingRegistry(keep = { false })
        val state = StoredSaveableState(RecordingStorage(), platform)

        state.registerProvider("scroll") { 1200 }

        assertTrue(platform.providers.isEmpty())
    }

    @Test
    fun whatThePlatformHoldsNowWinsOverWhatWasStored() {
        // Android's saved instance state is written as the app is left; the storage is written a
        // moment behind it, so what the platform has is the fresher one.
        val platform = RecordingRegistry(restored = mapOf("pager" to listOf(listOf(4, 0f, 40))))
        val stored = mapOf<String, List<Any?>>("pager" to listOf(listOf(11, 0f, 40)))
        val state = StoredSaveableState(RecordingStorage(stored), platform)

        assertEquals(4, (state.consumeRestored("pager") as List<*>).first())
    }

    @Test
    fun aValueNothingKeepsIsStillTaken() {
        // With no registry above it there is nowhere to hand a value to, and refusing it would
        // crash the screen holding it - which is what the platform registry would do anyway.
        val state = StoredSaveableState(RecordingStorage(), platform = null)

        assertTrue(state.canBeSaved(Any()))
        state.registerProvider("bundle") { Any() }
    }
}

/** A storage that remembers what it was given, and how often it was written. */
private class RecordingStorage(initial: Map<String, List<Any?>> = emptyMap()) : ScreenStateStorage {
    var state: Map<String, List<Any?>> = initial
        private set

    var writes = 0
        private set

    override fun read(): Map<String, List<Any?>> = state

    override fun write(state: Map<String, List<Any?>>) {
        this.state = state
        writes++
    }
}

/** The registry a platform puts above the app's own, kept plain enough for a test to look at. */
private class RecordingRegistry(
    restored: Map<String, List<Any?>> = emptyMap(),
    private val keep: (Any) -> Boolean = { true },
) : SaveableStateRegistry {
    private val restoredState = restored.toMutableMap()

    val providers = mutableMapOf<String, () -> Any?>()

    override fun canBeSaved(value: Any): Boolean = keep(value)

    override fun consumeRestored(key: String): Any? = restoredState.remove(key)?.firstOrNull()

    override fun registerProvider(key: String, valueProvider: () -> Any?): SaveableStateRegistry.Entry {
        providers[key] = valueProvider
        return object : SaveableStateRegistry.Entry {
            override fun unregister() {
                providers.remove(key)
            }
        }
    }

    override fun performSave(): Map<String, List<Any?>> = emptyMap()
}

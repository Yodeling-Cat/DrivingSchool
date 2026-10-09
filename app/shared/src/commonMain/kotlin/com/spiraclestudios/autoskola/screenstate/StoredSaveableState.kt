package com.spiraclestudios.autoskola.screenstate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long the state is left to settle after it changes before it is written. */
private const val WRITE_DELAY_MILLIS = 500L

/**
 * Where the screens' state is turned into text and written: off the main thread, one save at a time.
 * A browser has one thread, so there it is the main one, a moment later.
 */
private val ScreenStateWriter: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1)

/**
 * Keeps what `rememberSaveable` holds - the question a test was on, how far a screen was scrolled,
 * the tab that was open - in [storage], which outlives the process.
 *
 * Compose keeps that state only as long as the platform does: a browser's address bar, or a process
 * the system killed while it kept the app's instance state. The screen the app opens on is kept by
 * the app itself, which is why it already comes back to the practice screen - but not to the
 * question in it, because the state inside a screen had nothing to outlive the process with. This
 * is that, for every screen at once.
 *
 * Anything the registry above this one can keep is registered with it as well, so nothing that was
 * kept before is dropped by providing this one - Android's saved instance state above all, which
 * holds the values the app had as it was left rather than the ones a moment before. What that
 * registry holds is also preferred when a screen asks for its state back, for the same reason:
 * [storage] is written a moment behind it. What neither can keep is left out of [storage] by
 * [ScreenStateText] rather than refused, so a value that cannot be written can never fail a screen.
 *
 * A save is split in two. The screens are asked for their state on the thread they live on, the
 * main one, because that is where their state changes. Turning it into text, comparing it with the
 * last write and writing it happens on [writer], one save at a time and in the order they were made,
 * so the main thread only pays for the asking.
 */
internal class StoredSaveableState(
    private val storage: ScreenStateStorage,
    private val platform: SaveableStateRegistry?,
    writer: CoroutineDispatcher,
) : SaveableStateRegistry {

    /**
     * Where the saves are written. Not tied to a composition: a save made as the app leaves the
     * front is written even when the screens are gone by the time it gets its turn.
     */
    private val writes = CoroutineScope(SupervisorJob() + writer)

    /** What the storage held, as it was written. */
    private val storedText: String? = storage.read()

    /** What the storage held, kept here as the screens take it up. */
    private val stored: Map<String, List<Any?>> = storedText?.let(ScreenStateText::read).orEmpty()

    /** What the storage is fed from. It takes anything: [ScreenStateText] leaves out the rest. */
    private val saved: SaveableStateRegistry = SaveableStateRegistry(restoredValues = stored, canBeSaved = { true })

    /**
     * What was written last, as the text that was written: what is compared is what [storage] is
     * given, not the values behind it. Only ever touched on [writes]. Compose's own savers hand over a fresh object each time they
     * are asked (Navigation 3's screens do), so the values compare unequal from one moment to the
     * next while the state they describe is the same - and the state would be written every time.
     */
    private var written: String? = storedText

    override fun canBeSaved(value: Any): Boolean = ScreenStateText.holds(value) || (platform?.canBeSaved(value) ?: true)

    override fun consumeRestored(key: String): Any? = platform?.consumeRestored(key) ?: saved.consumeRestored(key)

    override fun registerProvider(key: String, valueProvider: () -> Any?): SaveableStateRegistry.Entry {
        val written = saved.registerProvider(key, valueProvider)
        // Only if the registry above can keep this value too, asked the way Compose asks before it
        // saves: registering one it cannot would fail later, as it writes its state. A value with
        // nothing in it is left alone, the same way Compose saves nothing for it.
        val held = valueProvider()
        val alsoKept = platform
            ?.takeIf { held == null || it.canBeSaved(held) }
            ?.registerProvider(key, valueProvider)
        return if (alsoKept == null) written else Both(written, alsoKept)
    }

    override fun performSave(): Map<String, List<Any?>> = saved.performSave()

    /**
     * Takes what the screens hold now and has [writer] write it, if it differs from the last write: a
     * screen's position settles as the driver works, so writing on every change would write it over
     * and over. Called on the main thread, where the screens' state lives.
     */
    fun save() {
        val state = performSave()
        writes.launch { writeIfChanged(state) }
    }

    private fun writeIfChanged(state: Map<String, List<Any?>>) {
        val text = ScreenStateText.write(state)
        if (text == written) return
        written = text
        storage.write(text)
    }

    /** A value kept in two places at once, let go of in both. */
    private class Both(
        private val written: SaveableStateRegistry.Entry,
        private val alsoKept: SaveableStateRegistry.Entry,
    ) : SaveableStateRegistry.Entry {
        override fun unregister() {
            written.unregister()
            alsoKept.unregister()
        }
    }
}

/**
 * Provides the state of the app's screens from [storage], so every `rememberSaveable` in [content]
 * outlives the process (see [StoredSaveableState]).
 *
 * It is written as the app leaves the front - the moment before a phone's system kills it - and now
 * and then while it is in use, so a browser that reloads or a process that dies still comes back to
 * where the driver was.
 */
@Composable
fun ProvideStoredScreenState(storage: ScreenStateStorage, content: @Composable () -> Unit) {
    val platform = LocalSaveableStateRegistry.current
    val state = remember(storage) { StoredSaveableState(storage, platform, ScreenStateWriter) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { state.save() }
    // Only while the app is in front: once it has left, the save on the way out has the last word,
    // and asking the screens twice a second for state that is not changing is work for nothing.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(state, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(WRITE_DELAY_MILLIS)
                state.save()
            }
        }
    }
    CompositionLocalProvider(LocalSaveableStateRegistry provides state, content = content)
}

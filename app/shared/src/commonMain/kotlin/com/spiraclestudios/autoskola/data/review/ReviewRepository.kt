package com.spiraclestudios.autoskola.data.review

import com.russhwolf.settings.Settings
import com.spiraclestudios.autoskola.review.ReviewAsk
import com.spiraclestudios.autoskola.review.ReviewRequest
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * How far the app has got in asking the user to rate it, kept in [Settings] the way preferences
 * are: it is two small values the home screen needs for its first frame, so [request] is read once
 * and answered synchronously.
 *
 * Writes are suspended and run to the end even when the screen that made them goes away, and one
 * at a time, so two answers - a press on the store and a cross, one after the other - can't lose
 * one another. What the ladder means, and how it moves, is [ReviewRequest]'s job in `:core`.
 */
class ReviewRepository(private val settings: Settings) {

    private val state = MutableStateFlow(read())

    /** Where the asking stands, as it changes. */
    val request: StateFlow<ReviewRequest> = state.asStateFlow()

    private val writes = Mutex()

    /** The user closed the card without going to the store, having finished [finishedTests] tests. */
    suspend fun dismissed(finishedTests: Int, everyTestPassed: Boolean) =
        write { request -> request.dismissed(finishedTests, everyTestPassed) }

    /** The user was taken to the store, which ends the asking whether or not they rated the app. */
    suspend fun rated() = write(ReviewRequest::rated)

    private suspend fun write(change: (ReviewRequest) -> ReviewRequest) = withContext(NonCancellable) {
        writes.withLock {
            val updated = change(state.value)
            settings.putString(KEY_ASK, updated.ask.name)
            updated.dismissedAfter?.let { settings.putInt(KEY_DISMISSED_AFTER, it) }
            state.value = updated
        }
    }

    /**
     * What was stored, or the beginning of the ladder. The stage is stored by its name, so a stage
     * this build doesn't know - one a later build added, or a name it renamed - reads as the card
     * never having been dismissed, which asks the user again rather than hiding the ask for good.
     */
    private fun read() = ReviewRequest(
        ask = settings.getStringOrNull(KEY_ASK)?.let { stored ->
            ReviewAsk.entries.firstOrNull { it.name == stored }
        } ?: ReviewAsk.FirstAsk,
        dismissedAfter = settings.getIntOrNull(KEY_DISMISSED_AFTER),
    )

    private companion object {
        const val KEY_ASK = "review_ask"
        const val KEY_DISMISSED_AFTER = "review_dismissed_after"
    }
}

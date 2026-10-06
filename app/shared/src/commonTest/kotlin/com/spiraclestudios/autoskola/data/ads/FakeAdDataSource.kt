package com.spiraclestudios.autoskola.data.ads

/**
 * An [AdDataSource] a test can script, holding the little state a real one holds: at most one ad
 * loaded and ready at a time, and a record of what it was asked to show.
 *
 * That record is the point. Several of the rules below are about what was *not* shown, and an ad
 * that was never asked for is invisible unless something writes it down.
 */
class FakeAdDataSource(
    override val available: Boolean = true,
    /** What [show] answers once an ad has been preloaded. */
    var result: AdResult = AdResult.Shown,
) : AdDataSource {

    /** The slot whose ad is loaded and ready, if any. Dropped by [show] and by [discard]. */
    var loaded: AdSlot? = null
        private set

    /** Every slot a [show] was attempted for, in order. */
    val shown = mutableListOf<AdSlot>()

    /** Every slot a [preload] was attempted for, in order. */
    val preloaded = mutableListOf<AdSlot>()

    override suspend fun preload(slot: AdSlot) {
        preloaded += slot
        loaded = slot
    }

    override suspend fun show(slot: AdSlot): AdResult {
        shown += slot
        loaded = null
        return result
    }

    override suspend fun discard() {
        loaded = null
    }

    /** Every value [setUnderAgeOfConsent] was handed, in order. */
    val underAgeOfConsent = mutableListOf<Boolean>()

    override fun setUnderAgeOfConsent(underAge: Boolean) {
        underAgeOfConsent += underAge
    }
}

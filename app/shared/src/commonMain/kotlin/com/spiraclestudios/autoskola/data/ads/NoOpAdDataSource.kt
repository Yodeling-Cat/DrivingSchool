package com.spiraclestudios.autoskola.data.ads

/**
 * The implementation for a target with no ad SDK, and the one every target runs until the Android
 * and iOS data sources are written (see `plans/ads.md`, steps 8 and 13).
 *
 * It reports [available] as false rather than answering no fill, so that nothing downstream mistakes
 * it for an ad network that happened to have nothing that day: no ad is requested, no consent is
 * needed for one, and the offer never appears - which is exactly what the app should do before there
 * is anything to show.
 */
class NoOpAdDataSource : AdDataSource {

    override val available: Boolean = false

    override suspend fun preload(slot: AdSlot) = Unit

    override suspend fun show(slot: AdSlot): AdResult = AdResult.Unavailable

    override suspend fun discard() = Unit
}

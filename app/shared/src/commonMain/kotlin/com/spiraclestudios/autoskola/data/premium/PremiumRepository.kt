package com.spiraclestudios.autoskola.data.premium

import com.spiraclestudios.autoskola.premium.PremiumFeature
import com.spiraclestudios.autoskola.premium.PremiumRules
import com.spiraclestudios.autoskola.premium.PremiumStatus
import com.spiraclestudios.autoskola.premium.PurchaseSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The one flag that stands in for the store. There is no billing yet (see `plans/premium.md`), so
 * there is nothing to ask, and this is what the app answers with instead: flip it to `true` and the
 * whole app behaves as if Premium had been bought, which is how the premium paths - no ads, every
 * license group, the full learn material - are walked by hand before there is a purchase to make.
 *
 * It is a local, temporary change rather than a setting. `PremiumRepositoryTest.theShippedDudIsFree`
 * fails while it is on, so it cannot be committed by accident.
 */
const val PREMIUM_UNLOCKED = false

/**
 * What the user owns.
 *
 * Screens and ViewModels ask [isUnlocked] rather than comparing a status themselves, so what Premium
 * includes is decided in exactly one place ([PremiumRules]) and changing it changes a test.
 *
 * [entitlement] is a flow because the answer changes without the app restarting - a purchase, a
 * restore, or a refund that takes it back again - and everything watching it has to follow. Nothing
 * writes it while the dud is in place; billing will, from the other side of the constructor.
 */
class PremiumRepository(initial: PremiumStatus = dudStatus()) {

    private val state = MutableStateFlow(initial)

    /** The entitlement as it changes. */
    val entitlement: StateFlow<PremiumStatus> = state.asStateFlow()

    /** Whether [feature] is unlocked right now. Reads the live answer, never a cached one. */
    fun isUnlocked(feature: PremiumFeature): Boolean = PremiumRules.isUnlocked(feature, state.value)
}

/**
 * The dud's answer while there is no store to ask.
 *
 * [PurchaseSource.Unknown] rather than a store, because no store granted it: it is a flag in the
 * build, and naming Play or Apple here would put a lie in the entitlement.
 */
private fun dudStatus(): PremiumStatus =
    if (PREMIUM_UNLOCKED) PremiumStatus.Premium(PurchaseSource.Unknown) else PremiumStatus.Free

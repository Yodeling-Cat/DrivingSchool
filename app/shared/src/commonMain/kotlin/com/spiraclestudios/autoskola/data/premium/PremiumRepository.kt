package com.spiraclestudios.autoskola.data.premium

import com.spiraclestudios.autoskola.premium.PremiumFeature
import com.spiraclestudios.autoskola.premium.PremiumRules
import com.spiraclestudios.autoskola.premium.PremiumStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
class PremiumRepository(initial: PremiumStatus = PremiumStatus.Free) {

    private val state = MutableStateFlow(initial)

    /** The entitlement as it changes. */
    val entitlement: StateFlow<PremiumStatus> = state.asStateFlow()

    /** Whether [feature] is unlocked right now. Reads the live answer, never a cached one. */
    fun isUnlocked(feature: PremiumFeature): Boolean = PremiumRules.isUnlocked(feature, state.value)

    /**
     * Puts the entitlement to [status]: a purchase, a restore, or a refund taking it back again.
     *
     * Billing calls this with what the store last confirmed (see `plans/premium.md`), and nothing
     * calls it while the dud stands in for the store. It is here rather than left for that step
     * because everything reading the entitlement has to cope with it changing mid-session - the ads
     * do, and this is how their tests say so.
     */
    fun update(status: PremiumStatus) {
        state.value = status
    }
}

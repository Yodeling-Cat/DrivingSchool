package com.spiraclestudios.autoskola.premium

/**
 * The free/paid split, in one place. Screens, ViewModels and repositories ask this rather than
 * comparing a boolean, so changing what Premium includes is a change to one `when` and the tests
 * that pin it (`plans/premium.md`).
 */
object PremiumRules {

    /** Whether [status] unlocks [feature]. */
    fun isUnlocked(feature: PremiumFeature, status: PremiumStatus): Boolean = when (status) {
        PremiumStatus.Free -> false

        is PremiumStatus.Premium -> true

        // The "remove ads" product unlocks the one thing it was sold for.
        is PremiumStatus.AdsRemoved -> feature == PremiumFeature.NoAds
    }
}

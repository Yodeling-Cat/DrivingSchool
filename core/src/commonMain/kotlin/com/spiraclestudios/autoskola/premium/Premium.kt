package com.spiraclestudios.autoskola.premium

/**
 * The things a purchase unlocks. The default is not to have them: the free tier has to be enough to
 * get a learner through the theory test on its own (see `plans/premium.md`).
 */
enum class PremiumFeature {
    /** No ads are requested, so every ad slot in the app collapses. */
    NoAds,

    /** Every licence group's questions, not only the chosen exam's. */
    AllLicenseGroups,

    /** Every country's bank, not only the chosen exam's. */
    AllCountries,

    /** The full learn material rather than its free sample. */
    LearnMaterial,

    /** Mistakes review and per-question statistics; the plain result stays free. */
    MistakeStatistics,
}

/**
 * Which store the entitlement came from.
 *
 * [Unknown] is not an error case: it covers an entitlement cached from a store that could not be
 * identified, a hand-set flag in a debug build, and a future server-side entitlement — none of which
 * is a purchase this device made, and all of which are still Premium.
 */
enum class PurchaseSource {
    Play,
    AppStore,
    Unknown,
}

/** What the user owns. */
sealed interface PremiumStatus {
    /** Nothing bought: every feature is locked and ads are shown. */
    data object Free : PremiumStatus

    /** Bought, or granted. Which store it came from does not change what is unlocked. */
    data class Premium(val source: PurchaseSource) : PremiumStatus
}

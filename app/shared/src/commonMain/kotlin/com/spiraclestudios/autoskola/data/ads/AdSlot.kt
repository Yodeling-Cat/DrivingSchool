package com.spiraclestudios.autoskola.data.ads

/**
 * One ad placement, which is one ad unit in the account and one line in the reports.
 *
 * It is deliberately not "banner" or "interstitial": the format is a property of the unit, and the
 * same slot always resolves to the same unit on a given platform, so that the numbers for "the
 * banner under the mistakes card" can be read on their own rather than as a blend of four screens.
 *
 * See `plans/ads.md`, "Placements", for where each of these sits and why.
 */
enum class AdSlot {
    /** Under the mistakes card on the home screen. */
    HomeBanner,

    /** Pinned to the bottom of the intersections list. */
    IntersectionsBanner,

    /** Pinned to the bottom of one situation. */
    SituationBanner,

    /** After a test result is dismissed. Never on review. */
    ResultsInterstitial,

    /** Gates every tenth practice test. */
    TestOfferInterstitial,
}

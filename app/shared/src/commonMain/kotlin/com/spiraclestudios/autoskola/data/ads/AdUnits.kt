package com.spiraclestudios.autoskola.data.ads

import com.spiraclestudios.autoskola.isDebugBuild

/**
 * Google's demo units, which belong to no account and can be used by anyone.
 *
 * The demo set is one unit per *format*, not one per placement, so five slots share the banner unit
 * and two share the interstitial. That is the whole reason a debug build cannot exercise per-placement
 * reporting, and it is also why no test asserts that these ids are distinct.
 *
 * Verify them against the current [Android](https://developers.google.com/admob/android/test-ads) and
 * [iOS](https://developers.google.com/admob/ios/test-ads) pages when this is touched: the set has
 * changed before.
 */
internal val TestAdUnits: Map<AdSlot, String> = mapOf(
    AdSlot.HomeBanner to DEMO_BANNER,
    AdSlot.IntersectionsBanner to DEMO_BANNER,
    AdSlot.SituationBanner to DEMO_BANNER,
    AdSlot.SignsBanner to DEMO_BANNER,
    AdSlot.ResultsBanner to DEMO_BANNER,
    AdSlot.ResultsInterstitial to DEMO_INTERSTITIAL,
    AdSlot.TestOfferInterstitial to DEMO_INTERSTITIAL,
)

/**
 * This account's units for the target in question, one per slot, by the country whose app they
 * belong to (the bundled content's `country`).
 *
 * An `expect` rather than one shared map, because AdMob registers an app *per platform* and an ad
 * unit belongs to its app: an Android unit id cannot be used in an iOS build, and the Slovak app's
 * cannot be used in the Czech one. A country with no AdMob app of its own has no entry, and a target
 * that cannot serve ads at all - the web build - answers with an empty map rather than placeholders,
 * which is what lets [adUnitId] return null instead of a string that would fail at request time.
 */
internal expect val ReleaseAdUnits: Map<String, Map<AdSlot, String>>

/**
 * The unit to ask for [slot] in [country]'s app, or null where it has no ads at all.
 *
 * [debug] is a parameter rather than a read of [isDebugBuild] inside, so a test can pin both
 * directions: a debug build must never resolve an account's unit, which serves a real impression to a
 * developer and is an AdMob policy problem, and a release build must never resolve a demo unit, which
 * serves nothing and shows up only as no-fill in production.
 */
internal fun adUnitId(slot: AdSlot, country: String, debug: Boolean = isDebugBuild()): String? =
    if (debug) TestAdUnits[slot] else ReleaseAdUnits[country]?.get(slot)

/** Google's demo adaptive banner, used by every banner slot in a debug build. */
private const val DEMO_BANNER = "ca-app-pub-3940256099942544/9214589741"

/** Google's demo interstitial, used by every interstitial slot in a debug build. */
private const val DEMO_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"

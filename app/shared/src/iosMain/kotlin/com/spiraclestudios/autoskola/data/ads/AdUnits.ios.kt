package com.spiraclestudios.autoskola.data.ads

/**
 * This account's iOS units, from AdMob, under the app "Naj Autoskola (iOS)".
 *
 * Separate units rather than the Android ones in a different file: AdMob registers an app per
 * platform, and an ad unit belongs to its app, so the numbers differ and only these will serve. The
 * app id that goes with them - for `GADApplicationIdentifier` in `Info.plist` - is
 * `ca-app-pub-7362127401681337~7083800511`.
 *
 * The names in the account are `ios_<slot>`.
 */
internal actual val ReleaseAdUnits: Map<AdSlot, String> = mapOf(
    AdSlot.HomeBanner to "ca-app-pub-7362127401681337/8153339602",
    AdSlot.IntersectionsBanner to "ca-app-pub-7362127401681337/9470290702",
    AdSlot.SituationBanner to "ca-app-pub-7362127401681337/1101769537",
    AdSlot.ResultsInterstitial to "ca-app-pub-7362127401681337/2901012923",
    AdSlot.TestOfferInterstitial to "ca-app-pub-7362127401681337/8213972792",
)

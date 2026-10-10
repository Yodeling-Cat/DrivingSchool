package com.spiraclestudios.autoskola.data.ads

/**
 * This account's Android units, from AdMob, under the app "Naj Autoskola".
 *
 * The names in the account are `android_<slot>`, so a report line and a line of this file can be read
 * against each other without guessing. The app id that goes with them - for the manifest's
 * `com.google.android.gms.ads.APPLICATION_ID` - is `ca-app-pub-7362127401681337~8243476603`.
 *
 * These are not secrets: every one of them ships inside the APK and can be read out of it. What keeps
 * them out of a developer's build is [adUnitId]'s debug flag, not their being hidden.
 *
 * They belong to the Slovak app, the `sk` flavor: a unit serves only the app it was made under. The
 * Czech app has no AdMob app of its own yet, so its release build has no units and shows no ads (see
 * `plans/czech-tests.md`).
 */
internal actual val ReleaseAdUnits: Map<String, Map<AdSlot, String>> = mapOf(
    "SK" to mapOf(
        AdSlot.HomeBanner to "ca-app-pub-7362127401681337/2361956877",
        AdSlot.IntersectionsBanner to "ca-app-pub-7362127401681337/5451154646",
        AdSlot.SituationBanner to "ca-app-pub-7362127401681337/5041014544",
        AdSlot.SignsBanner to "ca-app-pub-7362127401681337/6777636616",
        AdSlot.ResultsBanner to "ca-app-pub-7362127401681337/5977534146",
        AdSlot.ResultsInterstitial to "ca-app-pub-7362127401681337/5718747956",
        AdSlot.TestOfferInterstitial to "ca-app-pub-7362127401681337/2901936367",
    ),
)

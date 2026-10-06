package com.spiraclestudios.autoskola.data.ads

import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.spiraclestudios.autoskola.currentActivity
import com.spiraclestudios.autoskola.isDebugBuild
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Google's ads SDK on Android.
 *
 * Deliberately thin: it loads, shows and discards, and knows nothing about the entitlement or how
 * often an ad is due. Both are [AdRepository]'s, which is why this is a class with no policy in it
 * rather than one that checks anything. What it is told about the user - under the age of consent,
 * and whether ads may be personalized - it applies and does not question.
 *
 * One ad is held at a time, because one is all that is ever asked for: a fullscreen ad belongs to a
 * slot, and a second load would be paying for an ad the app has no moment to show.
 *
 * There is no test-device registration here. A debug build asks for Google's demo units
 * ([TestAdUnits]), which serve test ads to anyone and bill nothing, so there is nothing left for a
 * test-device list to protect. It would matter if a debug build used the real units, which
 * [adUnitId] is written to make impossible.
 */
class GoogleMobileAdsDataSource(private val context: Context) : AdDataSource {

    override val available: Boolean = true

    /** The slot whose ad is loaded and ready, if any. */
    private var loaded: Pair<AdSlot, InterstitialAd>? = null

    override suspend fun preload(slot: AdSlot) {
        val unit = adUnitId(slot, debug = isDebugBuild()) ?: return
        loaded = load(unit)?.let { slot to it }
    }

    override suspend fun show(slot: AdSlot): AdResult {
        val (loadedSlot, ad) = loaded ?: return AdResult.NoFill
        // Dropped either way: an interstitial is shown once, and a second show on the same object
        // does nothing at all.
        loaded = null
        if (loadedSlot != slot) return AdResult.NoFill

        // An interstitial is shown by an activity, and there is not always one in front - the app
        // may be backgrounded between the preload and the moment the ad was wanted. Answering no
        // fill is what lets the user through, which is the right way to fail.
        val activity = currentActivity() ?: return AdResult.NoFill

        return suspendCancellableCoroutine { continuation ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    if (continuation.isActive) continuation.resume(AdResult.Shown)
                }

                // Dismissing is the normal end of an interstitial: it can be closed as soon as the
                // app lets it be. Whether the user watched or skipped is not something this layer
                // could know, and the app does not need to.
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    if (continuation.isActive) continuation.resume(AdResult.NoFill)
                }
            }
            ad.show(activity)
        }
    }

    override suspend fun discard() {
        loaded = null
    }

    /** A load that answers `null` rather than throwing: no fill is an answer, not a failure. */
    private suspend fun load(unit: String): InterstitialAd? = suspendCancellableCoroutine { continuation ->
        InterstitialAd.load(
            context,
            unit,
            adRequest(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    if (continuation.isActive) continuation.resume(ad)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (continuation.isActive) continuation.resume(null)
                }
            },
        )
    }

    /**
     * Google takes this as a request configuration rather than per request: it applies to whatever the
     * SDK sends afterwards, banners included, which is the only way to reach a banner - `AdBanner`
     * asks the SDK itself, without coming through [AdRepository].
     *
     * The content rating follows the band. Under the age of consent it is capped at general audiences
     * as well as the flag being set: an app whose users may be too young to agree on their own keeps
     * what they are shown to the mildest rating Google rates for. A teenager gets the teen rating and
     * *not* the flag, whose general-audiences cap would be wrong for a 17-year-old learner, and an
     * adult gets neither.
     *
     * The tag is deprecated in this SDK version in favour of `setAgeRestrictedTreatment`, whose values
     * this build does not expose (only `UNSPECIFIED` is readable from the artifact). This stays until
     * the replacement can be read from the SDK itself rather than guessed at: the meaning of the flag
     * below is the one Google documents for exactly this case, and a warning is cheaper than being
     * wrong about who is under the age of consent.
     */
    override fun setAudience(audience: AdAudience) {
        val wantedTag = if (audience == AdAudience.Child) {
            RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
        } else {
            RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE
        }
        val wantedRating = when (audience) {
            AdAudience.Child -> RequestConfiguration.MAX_AD_CONTENT_RATING_G
            AdAudience.Teen -> RequestConfiguration.MAX_AD_CONTENT_RATING_T
            AdAudience.Adult -> RequestConfiguration.MAX_AD_CONTENT_RATING_UNSPECIFIED
        }
        val current = MobileAds.getRequestConfiguration()
        if (current.tagForUnderAgeOfConsent == wantedTag && current.maxAdContentRating == wantedRating) return

        // Both are set on every path. A new configuration replaces the whole of the old, so leaving
        // the rating out on the way up would keep a child's rating on an adult's ad requests.
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTagForUnderAgeOfConsent(wantedTag)
                .setMaxAdContentRating(wantedRating)
                .build(),
        )
    }

    /**
     * Recorded for [adRequest], which every request on this platform is built by, banners included.
     * The SDK has no global switch for it the way it has for the age tag, so the answer travels with
     * each request instead.
     */
    override fun setPersonalizedAds(personalized: Boolean) {
        AdPersonalization.personalized = personalized
    }
}

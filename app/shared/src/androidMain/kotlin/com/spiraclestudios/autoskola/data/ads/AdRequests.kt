package com.spiraclestudios.autoskola.data.ads

import android.os.Bundle
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest

/**
 * Whether the next ad request may ask for an ad chosen for this user.
 *
 * Process-wide rather than held by [GoogleMobileAdsDataSource], because a banner is not requested
 * through it: `AdBanner` asks the SDK itself, and this is the one place both paths read the answer
 * from. [AdRepository] writes it before anything is requested and again whenever an answer changes.
 *
 * Starts false, and that is the point: until the app has said the user agreed, a request is for a
 * generic ad. The failure to push an answer in time therefore costs a tailored ad, never a consent.
 */
internal object AdPersonalization {
    @Volatile
    var personalized: Boolean = false
}

/**
 * The request every ad on this platform is made with, so that no path can forget the user's answer.
 *
 * Google's way to ask for a non-personalized ad is the `npa` extra on the AdMob adapter: with it set,
 * the SDK does not use the advertising ID to choose the ad. It still needs some data to deliver one -
 * limiting how often the same ad appears, measuring it, and fraud prevention - which is what the
 * privacy policy says rather than claiming the request is empty.
 */
internal fun adRequest(): AdRequest {
    val builder = AdRequest.Builder()
    if (!AdPersonalization.personalized) {
        val extras = Bundle().apply { putString("npa", "1") }
        builder.addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
    }
    return builder.build()
}

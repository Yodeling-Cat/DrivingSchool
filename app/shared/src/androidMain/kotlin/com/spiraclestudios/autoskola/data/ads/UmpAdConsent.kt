package com.spiraclestudios.autoskola.data.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.spiraclestudios.autoskola.currentActivity
import com.spiraclestudios.autoskola.isDebugBuild
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Google's User Messaging Platform on Android.
 *
 * Deliberately thin, like [GoogleMobileAdsDataSource]: it asks Google, shows what Google says to
 * show, and publishes what it ended up with. Whether to ask and for whom is [AdRepository]'s.
 *
 * **What it starts from.** [canRequestAds] begins at whatever Google already has stored from an
 * earlier launch, because a returning user who has answered can be served at once rather than waiting
 * for the network; [gather] then refreshes it. A failed refresh — offline, or Google unreachable —
 * leaves the stored answer standing instead of switching ads off, which is what Google's own guidance
 * says to do.
 *
 * **Testing the form.** It is only shown in the EEA, the UK and Switzerland, so on a phone anywhere
 * else it never appears. Set [FORCE_EEA_FOR_TESTING] to `true` in a debug build to be treated as in the
 * EEA. Google applies that only to a device it knows, which is what [TEST_DEVICE_HASHED_IDS] is: a
 * device's id is printed to logcat the first time a debug build asks (look for "addTestDeviceHashedId"
 * under the tag `UserMessagingPlatform`), and it is safe to commit, because it names a device and
 * unlocks nothing. Neither does anything in a release build, and [FORCE_EEA_FOR_TESTING] must not be
 * committed on.
 */
class UmpAdConsent(private val context: Context) : AdConsent {

    private val information: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)

    private val canRequest = MutableStateFlow(information.canRequestAds())
    private val privacyOptions = MutableStateFlow(privacyOptionsAreRequired())

    override val canRequestAds: StateFlow<Boolean> = canRequest
    override val privacyOptionsRequired: StateFlow<Boolean> = privacyOptions

    override suspend fun gather(audience: AdAudience) {
        val activity = frontActivity() ?: return
        val parameters = ConsentRequestParameters.Builder()
            // A user under the age of consent is not asked for consent at all, and the form's own
            // setting is how to say so. The SDK does not carry it to the ad requests, which is why
            // `GoogleMobileAdsDataSource.setAudience` sets the same thing there.
            .setTagForUnderAgeOfConsent(audience == AdAudience.Child)
            .apply { debugSettings(activity)?.let { setConsentDebugSettings(it) } }
            .build()

        suspendCancellableCoroutine { continuation ->
            information.requestConsentInfoUpdate(
                activity,
                parameters,
                {
                    // The information is fresh; show the form if it says one is required, and finish
                    // when it is dismissed or when there turned out to be nothing to show.
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                },
                { if (continuation.isActive) continuation.resume(Unit) },
            )
        }
        publish()
    }

    override suspend fun showPrivacyOptions() {
        val activity = frontActivity() ?: return
        suspendCancellableCoroutine { continuation ->
            UserMessagingPlatform.showPrivacyOptionsForm(activity) {
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
        publish()
    }

    /** What the form left, read off Google's own state rather than guessed from how the call ended. */
    private fun publish() {
        canRequest.value = information.canRequestAds()
        privacyOptions.value = privacyOptionsAreRequired()
    }

    private fun privacyOptionsAreRequired(): Boolean =
        information.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /**
     * The activity in front, waiting a little for one: the form is asked for as the app starts, which
     * can be before the first activity has resumed. Ten seconds is long enough for a slow start and
     * short enough that a form with nowhere to show does not hold the ads back forever.
     */
    private suspend fun frontActivity(): Activity? {
        repeat(FRONT_ACTIVITY_ATTEMPTS) {
            currentActivity()?.let { return it }
            delay(FRONT_ACTIVITY_WAIT_MILLIS)
        }
        return null
    }

    /**
     * What makes a debug build testable, and nothing in a release one. The known devices are
     * registered whenever there are any, so a listed device is recognized; the geography is forced only
     * while [FORCE_EEA_FOR_TESTING] is on.
     */
    private fun debugSettings(activity: Activity): ConsentDebugSettings? {
        if (!isDebugBuild()) return null
        if (!FORCE_EEA_FOR_TESTING && TEST_DEVICE_HASHED_IDS.isEmpty()) return null
        return ConsentDebugSettings.Builder(activity)
            .apply {
                if (FORCE_EEA_FOR_TESTING) setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                TEST_DEVICE_HASHED_IDS.forEach { addTestDeviceHashedId(it) }
            }
            .build()
    }

    private companion object {
        const val FRONT_ACTIVITY_ATTEMPTS = 50
        const val FRONT_ACTIVITY_WAIT_MILLIS = 200L

        /** See the class comment. Leave `false` in anything that is committed. */
        const val FORCE_EEA_FOR_TESTING = false

        /** Devices Google recognizes as test devices: the Pixel 9 Pro this was first tried on. */
        val TEST_DEVICE_HASHED_IDS = listOf("55EB73158EB4E63A5C939E2AF919FF4E")
    }
}

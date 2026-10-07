package com.spiraclestudios.autoskola.data.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Google's consent form (the User Messaging Platform, UMP) as the app sees it.
 *
 * **Google requires this, not only the law.** Since January 2024 AdMob serves only limited ads in the
 * EEA, the UK and Switzerland unless a Google-certified consent form is in use, so in the app's main
 * market nothing is requested without it (`plans/ads.md`, "The risk this decision takes"). What the
 * form decides is therefore the first condition on every ad request: [canRequestAds].
 *
 * It is separate from the app's own "Personalized ads" answer on purpose. The form is Google's record
 * of what the user agreed to for Google's purposes and vendors; the switch is the app's, age-gated
 * record of whether the user may be shown tailored ads at all. Neither can widen the other: the SDK
 * reads the form's result itself, and the app can only narrow a request further (`npa=1`).
 *
 * One implementation per platform behind [platformAdConsent]. Whether to ask, and for whom, is
 * [AdRepository]'s decision; this is the form and what it said.
 */
interface AdConsent {

    /**
     * Whether the ad SDK may be asked for an ad, as the form last left it. The first condition on
     * every request, and a flow because it changes while the app runs: the form is answered after the
     * app has started, and the user can withdraw from it later.
     *
     * False until the form has answered, and false on a target with no form — the safe direction.
     */
    val canRequestAds: StateFlow<Boolean>

    /**
     * Whether the user must be offered a way back into the form, which Google requires wherever it
     * shows one. False outside the regions that have a form, so a row about a form that does not exist
     * for the user is never drawn.
     */
    val privacyOptionsRequired: StateFlow<Boolean>

    /**
     * Asks Google what is required for this [audience] and shows the form if it is. Returns when the
     * user has dealt with it, or at once when there was nothing to show.
     *
     * Needs the app to be in front, because the form is a dialog; where it is not, nothing is shown
     * and [canRequestAds] is left as it was. Called at every launch rather than once, because the
     * answer can expire and Google's vendor list changes.
     *
     * For [AdAudience.Child] the form is not shown at all: a user under the age of consent is not
     * asked, and is served as one (see [AdDataSource.setAudience]).
     */
    suspend fun gather(audience: AdAudience)

    /** Reopens the form so the user can change what they agreed to. Only offered when [privacyOptionsRequired]. */
    suspend fun showPrivacyOptions()
}

/**
 * The consent for a target with no form: nothing can be requested, and there is nothing to reopen.
 *
 * What the targets without an ad SDK answer. It is a refusal rather than a pass, so a target that
 * gains an SDK before it gains a form asks for nothing rather than asking without one.
 */
class NoAdConsent : AdConsent {
    override val canRequestAds: StateFlow<Boolean> = MutableStateFlow(false)

    override val privacyOptionsRequired: StateFlow<Boolean> = MutableStateFlow(false)

    override suspend fun gather(audience: AdAudience) = Unit

    override suspend fun showPrivacyOptions() = Unit
}

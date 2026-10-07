package com.spiraclestudios.autoskola.data.ads

/**
 * The ad SDK, one implementation per platform, behind the same rules as everything else that stores
 * or fetches anything: suspended calls, so a request survives the screen that started it.
 *
 * Whether any of these is called is [AdRepository]'s decision, not this one's, and the implementations
 * here are deliberately thin - what the SDK needs, and nothing about the app's policy.
 *
 * There is no banner method. A banner is not asked for and then shown, it is a view: `AdBanner`
 * renders the unit [adUnitId] resolves and the SDK loads it in place, so there is nothing for a
 * repository to decide between the two.
 */
interface AdDataSource {

    /**
     * Whether this target can serve ads at all. False on the web, which has no SDK, and false in
     * [NoOpAdDataSource], which is what the phone targets run until their implementations are
     * written - so the app behaves as it will when ads are unavailable rather than as though they
     * were switched on by accident.
     */
    val available: Boolean

    /** Asks for [slot]'s fullscreen ad ahead of it being needed, so showing it does not wait. */
    suspend fun preload(slot: AdSlot)

    /**
     * Shows the ad [preload] asked for. [AdResult.NoFill] when nothing is loaded or the load failed,
     * which the caller answers by letting the user through.
     */
    suspend fun show(slot: AdSlot): AdResult

    /** Throws away anything loaded, for when ads stop being wanted - a purchase arriving. */
    suspend fun discard()

    /**
     * Starts the SDK. Called by [AdRepository] only once the consent form has said ads may be
     * requested, because Google wants the SDK started after consent and not before it, and more than
     * once is harmless: a second call finds it running. It has a body because a target with no SDK has
     * nothing to start.
     */
    suspend fun initialize() = Unit

    /**
     * Tells the SDK who the ads are for ([AdAudience]): under the age of consent, a teenager, or an
     * adult. Google requires the under-age flag to be in place before anything is requested for such a
     * user, and the content rating follows the band too. Only the app knows the answer, so
     * [AdRepository] pushes it here rather than the SDK deciding.
     *
     * It has a body rather than being abstract because the targets with no ad SDK have nothing to set,
     * and neither does [NoOpAdDataSource]: "cannot serve ads" already means no request is made.
     */
    fun setAudience(audience: AdAudience) = Unit

    /**
     * Tells the SDK whether the ads it requests from now on may be chosen for this user, which is the
     * user's own answer to "personalize your ads?" as far as their age lets it count. False is the
     * state every request starts in and the one anyone who has not agreed is in: the request asks for
     * a generic ad, and the SDK is not to use the advertising ID to pick one.
     *
     * Like [setAudience] it is pushed by [AdRepository] rather than decided here, and for the
     * same reason it has to be in place before the *next* request, banners included - a banner asks
     * the SDK itself, so this is the only way an answer reaches one.
     */
    fun setPersonalizedAds(personalized: Boolean) = Unit
}

package com.spiraclestudios.autoskola.data.ads

import com.russhwolf.settings.Settings
import com.spiraclestudios.autoskola.data.preferences.AgeBand
import com.spiraclestudios.autoskola.data.preferences.ConsentAge
import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository
import com.spiraclestudios.autoskola.data.premium.PremiumRepository
import com.spiraclestudios.autoskola.premium.PremiumFeature
import com.spiraclestudios.autoskola.premium.PremiumRules
import com.spiraclestudios.autoskola.premium.PremiumStatus
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Whether an ad may be asked for at all, and when one is due.
 *
 * Three questions, and every request goes through all of them in this order:
 *
 * 1. **Google's consent form** ([AdConsent]) - the form has said ads may be requested. In the EEA, the
 *    UK and Switzerland that means the user agreed to it, because Google serves nothing worth having
 *    without a certified form; elsewhere the form has nothing to ask and says yes.
 * 2. **The entitlement** ([PremiumRepository]) - nobody who has paid for the ads to stop gets one.
 * 3. **The frequency** ([AdRules]) - every tenth practice test, and never more often than that.
 *
 * **The user's own answer is not on that list, because it does not decide whether an ad is shown.**
 * Ads are what pays for the app, so every user who has not paid for them to stop, and whom the form
 * allows, is shown them; what the user's answer decides is the *kind*: a personalized ad if they
 * agreed to that, a generic one if they did not or are too young for the agreement to count
 * ([personalizedAds]). That is a setting on the SDK rather than a gate in front of it, which is why it
 * is pushed ([pushConsent]) instead of checked.
 *
 * The entitlement is read at the moment of each request rather than remembered from when something
 * was loaded, which is why [showTestOfferAd] throws away a preloaded ad it turns out it may no longer
 * show. A purchase arrives mid-session, and what was bought is the absence of exactly that.
 */
class AdRepository(
    private val dataSource: AdDataSource,
    private val preferences: PreferencesRepository,
    private val premium: PremiumRepository,
    private val settings: Settings,
    private val consent: AdConsent,
) {

    /**
     * The practice-test count the offer was last served at, or `null` if it never was. What the
     * count alone cannot say: a user who watched the ad and whose test was never recorded is still
     * at the count that asked, and would be asked again for an ad they have already sat through.
     */
    private var servedAt: Int? = settings.getIntOrNull(KEY_OFFER_SERVED_AT)

    private val writes = Mutex()

    /**
     * Whether the app may request an ad, as it changes.
     *
     * What a banner placement follows: Premium can be bought while a screen is open, and a placement
     * drawn from a snapshot would keep showing an ad the user had just paid to be rid of.
     */
    val adsAllowed: Flow<Boolean> =
        combine(premium.entitlement, consent.canRequestAds, ::allowed).distinctUntilChanged()

    /**
     * Whether the user must be offered a way back into Google's consent form, which Google requires
     * wherever it shows one. What Settings draws its "ad privacy options" row from.
     */
    val privacyOptionsRequired: StateFlow<Boolean> get() = consent.privacyOptionsRequired

    /**
     * The same answer, read once: what the request paths below ask before asking the SDK anything.
     *
     * Deliberately separate from [adsAllowed] rather than `adsAllowed.value`, because this is the
     * question a caller that is about to act needs answered now, and the flow is the one a screen
     * drawing itself needs to follow.
     */
    fun mayRequestAds(): Boolean = allowed(premium.entitlement.value, consent.canRequestAds.value)

    /**
     * The one place the conditions are put together: the target can serve ads at all, Google's form
     * has said they may be requested, and nobody has paid for them to stop. The user's answer about
     * personalization is not one of them - it changes what is requested, not whether anything is.
     */
    private fun allowed(entitlement: PremiumStatus, canRequestAds: Boolean): Boolean {
        val paid = PremiumRules.isUnlocked(PremiumFeature.NoAds, entitlement)
        return dataSource.available && canRequestAds && !paid
    }

    /**
     * Whether the ads requested for this user may be chosen for them: they said yes, and they are an
     * adult. The age answer is asked here as well as at the switch, because a value stored before the
     * app asked about age - or one a parent or guardian's agreement once unlocked - must not
     * personalize anything on its own ([ConsentAge.effectivePersonalizedAds]).
     */
    fun personalizedAds(): Boolean = preferences.preferences.value.let {
        ConsentAge.effectivePersonalizedAds(it.personalizedAdsEnabled, it.ageBand)
    }

    /**
     * Who the ads are for. An unanswered question is treated as the youngest band, so the child
     * treatment only comes off on an answer the user gave.
     */
    fun audience(): AdAudience = when (preferences.preferences.value.ageBand) {
        AgeBand.Adult -> AdAudience.Adult
        AgeBand.From16To17 -> AdAudience.Teen
        AgeBand.Under16, null -> AdAudience.Child
    }

    init {
        // The SDK has to be told before it is asked for anything, and a banner asks it directly
        // without coming through this class, so this cannot wait for the first request.
        pushConsent()
    }

    /**
     * Tells the ad SDK the two things it needs to know about the user before a request: who the ads
     * are for ([audience]), which Google requires in place before a request is made for a user under
     * the age of consent, and whether the ads may be personalized. This class is the only thing that
     * knows both answers, so they are pushed from here: once when the repository is built, before
     * every request, and from the screens that change either answer, which is what keeps a banner
     * right.
     */
    fun pushConsent() {
        dataSource.setAudience(audience())
        dataSource.setPersonalizedAds(personalizedAds())
    }

    /**
     * Gets the ads ready for this launch: asks Google's consent form what it needs for this user, shows
     * it if it is required, and starts the SDK if the answer is that ads may be requested.
     *
     * Called once the user has been through the app's own consent screen — never before, so nobody is
     * asked about ads before the app has said what it is — and again when something that changes what
     * the form asks has changed ([refreshConsent]). Nothing here is needed for a user who has paid for
     * the ads to stop, or on a target that cannot serve them, so neither is asked a question with no
     * consequence.
     */
    suspend fun prepare() {
        pushConsent()
        if (!dataSource.available || paid()) return
        consent.gather(audience())
        startIfAllowed()
    }

    /**
     * Asks again after the age answer has changed, because it changes what the form says to do: a user
     * who has just turned 16 is no longer one to leave unasked, and the form is shown to them now.
     */
    suspend fun refreshConsent() = prepare()

    /** Reopens Google's form so the user can change what they agreed to, and starts the SDK if that now allows it. */
    suspend fun showPrivacyOptions() {
        consent.showPrivacyOptions()
        startIfAllowed()
    }

    private suspend fun startIfAllowed() {
        if (mayRequestAds()) dataSource.initialize()
    }

    private fun paid(): Boolean = PremiumRules.isUnlocked(PremiumFeature.NoAds, premium.entitlement.value)

    /**
     * Whether the offer is due before the next test: every tenth one, unless it has already been
     * served at this very count.
     */
    fun shouldOfferTest(practiceTestsStarted: Int): Boolean =
        mayRequestAds() && AdRules.offerDue(practiceTestsStarted) && servedAt != practiceTestsStarted

    /**
     * Asks for the offer's ad ahead of the user tapping, so the dialog's button does not wait on a
     * load. *When* to call this is a screen's decision; whether it does anything is not.
     */
    suspend fun preloadTestOffer() {
        pushConsent()
        if (mayRequestAds()) dataSource.preload(AdSlot.TestOfferInterstitial)
    }

    /**
     * Shows the offer's ad.
     *
     * The offer is spent - and the user let through - whether an ad turned up or not, because what
     * the user experiences is the same either way: they waited, and now they are on their way. Not
     * calling this at all is what dismissing the dialog means, and that leaves the offer due, so
     * tapping again asks again.
     */
    suspend fun showTestOfferAd(practiceTestsStarted: Int): AdResult {
        pushConsent()
        if (!mayRequestAds()) {
            dataSource.discard()
            return AdResult.Unavailable
        }
        // A second tap while the first ad is up, or a count that has already been served.
        if (!shouldOfferTest(practiceTestsStarted)) return AdResult.NotDue

        val result = dataSource.show(AdSlot.TestOfferInterstitial)
        if (result == AdResult.Shown || result == AdResult.NoFill) markServed(practiceTestsStarted)
        return result
    }

    /**
     * Shows the ad once a result screen is done with, unless the user's next tap is the gated one
     * (see [AdRules.closingAdYields]) - one fullscreen ad per trip through the app, and the gate is
     * the one worth spending, because it is the one that can be bought out of.
     */
    suspend fun showClosingAd(practiceTestsStarted: Int): AdResult {
        pushConsent()
        if (!mayRequestAds()) {
            dataSource.discard()
            return AdResult.Unavailable
        }
        if (AdRules.closingAdYields(practiceTestsStarted)) return AdResult.NotDue
        return dataSource.show(AdSlot.ResultsInterstitial)
    }

    /**
     * Records that the offer has been served at [practiceTestsStarted]. Written to the end even if
     * the screen that asked for the ad goes away, and one at a time: a marker that did not survive
     * would mean asking a user to watch the same ad twice.
     */
    private suspend fun markServed(practiceTestsStarted: Int) = withContext(NonCancellable) {
        writes.withLock {
            settings.putInt(KEY_OFFER_SERVED_AT, practiceTestsStarted)
            servedAt = practiceTestsStarted
        }
    }

    private companion object {
        const val KEY_OFFER_SERVED_AT = "ads_offer_served_at"
    }
}

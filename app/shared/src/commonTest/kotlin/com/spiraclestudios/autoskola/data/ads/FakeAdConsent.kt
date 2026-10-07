package com.spiraclestudios.autoskola.data.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * An [AdConsent] a test can script: what Google's form will answer when it is asked, and a record of
 * what it was asked.
 *
 * It starts allowing requests, because most tests are not about the form and a gate that starts shut
 * would put an unrelated line in every one of them. The tests that are about it say otherwise.
 */
class FakeAdConsent(
    canRequestAds: Boolean = true,
    privacyOptionsRequired: Boolean = false,
    /** What [canRequestAds] becomes once [gather] has run — the answer the user gives the form. */
    var answerOnGather: Boolean = canRequestAds,
) : AdConsent {

    private val canRequest = MutableStateFlow(canRequestAds)
    private val privacyOptions = MutableStateFlow(privacyOptionsRequired)

    override val canRequestAds: StateFlow<Boolean> = canRequest
    override val privacyOptionsRequired: StateFlow<Boolean> = privacyOptions

    /** The audience of every [gather], in order. */
    val gathered = mutableListOf<AdAudience>()

    /** How many times the privacy options were opened. */
    var privacyOptionsShown = 0
        private set

    /** What [canRequestAds] becomes once the privacy options have been opened. */
    var answerOnPrivacyOptions: Boolean? = null

    override suspend fun gather(audience: AdAudience) {
        gathered += audience
        canRequest.value = answerOnGather
    }

    override suspend fun showPrivacyOptions() {
        privacyOptionsShown++
        answerOnPrivacyOptions?.let { canRequest.value = it }
    }

    /** Whether the way back into the form is required, changed after the fact. */
    fun privacyOptions(required: Boolean) {
        privacyOptions.value = required
    }

    /** The form answering on its own, as it does when the user withdraws from it somewhere else. */
    fun answer(canRequestAds: Boolean) {
        canRequest.value = canRequestAds
    }
}

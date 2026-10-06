package com.spiraclestudios.autoskola.data.preferences

/**
 * What the age answer allows, in one place.
 *
 * The app's questions serve candidates younger than everyone assumed: a moped licence (AM) is taken
 * from 15, and the official tests the app carries are the same pool those candidates are examined
 * on. In Slovakia the age at which a person can agree to crash reports and usage statistics alone is
 * **16**, so below it those switches are not the user's to answer — only a parent or guardian can
 * agree on their behalf, and that agreement is what [parentalConsent] records (`plans/age-gate.md`).
 *
 * **Personalized ads have a different line: 18.** A parent or guardian's agreement does not unlock
 * them, because the restriction is on profiling minors for advertising rather than on who may consent,
 * so [mayPersonalizeAds] looks at the band alone.
 *
 * A rule object rather than a check inside a screen: the switches, the analytics gate and the ad
 * gate all have to answer the same question, and this is the only place that says what the answer is.
 */
object ConsentAge {

    /**
     * Whether the crash-report and usage-statistics switches may be answered at all.
     *
     * An unanswered question (`null`) is not an answer, so nothing may be turned on before the user
     * has said which band they are in. That also covers a just-updated app: the stored switches may
     * say yes while nothing has asked about age yet, which is exactly the state in which sending
     * anything would be wrong.
     */
    fun mayAnswerSwitches(band: AgeBand?, parentalConsent: Boolean): Boolean =
        band == AgeBand.From16To17 || band == AgeBand.Adult || (band == AgeBand.Under16 && parentalConsent)

    /**
     * One of those two answers as it takes effect: what the user asked for, and nothing more than the
     * age answer allows. A switch that says yes under an age that cannot be answered sends nothing.
     */
    fun effective(requested: Boolean, band: AgeBand?, parentalConsent: Boolean): Boolean =
        requested && mayAnswerSwitches(band, parentalConsent)

    /**
     * Whether the personalized-ads switch may be answered at all: only an adult's to give, and
     * nobody's before the question has been answered.
     */
    fun mayPersonalizeAds(band: AgeBand?): Boolean = band == AgeBand.Adult

    /** The personalized-ads answer as it takes effect: what was asked for, if the band allows it. */
    fun effectivePersonalizedAds(requested: Boolean, band: AgeBand?): Boolean = requested && mayPersonalizeAds(band)
}

package com.spiraclestudios.autoskola.data.preferences

/**
 * What the age answer allows, in one place.
 *
 * The app's questions serve candidates younger than everyone assumed: a moped licence (AM) is taken
 * from 15, and the official tests the app carries are the same pool those candidates are examined
 * on. In Slovakia the age at which a person can agree to this kind of processing alone is **16**, so
 * below it the three switches are not the user's to answer — only a parent or guardian can agree on
 * their behalf, and that agreement is what [parentalConsent] records (`plans/age-gate.md`).
 *
 * A rule object rather than a check inside a screen: the switches, the analytics gate and the ad
 * gate all have to answer the same question, and this is the only place that says what the answer is.
 */
object ConsentAge {

    /**
     * Whether the three switches may be answered at all.
     *
     * An unanswered question (`null`) is not an answer, so nothing may be turned on before the user
     * has said which side of 16 they are on. That also covers a just-updated app: the stored switches
     * may say yes while nothing has asked about age yet, which is exactly the state in which sending
     * anything would be wrong.
     */
    fun mayAnswerSwitches(overConsentAge: Boolean?, parentalConsent: Boolean): Boolean =
        overConsentAge == true || (overConsentAge == false && parentalConsent)

    /**
     * One answer as it takes effect: what the user asked for, and nothing more than the age answer
     * allows. A switch that says yes under an age that cannot be answered sends nothing.
     */
    fun effective(requested: Boolean, overConsentAge: Boolean?, parentalConsent: Boolean): Boolean =
        requested && mayAnswerSwitches(overConsentAge, parentalConsent)

    /**
     * Whether Google has to be told this user is under the age of consent where they live, which is
     * what its SDK asks for before it may serve such a user at all.
     *
     * True unless the user has said they are 16 or older, so an unanswered question and an under-16
     * answer are treated alike: the flag only comes off on an answer the user gave, and never by
     * default.
     */
    fun underAgeOfConsent(overConsentAge: Boolean?): Boolean = overConsentAge != true
}

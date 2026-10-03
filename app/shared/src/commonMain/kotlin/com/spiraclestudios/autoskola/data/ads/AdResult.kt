package com.spiraclestudios.autoskola.data.ads

/**
 * What came of asking for an ad.
 *
 * Four cases rather than two, because "no ad" has three different reasons and they are not
 * interchangeable: [NoFill] means an ad was wanted and none turned up, [Unavailable] means the app
 * was not allowed to want one, and [NotDue] means the rules said not this time. Only a [Shown] or a
 * [NoFill] spends the offer the user was waiting on - treating any of the others as a fill would
 * either trap the user behind a dialog that cannot be satisfied, or charge their patience for an ad
 * that was never theirs to watch.
 */
sealed interface AdResult {

    /** An ad was shown, and watched to the end. */
    data object Shown : AdResult

    /**
     * An ad was asked for and none came back: no fill, or a load that failed. The caller has to let
     * the user through, because a user who cannot be shown an ad must never be turned away from the
     * thing the ad was gating.
     */
    data object NoFill : AdResult

    /**
     * Nothing was asked for, because the app is not allowed to ask: the user has not agreed to ads,
     * has bought Premium, or this target has no ad SDK at all.
     */
    data object Unavailable : AdResult

    /** The app may ask, and could serve one, but the rules say not this time. */
    data object NotDue : AdResult
}

package com.spiraclestudios.autoskola

/**
 * Opens the platform's email app on [url] - a `mailto:` address with where it goes, what it is
 * about and the report itself already in it - and answers whether there was anything to open it.
 *
 * The app never sends the mail: the composer opens in front of the user, pre-written, and nothing
 * leaves the device until they send it. The answer is worth having on the phones, where a tablet
 * or an emulator may have no email app at all; a browser hands the address to whatever its system
 * opens for mail, and cannot say either way.
 */
expect fun openEmail(url: String): Boolean

/**
 * Whether this platform is one the app can hand a report to an email app on, which is what decides
 * how the feedback screen lets it leave.
 *
 * The phones say yes: both have an email app they can open a composer in, and can say when they
 * have none. A browser says no, and not because of the user's machine - a page cannot tell whether
 * anything is registered for mail, and the handover is unreliable enough that the report is better
 * written in the mail app the user already has open.
 */
expect fun hasEmailApp(): Boolean

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

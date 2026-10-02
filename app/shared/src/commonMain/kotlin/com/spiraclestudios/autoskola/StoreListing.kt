package com.spiraclestudios.autoskola

/**
 * The app's own page in the store it can be rated in: the store's name as the design writes it,
 * and the address that opens the page.
 */
data class StoreListing(val name: String, val url: String)

/**
 * The page this build can be rated in, or `null` where there is none.
 *
 * Each target answers for itself, the same way [appVersion] and [deviceDescription] do. A browser
 * is served the app rather than installing it, so the web targets have no store and are never
 * asked to rate anything; a platform whose page cannot be addressed yet answers `null` too, which
 * is what keeps the home screen's rating card off it rather than sending the user nowhere.
 */
expect val storeListing: StoreListing?

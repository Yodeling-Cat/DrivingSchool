package com.spiraclestudios.autoskola.data.preferences

/**
 * The version of the privacy policy and the terms the app currently asks the user to accept.
 *
 * Bumping it re-shows the consent screen on the next launch, which is exactly what adding a data
 * flow needs (see `plans/privacy-and-terms.md`). When the document text itself lands in `:core`,
 * this becomes `LegalDocuments.CURRENT_VERSION` and the consent screen renders the text from there
 * rather than only linking to it.
 */
const val CURRENT_LEGAL_VERSION = 1

/**
 * Which published document a link opens.
 *
 * The host is still an open decision (`plans/privacy-and-terms.md`, decision 7) and neither page
 * exists yet, so both point at a placeholder that MUST be replaced before the first store
 * submission — Play, App Store Connect and AdMob all read these URLs.
 */
enum class LegalDocument(val url: String) {
    PrivacyPolicy("https://example.com/privacy"),
    TermsOfUse("https://example.com/terms"),
}

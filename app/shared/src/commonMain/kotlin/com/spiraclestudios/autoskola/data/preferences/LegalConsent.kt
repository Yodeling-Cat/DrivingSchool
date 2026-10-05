package com.spiraclestudios.autoskola.data.preferences

/**
 * The version of the privacy policy and the terms the app currently asks the user to accept.
 *
 * Bumping it re-shows the consent screen on the next launch, which is exactly what adding a data
 * flow needs (see `plans/privacy-and-terms.md`). When the document text itself lands in `:core`,
 * this becomes `LegalDocuments.CURRENT_VERSION` and the consent screen renders the text from there
 * rather than only linking to it.
 *
 * **2 is the release that asks for ads.** The consent screen gained a third switch, and it is a bump
 * rather than a quiet addition because consent has to cover what is actually happening: a user who
 * accepted 1 agreed to two data flows and is now shown ads. Refusing the new switch leaves the app
 * exactly as version 1 left it.
 */
const val CURRENT_LEGAL_VERSION = 2

/**
 * Which published document a link opens.
 *
 * Both are served from the app's own hostname, which is deliberately not the marketing site's:
 * Play, App Store Connect and AdMob all read these URLs, and a store listing that points at a page
 * which moves with the marketing site is a listing that breaks. The two pages are one document
 * each, in the same shape as the rest of that site (`plans/privacy-and-terms.md`, decision 7).
 *
 * They are published in English: the app's four languages are the app's own text, which is a
 * separate job from the site's ("Translation" in the same plan).
 */
enum class LegalDocument(val url: String) {
    PrivacyPolicy("https://autoskola.tree-among-shrubs.com/en/privacy-policy"),
    TermsOfUse("https://autoskola.tree-among-shrubs.com/en/terms-of-service"),
}

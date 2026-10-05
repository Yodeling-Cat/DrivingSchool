package com.spiraclestudios.autoskola.data.preferences

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The documents the consent screen links to are published pages rather than placeholders.
 *
 * It is the one string in the app that nothing else can catch: no code fetches the URL, nothing
 * fails when it is wrong, and the app asks the user to agree to what is behind it before they can
 * continue. `https://example.com/privacy` sat here until the two pages were published, which is the
 * failure this pins (see `plans/privacy-and-terms.md`).
 */
class LegalDocumentTest {

    @Test
    fun everyDocumentIsPublishedOnTheAppsOwnHost() {
        LegalDocument.entries.forEach { document ->
            assertTrue(
                document.url.startsWith("$LEGAL_HOST/en/"),
                "${document.name} must point at the published document, but is ${document.url}",
            )
        }
    }

    /** Two documents, two pages: the consent screen links them separately and they must not collapse. */
    @Test
    fun eachDocumentIsItsOwnPage() {
        assertTrue(LegalDocument.PrivacyPolicy.url.endsWith("/privacy-policy"))
        assertTrue(LegalDocument.TermsOfUse.url.endsWith("/terms-of-service"))
        assertTrue(LegalDocument.PrivacyPolicy.url != LegalDocument.TermsOfUse.url)
    }

    private companion object {
        /** Where the two documents are served from, which is not the marketing site. */
        const val LEGAL_HOST = "https://autoskola.tree-among-shrubs.com"
    }
}

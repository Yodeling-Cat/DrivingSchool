package com.spiraclestudios.autoskola.feedback

import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.i18n.Strings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The report and the address it is handed to: what the user wrote, what the app adds to it, and
 * the one shape all three platforms understand.
 */
class FeedbackMailTest {

    private val english: Strings = AppLanguage.English.strings

    @Test
    fun theReportSaysWhatWasWrittenAndWhichBuildItCameFrom() {
        assertEquals(
            "The timer keeps counting after I finish.\n\nApp version: 1.4.2",
            feedbackBody(english, "The timer keeps counting after I finish.", device = null, version = "1.4.2"),
        )
    }

    @Test
    fun theDeviceGoesInOnlyWhenTheUserAskedForIt() {
        val without = feedbackBody(english, "Hello", device = null, version = "1.4.2")
        assertFalse("Pixel" in without, without)

        assertEquals(
            "Hello\n\nApp version: 1.4.2\nDevice: Pixel 7 · Android 14",
            feedbackBody(english, "Hello", device = "Pixel 7 · Android 14", version = "1.4.2"),
        )
    }

    @Test
    fun aBuildWithNoVersionLeavesTheLineOutRatherThanEmpty() {
        // The web build: it is whatever was deployed last, and reports no version of its own.
        assertEquals("Hello", feedbackBody(english, "Hello", device = null, version = null))
        assertEquals("Hello\n\nDevice: a browser", feedbackBody(english, "Hello", device = "a browser", version = null))
    }

    @Test
    fun theMessageLeadsTheReportAndTheFactsFollowIt() {
        assertEquals(
            "Hello there\n\nApp version: 1.0\nDevice: iPhone · iOS 17.4",
            feedbackBody(english, "  Hello there  ", device = "iPhone · iOS 17.4", version = "1.0"),
        )
    }

    @Test
    fun theAddressIsTheOneWeRead() {
        val url = feedbackMailto(english, "Hello", device = null, version = null)
        assertTrue(url.startsWith("mailto:anima@tree-among-shrubs.com?subject="), url)
    }

    @Test
    fun theSubjectSaysWhatTheMailIs() {
        val url = feedbackMailto(english, "Hello", device = null, version = null)
        assertEquals(
            "subject=Feedback%3A%20Naj%20Auto%C5%A1kola",
            url.substringAfter('?').substringBefore('&'),
            url,
        )
    }

    @Test
    fun whatWouldBreakTheAddressIsEncoded() {
        val url = feedbackMailto(english, "A ? and a # and an & and a newline\nand š", device = null, version = null)
        val body = url.substringAfter("&body=")

        // Nothing that would end the query early, or read as a character that was meant to be
        // escaped, survives into it; the accent travels as its UTF-8 bytes.
        assertFalse('&' in body, body)
        assertFalse('\n' in body, body)
        assertFalse(' ' in body, body)
        assertTrue("%3F" in body, body)
        assertTrue("%23" in body, body)
        assertTrue("%26" in body, body)
        assertTrue("%C5%A1" in body, body)
    }

    @Test
    fun theDetailsAreTheBuildAndTheDeviceItCameFrom() {
        assertEquals("App version: 1.4.2", feedbackDetails(english, device = null, version = "1.4.2"))
        assertEquals(
            "App version: 1.4.2\nDevice: Pixel 7 · Android 14",
            feedbackDetails(english, device = "Pixel 7 · Android 14", version = "1.4.2"),
        )
        // The web build: no version of its own, and a device only if the user hands it over.
        assertEquals("Device: a browser", feedbackDetails(english, device = "a browser", version = null))
    }

    @Test
    fun aPlatformWithNothingToSayAboutItselfHasNoDetails() {
        assertEquals("", feedbackDetails(english, device = null, version = null))
    }

    @Test
    fun theBodyIsTheMessageAndThoseVerySameDetails() {
        AppLanguage.entries.forEach { language ->
            val strings = language.strings
            val details = feedbackDetails(strings, device = "A device", version = "9.9")

            assertTrue(details.isNotEmpty(), language.name)
            assertEquals("Hello\n\n$details", feedbackBody(strings, "Hello", device = "A device", version = "9.9"))
        }
    }

    @Test
    fun everyLanguageNamesTheAppAndTheBuildInTheReport() {
        AppLanguage.entries.forEach { language ->
            val strings = language.strings
            assertTrue(strings.appName in strings.feedbackSubject(strings.appName), language.name)

            val body = feedbackBody(strings, "Hello", device = "A device", version = "9.9")
            assertTrue("9.9" in body, "${language.name}: $body")
            assertTrue("A device" in body, "${language.name}: $body")
        }
    }

    @Test
    fun everyLanguageGivesAnAddressThatCarriesItAll() {
        AppLanguage.entries.forEach { language ->
            val url = feedbackMailto(language.strings, "Hello", device = null, version = "9.9")

            assertTrue(url.startsWith("mailto:anima@tree-among-shrubs.com?subject="), url)
            assertTrue(url.endsWith("9.9"), url)
            // The app's name is translated, and one of those translations carries an accent: a
            // raw space or line break anywhere in the address is one the platform may cut off.
            assertFalse(' ' in url, url)
            assertFalse('\n' in url, url)
        }
    }
}

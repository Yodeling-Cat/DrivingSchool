package com.spiraclestudios.autoskola.feedback

import com.spiraclestudios.autoskola.i18n.Strings

/** Where a feedback report is sent: the one address the app ever opens. */
internal const val FEEDBACK_ADDRESS = "anima@tree-among-shrubs.com"

/**
 * The address the platform's email app is asked to open, with everything already in it: where it
 * goes, what it is about, and the report below.
 *
 * A `mailto:` address rather than an intent of the app's own, because it is the one shape all
 * three platforms understand: Android and iOS hand it to the app registered for mail, and a
 * browser gives it to whatever the system opens. The app never sends it - the composer belongs to
 * the user, and nothing leaves the device until they send it themselves.
 */
internal fun feedbackMailto(
    strings: Strings,
    message: String,
    device: String?,
    version: String?,
    identifier: String?,
    kind: FeedbackKind,
): String = "mailto:$FEEDBACK_ADDRESS" +
    "?subject=${encode(feedbackSubject(strings, kind))}" +
    "&body=${encode(feedbackBody(strings, message, device, version, identifier))}"

/**
 * What the mail is called, which is also what sorts it in the inbox: a report, or a request about
 * the user's own data.
 */
internal fun feedbackSubject(strings: Strings, kind: FeedbackKind): String = when (kind) {
    FeedbackKind.Feedback -> strings.feedbackSubject(strings.appName)
    FeedbackKind.PrivacyRequest -> strings.feedbackRequestSubject(strings.appName)
}

/**
 * The message the email app opens with: what the user wrote, and under it what the app reports
 * about itself.
 *
 * The version goes in either way, because a report we cannot place against a build is one we
 * cannot act on. The device line only goes in when the user asked for it, and a platform that has
 * nothing to report leaves its line out rather than sending an empty one. [identifier] is passed by
 * the screen, which is where the rule about who gets one lives (see `FeedbackUiState`).
 */
internal fun feedbackBody(
    strings: Strings,
    message: String,
    device: String?,
    version: String?,
    identifier: String? = null,
): String = listOf(message.trim(), feedbackDetails(strings, device, version, identifier))
    .filter { it.isNotEmpty() }
    .joinToString("\n\n")

/**
 * What the report says about itself, one line each: the build it came from, the device it was
 * written on when the user asked for it, and the identifier a request about their own data is about.
 *
 * A browser's user is told to paste this into the email they write themselves, so it is also what
 * the screen shows them and what the copy button takes. It is empty when a platform has nothing to
 * name and the device was left out, which is a case the screen is written for rather than a bug.
 */
internal fun feedbackDetails(
    strings: Strings,
    device: String?,
    version: String?,
    identifier: String? = null,
): String = listOfNotNull(
    version?.let(strings.feedbackVersionLine),
    device?.let(strings.feedbackDeviceLine),
    identifier?.let(strings.feedbackIdentifierLine),
).joinToString("\n")

/**
 * Percent-encodes [value] for the query of a `mailto:` address: everything but the unreserved
 * characters, so a newline, an `&` or an accent cannot end the subject or the message early, or
 * arrive as a character that was meant to be escaped.
 */
private fun encode(value: String): String = buildString {
    for (byte in value.encodeToByteArray()) {
        val code = byte.toInt() and 0xFF
        val character = code.toChar()
        if (character in UNRESERVED) {
            append(character)
        } else {
            append('%')
            append(HEX_DIGITS[code shr 4])
            append(HEX_DIGITS[code and 0x0F])
        }
    }
}

/** The characters a URL may carry as themselves; everything else is escaped (RFC 3986). */
private val UNRESERVED: Set<Char> = (('A'..'Z') + ('a'..'z') + ('0'..'9')).toSet() + "-._~".toSet()

private const val HEX_DIGITS = "0123456789ABCDEF"

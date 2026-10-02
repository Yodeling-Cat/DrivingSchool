@file:Suppress("MatchingDeclarationName")

package com.spiraclestudios.autoskola

/**
 * A page opens nothing: `mailto:` goes to whatever the system happens to have registered for mail,
 * and often nothing does, so the feedback screen does not offer a press here at all - it says where
 * to write and offers the report as text to copy ([hasEmailApp]).
 */
actual fun openEmail(url: String): Boolean = false

/** A page has no email app of its own to open; the user writes the mail in theirs. */
actual fun hasEmailApp(): Boolean = false

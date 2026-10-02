package com.spiraclestudios.autoskola.data.analytics

import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.results.hasVerdict
import com.spiraclestudios.autoskola.stats.AttemptKind

// The properties the events carry are built here rather than at each call site, so the shapes are
// decided once and can be unit tested without a ViewModel.
//
// Nothing here may name an attempt, a question, a test number, an answer or anything the user
// typed: counts, booleans, durations, enum names and bank ids only (see `plans/privacy-and-terms.md`).

/** The `consent_answered` choice for the button that answers both questions in one press. */
internal const val CONSENT_CHOICE_ACCEPT_ALL = "accept_all"

/** The `consent_answered` choice for the button that keeps the switches as they stand. */
internal const val CONSENT_CHOICE_ACCEPT_CHOSEN = "accept_chosen"

/** The `consent_changed` switch for the crash-reporting answer. */
internal const val CONSENT_SWITCH_CRASH_REPORTING = "crash_reporting"

/** The `consent_changed` switch for the analytics answer. */
internal const val CONSENT_SWITCH_ANALYTICS = "analytics"

/**
 * The wire name of [AttemptKind], kept apart from the constant's own name so renaming a Kotlin
 * enum is a refactor while changing this splits the event's history in the dashboard.
 */
internal fun AttemptKind.wireName(): String = when (this) {
    AttemptKind.PracticeTest -> "practice_test"
    AttemptKind.Mistakes -> "mistakes"
    AttemptKind.Situations -> "situations"
}

/** The properties `test_started` carries: what was started, and whether it was continued. */
internal fun testStartedProperties(kind: AttemptKind, resumed: Boolean): Map<String, Any> =
    mapOf("kind" to kind.wireName(), "resumed" to resumed)

/**
 * The properties `test_completed` carries.
 *
 * `passed` says how a kind that has a verdict went against the pass mark, and is left out
 * altogether for one that doesn't: a situations drill is neither passed nor failed, and sending
 * `false` would read in the dashboard as a drill that always fails (see [hasVerdict]).
 */
internal fun testCompletedProperties(
    kind: AttemptKind,
    durationSeconds: Long,
    wrongCount: Int,
    passed: Boolean,
): Map<String, Any> = buildMap {
    put("kind", kind.wireName())
    put("duration_seconds", durationSeconds)
    put("wrong_count", wrongCount)
    if (hasVerdict(kind)) put("passed", passed)
}

/** The properties `test_abandoned` carries: what was left, with how much done in how long. */
internal fun testAbandonedProperties(kind: AttemptKind, answersGiven: Int, durationSeconds: Long): Map<String, Any> =
    mapOf(
        "kind" to kind.wireName(),
        "answers_given" to answersGiven,
        "duration_seconds" to durationSeconds,
    )

/** The properties `bank_missing` carries: the bank that could no longer be resolved. */
internal fun bankMissingProperties(bankId: String): Map<String, Any> = mapOf("bank_id" to bankId)

/** The properties `situation_opened` carries: which situation was opened. */
internal fun situationOpenedProperties(number: Int): Map<String, Any> = mapOf("number" to number)

/** The properties `consent_answered` carries: the answers committed, and the button pressed. */
internal fun consentAnsweredProperties(crashReporting: Boolean, analytics: Boolean, choice: String): Map<String, Any> =
    mapOf(
        "crash_reporting" to crashReporting,
        "analytics" to analytics,
        "choice" to choice,
    )

/** The properties `consent_changed` carries: which switch, and what it was set to. */
internal fun consentChangedProperties(switch: String, enabled: Boolean): Map<String, Any> =
    mapOf("switch" to switch, "enabled" to enabled)

/** The properties `language_changed` carries: the language picked, as its code. */
internal fun languageChangedProperties(to: AppLanguage): Map<String, Any> = mapOf("to" to to.code)

/**
 * Records a language change, or nothing when [to] is the language already in use: picking the
 * language the app is already in is not a change, so it is not recorded as one.
 *
 * Called before the preference is written, so the comparison is against the language being left.
 */
internal fun AnalyticsRepository.languageChanged(from: AppLanguage, to: AppLanguage) {
    if (from == to) return
    capture(AnalyticsEvent.LanguageChanged, languageChangedProperties(to))
}

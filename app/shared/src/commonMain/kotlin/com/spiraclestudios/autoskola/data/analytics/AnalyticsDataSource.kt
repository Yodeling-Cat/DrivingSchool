package com.spiraclestudios.autoskola.data.analytics

/**
 * The events we record, named once so a call site cannot invent a new one by typo.
 *
 * The Kotlin name and the wire name are deliberately separate: renaming a constant here is a
 * refactor, while changing [eventName] splits that event's history in the dashboard, so the two
 * should not be able to drift by accident.
 */
enum class AnalyticsEvent(val eventName: String) {
    /** The user accepted the disclaimer and the terms, which is also what turns analytics on. */
    OnboardingCompleted("onboarding_completed"),

    /** The user picked the exam they are preparing for, at the end of the selection wizard. */
    ExamSelected("exam_selected"),
}

/**
 * Where events go. Write-only: nothing reads analytics back, and an implementation must never
 * throw into the app, because a dropped event is not a failed test attempt.
 */
interface AnalyticsDataSource {

    /** Records [event] with [properties]. */
    fun capture(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap())

    /** Records that a screen was shown, so navigation funnels can be read later. */
    fun screen(name: String)

    /**
     * Turns recording on or off. Consent is the app's decision (see [AnalyticsRepository]); the
     * data source only obeys it.
     */
    fun setEnabled(enabled: Boolean)
}

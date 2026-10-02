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

    /** A test, a mistakes retest or a situations drill was started or continued. */
    TestStarted("test_started"),

    /** A test or a mistakes retest was finished and scored. */
    TestCompleted("test_completed"),

    /** An attempt was left unfinished - discarded, or walked away from with answers in it. */
    TestAbandoned("test_abandoned"),

    /** The user answered the consent screen by pressing one of its two buttons. */
    ConsentAnswered("consent_answered"),

    /** The user changed one of the two consent switches in Settings. */
    ConsentChanged("consent_changed"),

    /** The user picked a language different from the one in use. */
    LanguageChanged("language_changed"),

    /** A situation was opened from the intersections list, which only the screen view can't say. */
    SituationOpened("situation_opened"),

    /** The selected question bank no longer exists, e.g. an update removed it. */
    BankMissing("bank_missing"),

    /** A question's picture could not be loaded. */
    QuestionImageFailed("question_image_failed"),
}

/**
 * The super property that says which build an event came from, and the two values it takes.
 *
 * PostHog's free plan allows one project, so a development build cannot have one of its own and
 * its events cannot be kept out of the real users' by where they are sent. It is tagged instead,
 * and filtered out of an insight by hand — which is why the property has to be on every event,
 * including a crash report.
 */
const val ANALYTICS_ENVIRONMENT_PROPERTY = "environment"

/** The value of [ANALYTICS_ENVIRONMENT_PROPERTY] for a build a developer runs. */
const val ANALYTICS_ENVIRONMENT_DEVELOPMENT = "debug"

/** The value of [ANALYTICS_ENVIRONMENT_PROPERTY] for a build a user has. */
const val ANALYTICS_ENVIRONMENT_RELEASE = "release"

/**
 * The super property that says which platform an event came from: the same value on every event,
 * so a platform's numbers can be read on their own. The web targets both answer "web".
 */
const val ANALYTICS_PLATFORM_PROPERTY = "platform"

/**
 * The super property that says which version of the app sent an event. Left off where the platform
 * reports no version rather than sent empty.
 */
const val ANALYTICS_APP_VERSION_PROPERTY = "app_version"

/** The environment a build reports itself as, from whether it is a developer's build. */
internal fun analyticsEnvironment(developmentBuild: Boolean): String =
    if (developmentBuild) ANALYTICS_ENVIRONMENT_DEVELOPMENT else ANALYTICS_ENVIRONMENT_RELEASE

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
     * Told what the user has consented to — two separate answers, because they are two separate
     * questions. Reading consent is the app's job (see [AnalyticsRepository]); the data source only
     * obeys it.
     *
     * @param crashReporting unhandled exceptions may be reported
     * @param analytics events we record on purpose may be sent
     */
    fun setConsent(crashReporting: Boolean, analytics: Boolean)
}

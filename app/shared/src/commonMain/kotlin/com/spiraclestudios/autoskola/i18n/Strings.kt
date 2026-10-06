package com.spiraclestudios.autoskola.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.intl.Locale

/**
 * Languages the app UI is translated into. These are the languages we have tests in, plus Czech.
 *
 * UI strings are typed Kotlin tables (see [Strings]) rather than Compose resources so the
 * language can be switched at runtime on every platform without per-platform locale hacks.
 */
enum class AppLanguage(val code: String, val nativeName: String, val strings: Strings) {
    English("en", "English", EnglishStrings),
    Slovak("sk", "Slovenčina", SlovakStrings),
    Hungarian("hu", "Magyar", HungarianStrings),
    Czech("cs", "Čeština", CzechStrings),
    ;

    companion object {
        fun fromCode(code: String?): AppLanguage? = entries.firstOrNull { it.code == code }

        /** The language for a device locale language code: that language if supported, else English. */
        fun forLocale(languageCode: String): AppLanguage = fromCode(languageCode) ?: English

        fun system(): AppLanguage = forLocale(Locale.current.language)
    }
}

val LocalStrings = staticCompositionLocalOf<Strings> { EnglishStrings }

/** Shorthand for the current language's strings. */
val strings: Strings
    @Composable @ReadOnlyComposable
    get() = LocalStrings.current

class Strings(
    val appName: String,
    // Disclaimer
    val disclaimerTitleBefore: String,
    val disclaimerTitleAccent: String,
    val disclaimerIndependent: String,
    val disclaimerSource: String,
    val disclaimerAccept: String,
    // Consent. The three switches are separate answers, and the documents are linked rather than
    // summarized: a user cannot agree to what they cannot read.
    val consentTitle: String,
    val consentSubtitle: String,
    /**
     * The question the three switches depend on: under 16 the answers are not the user's to give, so
     * this comes before either button can commit anything (`plans/age-gate.md`).
     */
    val consentAgeQuestion: String,
    val consentAgeOver16: String,
    val consentAgeUnder16: String,
    /** Why the switches cannot be answered yet, drawn with the guardian's agreement. */
    val consentAgeUnder16Reason: String,
    val consentParentalConsent: String,
    /** The question behind the one press that unlocks three switches. */
    val settingsAgeConfirmTitle: String,
    val settingsAgeConfirmMessage: String,
    val settingsAgeConfirmAction: String,
    val settingsAgeConfirmDismiss: String,
    /** The one switch whose off position costs the app rather than the user. */
    val settingsAdsOffTitle: String,
    val settingsAdsOffMessage: String,
    val settingsAdsOffAction: String,
    val settingsAdsKeep: String,
    val consentParentalConsentDescription: String,
    val consentCrashReportingTitle: String,
    val consentCrashReportingDescription: String,
    val consentAnalyticsTitle: String,
    val consentAnalyticsDescription: String,
    val consentAdsTitle: String,
    val consentAdsDescription: String,
    val consentAgree: String,
    /** Announced rather than drawn: what a screen reader hears when the agreement is missing. */
    val consentAgreeRequired: String,
    val legalPrivacyPolicy: String,
    val legalTermsOfUse: String,
    val consentAcceptAll: String,
    val consentAcceptChosen: String,
    /** The Settings section that carries the same two answers, so they can be changed later. */
    val settingsPrivacy: String,
    // Exam selection. Titles carry the word that the design sets in coral italic separately, so a
    // translation can put it wherever its grammar wants instead of guessing from the sentence.
    val examSelectionCountryTitleBefore: String,
    val examSelectionCountryTitleAccent: String,
    val examSelectionCountrySubtitle: String,
    val examSelectionCountryComingSoon: String,
    val examSelectionLanguageTitleBefore: String,
    val examSelectionLanguageTitleAccent: String,
    val examSelectionLanguageSubtitle: String,
    val examSelectionCategoryTitleBefore: String,
    val examSelectionCategoryTitleAccent: String,
    val countryName: (code: String) -> String,
    // Navigation
    val navTests: String,
    val navLearn: String,
    val navSettings: String,
    val back: String,
    // Learn
    val learnTitle: String,
    val learnRoadSigns: String,
    val learnRoadSignsSubtitle: String,
    val learnIntersections: String,
    val learnIntersectionsSubtitle: String,
    val learnLegislation: String,
    val learnLegislationSubtitle: String,
    /** Said of a learning subject that has no content yet. */
    val learnComingSoon: String,
    // Road signs
    val signsSearchPlaceholder: String,
    val clearSearch: String,
    val signsCount: (count: Int) -> String,
    /** How many signs a search is showing, out of the whole catalog. */
    val signsMatches: (matched: Int, total: Int) -> String,
    val signsNothingFound: String,
    /** The bar action that folds every group away, leaving its heading. */
    val signsCollapseAll: String,
    /** The bar action that opens every group again. */
    val signsExpandAll: String,
    /** On a group's heading: folds it away, leaving the heading. */
    val signsCollapseGroup: String,
    /** On a group's heading: opens its signs. */
    val signsExpandGroup: String,
    // Intersections
    /** How many situations at intersections the material holds. */
    val situationsCount: (count: Int) -> String,
    /** What the screen calls the situation it is showing, e.g. "Situation 3". */
    val situationName: (number: Int) -> String,
    /** How many questions are asked about one situation. */
    val situationQuestions: (count: Int) -> String,
    /** Shown for an exam whose bank asks about no situations at all. */
    val situationsNothingFound: String,
    /** The action that drills every situation, in the practice screen. */
    val situationsDrillAll: String,
    /** What a run through the situations is called in the practice bar, e.g. "Situations drill". */
    val situationsDrillName: String,
    /** The list's overflow action: clear what has been answered about the situations. */
    val situationsResetAnswers: String,
    /** What the prompt standing between that action and the answers asks. */
    val situationsResetTitle: String,
    /** The prompt's own words: what the driver is about to give up. */
    val situationsResetMessage: String,
    /** The highlighted answer to the prompt: the answers go. */
    val situationsResetConfirm: String,
    /** The quiet answer to it: they stay. */
    val situationsResetKeep: String,
    /** What one question about a situation is called on its tab, e.g. "Question 2". */
    val questionNumber: (number: Int) -> String,
    /** Read out with the green mark: a question, or a situation, answered correctly. */
    val answeredCorrectly: String,
    /** The same for the red cross: a question answered wrongly. */
    val answeredWrongly: String,
    // Home
    val change: String,
    /** The words beside the count of finished tests, e.g. "tests finished". */
    val testsFinishedLabel: String,

    /** The label over the progress card, e.g. "driver in training". */
    val progressEyebrow: String,
    val progressEyebrowPassed: String,
    val passed: String,
    val failed: String,
    val averageScore: String,

    /** Short label for the statistics row on the license card. */
    val averageShort: String,

    val randomTest: String,

    /** The same pill's label where the row has no room for the noun: a phone keeps just the adjective. */
    val randomTestShort: String,
    val notPassed: String,
    val allTests: String,
    val notStarted: String,
    val resultSummary: (score: Int, max: Int, passed: Boolean) -> String,
    val testName: (number: Int) -> String,
    val unfinishedTest: String,
    val resumeSummary: (answered: Int, total: Int) -> String,
    val continueTest: String,
    val resumePromptTitle: String,
    // The every-tenth-test gate. It names the test the user asked for, because the dialog is the only
    // place they are told why nothing happened when they tapped it.
    val adGateTitle: String,
    val adGateMessage: (testNumber: Int) -> String,
    val adGateWatch: String,
    val adGateDismiss: String,
    val startOver: String,

    /** What the prompt says when the picked test has already been passed. */
    val passedPromptTitle: String,

    /** The same prompt's words, with the score that redoing the test would give up. */
    val passedPromptMessage: (score: Int, max: Int) -> String,

    /** The quiet answer to it: keep the score the test already has. */
    val keepScore: String,
    val inProgress: String,
    val practiceMistakes: String,
    val mistakesToReview: (count: Int) -> String,
    val mistakesTestName: String,

    /** The card asking the user to rate the app in the store, which sits under the mistakes card. */
    val rateCardTitle: String,

    /** Its words, naming the store the user is asked to rate the app in: "the Play Store". */
    val rateCardMessage: (store: String) -> String,
    val rateCardAction: String,

    /** What the card's cross says to a screen reader, which is all it ever says. */
    val rateCardDismiss: String,
    val questionsAnswered: String,
    // Practice
    /** The question the practice bar is on, on its own: e.g. "7". */
    val questionCounter: (number: Int) -> String,

    /** How many the test has, drawn dimmed with the slash that counts them off: e.g. "/ 50". */
    val questionCount: (total: Int) -> String,

    /** E.g. "6 answered": the questions of the test that carry an answer. */
    val answeredCount: (count: Int) -> String,

    /** E.g. "1 skipped": the questions the test was left with open. */
    val skippedCount: (count: Int) -> String,
    val points: (count: Int) -> String,
    val questionList: String,
    val closeQuestionList: String,
    val previousQuestion: String,
    val nextQuestion: String,
    /** The same steps for the situations, which the detail screen moves between the same way. */
    val previousSituation: String,
    val nextSituation: String,
    val moreOptions: String,
    val finishTest: String,

    /** Debug builds only: the practice screen's overflow menu fills the test in. */
    val answerAllRandomly: String,
    val answerAllCorrectly: String,
    val yourAnswer: String,
    val correctAnswer: String,
    val enlargeImage: String,
    val closeImage: String,
    val skippedQuestion: String,
    val allQuestionsAnswered: String,
    val overTime: (minutes: Int) -> String,
    val finish: String,
    val testFinished: String,
    val showResults: String,
    val leaveTestTitle: String,
    val leaveTestMessage: String,
    val leaveTest: String,
    val keepTesting: String,
    // Results
    val resultsTitle: String,

    /** The word the card's stamp carries, as a stamp is: one word, not a sentence. */
    val passedStamp: String,

    /** The same word for a test that came in under the pass mark. */
    val failedStamp: String,
    val correctCount: String,
    val wrongCount: String,
    val unansweredCount: String,
    val passMark: (percent: Int) -> String,

    /** The same pass mark, set beside the score on the result card. */
    val passMarkShort: (percent: Int) -> String,

    /** The words beside that score, as the license card sets "tests finished": "points". */
    val pointsLabel: String,

    /** How long the test ran, set at the card's other corner: "23 min". */
    val minutesShort: (minutes: Int) -> String,
    val bySection: String,
    val reviewAnswers: String,
    val backToTests: String,
    /** The same way out for an attempt begun elsewhere: back to the situations a drill came from. */
    val backToList: String,
    /** What the prompt says when a drill of the situations is already under way. */
    val drillPromptTitle: String,
    // Settings
    val settingsTitle: String,
    val settingsLanguage: String,
    val settingsAnswers: String,
    val settingsCheckImmediately: String,
    val settingsCheckImmediatelyDescription: String,
    val settingsAdvanceAfterAnswer: String,
    val settingsAdvanceAfterAnswerDescription: String,
    val settingsExam: String,
    val settingsChangeExam: String,
    // About
    /** The screen's own title. */
    val aboutTitle: String,
    /** The Settings section the row that opens the screen sits under. */
    val settingsApp: String,
    /** What the screen, and the row in Settings that opens it, are both called. */
    val aboutApp: String,
    /** Which build the user is running, e.g. "Version 1.0". */
    val aboutVersion: (version: String) -> String,
    /**
     * The row that shows the identifier our statistics and crash reports are filed under. Named for
     * the device rather than the person, because a device is all it names.
     */
    val settingsDeviceIdentifier: String,
    /** What the identifier is for, drawn under the row's title while there is one to show. */
    val settingsDeviceIdentifierDescription: String,
    /** Drawn instead of an identifier, and the reason there is not one to show. */
    val settingsDeviceIdentifierEmpty: String,
    /** The heading over the list of the projects the app is built from. */
    val aboutLibraries: String,
    val aboutLibrariesIntro: String,
    val aboutLibrariesSearch: String,
    val aboutLibrariesNothingFound: String,
    // Feedback
    /** The screen's own title, and the row in Settings that opens it. */
    val feedbackTitle: String,
    /** What the screen is for: the sentence under the title. */
    val feedbackIntro: String,
    /** Where to write to us instead, on a platform whose report the user sends themselves. */
    val feedbackSendTo: (address: String) -> String,
    /** What the empty message field says. */
    val feedbackMessageHint: String,
    /** The two things a user can write to us about, as the picker offers them. */
    val feedbackKindFeedback: String,
    val feedbackKindPrivacyRequest: String,
    /** What the empty field asks for when it is a request about data rather than a report. */
    val feedbackRequestHint: String,
    /** What a request carries that a report does not, and what it means when there is no identifier. */
    val feedbackPrivacyRequestNote: String,
    val feedbackPrivacyRequestNoIdentifier: String,
    /** The answer to whether the report carries the device it was written on. */
    val feedbackIncludeDeviceInfo: String,
    val feedbackIncludeDeviceInfoDescription: String,
    val feedbackSend: String,
    /** Announced rather than drawn: see the app's feedback-and-semantics notes. */
    val feedbackMessageMissing: String,
    /** The platform has no email app to open, which is said rather than silently ignored. */
    val feedbackNoEmailApp: String,
    /** The heading over the block a browser's user pastes into their own email. */
    val feedbackIncludeDetails: String,
    /** The press that puts that block on the clipboard, and what it says once it has. */
    val feedbackCopy: String,
    val feedbackCopied: String,
    /** The subject of the report, which names the app: "Feedback: Naj Autoškola". */
    val feedbackSubject: (appName: String) -> String,
    /** The subject of a request about the user's own data: "Privacy request: Naj Autoškola". */
    val feedbackRequestSubject: (appName: String) -> String,
    /** The build the report came from, which every report carries: "App version: 1.0". */
    val feedbackVersionLine: (version: String) -> String,
    /** What the report was written on: "Device: Pixel 7 · Android 14". */
    val feedbackDeviceLine: (device: String) -> String,
    /** The identifier a request carries, when the app has one to quote. */
    val feedbackIdentifierLine: (identifier: String) -> String,
)

/** Slovak and Czech plurals: 1 / 2–4 / 5+ (and 0). */
private fun westSlavicPlural(
    count: Int,
    one: String,
    few: String,
    many: String,
) = when (count) {
    1 -> one
    in 2..4 -> few
    else -> many
}

private val EnglishStrings = Strings(
    appName = "Naj Autoškola",
    disclaimerTitleBefore = "Before you",
    disclaimerTitleAccent = "start",
    disclaimerIndependent = "Naj Autoškola is an independent app. It is not made by, affiliated with, " +
        "or endorsed by any government authority.",
    disclaimerSource = "The questions and pictures come from the official question bank published by " +
        "the Ministry of Interior of the Slovak Republic (minv.sk). We copy them as published, but if " +
        "anything differs, the official exam and current law apply.",
    disclaimerAccept = "I understand",
    consentTitle = "Your data, your choice",
    consentSubtitle = "This app can show ads, and can send two things to help us fix it and " +
        "improve it.",
    consentAgeQuestion = "Are you 16 or older?",
    consentAgeOver16 = "16 or older",
    consentAgeUnder16 = "Under 16",
    consentAgeUnder16Reason = "Under 16 you cannot agree to these yourself: only a parent or guardian " +
        "can agree on your behalf.",
    consentParentalConsent = "A parent or guardian has agreed to this on my behalf",
    settingsAgeConfirmTitle = "Have you turned 16?",
    settingsAgeConfirmMessage = "Answering yes unlocks the three switches below, so please answer " +
        "only if it is true.",
    settingsAgeConfirmAction = "Yes, I am 16 or older",
    settingsAgeConfirmDismiss = "Not yet",
    settingsAdsOffTitle = "Turn the ads off?",
    settingsAdsOffMessage = "The app is free because of the ads. Turning them off removes the " +
        "adverts and the revenue that keeps it running.",
    settingsAdsOffAction = "Turn the ads off",
    settingsAdsKeep = "Keep the ads",
    consentParentalConsentDescription = "They have read the Privacy Policy and the Terms of Use, and " +
        "agreed to the three answers below for me.",
    consentCrashReportingTitle = "Crash reports",
    consentCrashReportingDescription = "If the app crashes, it sends us what went wrong, along with " +
        "your device model and the app version.",
    consentAnalyticsTitle = "Usage statistics",
    consentAnalyticsDescription = "Tracks usage metrics and events while you use the app.",
    consentAdsTitle = "Ads",
    consentAdsDescription = "Shows ads, which is what keeps the app free.",
    consentAgree = "I agree to the Privacy Policy and the Terms of Use",
    consentAgreeRequired = "Tick this box to continue.",
    legalPrivacyPolicy = "Privacy Policy",
    legalTermsOfUse = "Terms of Use",
    consentAcceptAll = "Accept all",
    consentAcceptChosen = "Accept chosen",
    settingsPrivacy = "Privacy",
    examSelectionCountryTitleBefore = "Which country's test are you",
    examSelectionCountryTitleAccent = "taking?",
    examSelectionCountrySubtitle = "You can change this later.",
    examSelectionCountryComingSoon = "Coming soon",
    examSelectionLanguageTitleBefore = "Which",
    examSelectionLanguageTitleAccent = "language?",
    examSelectionLanguageSubtitle = "The test is available in several languages. A translated version " +
        "may be missing a few questions.",
    examSelectionCategoryTitleBefore = "Which license",
    examSelectionCategoryTitleAccent = "category?",
    countryName = { mapOf("SK" to "Slovakia", "CZ" to "Czechia")[it] ?: it },
    navTests = "Tests",
    navLearn = "Learn",
    navSettings = "Settings",
    back = "Back",
    learnTitle = "Learn",
    learnRoadSigns = "Road signs",
    learnRoadSignsSubtitle = "Every sign, and what it means",
    learnIntersections = "Intersections",
    learnIntersectionsSubtitle = "Who goes first, and when",
    learnLegislation = "Legislation",
    learnLegislationSubtitle = "The rules of the road",
    learnComingSoon = "Coming soon",
    signsSearchPlaceholder = "Search by name, number or meaning",
    clearSearch = "Clear search",
    signsCount = { if (it == 1) "1 sign" else "$it signs" },
    signsMatches = { matched, total -> "$matched of $total signs" },
    signsNothingFound = "No sign matches your search",
    signsCollapseAll = "Collapse every group",
    signsExpandAll = "Expand every group",
    signsCollapseGroup = "Collapse group",
    signsExpandGroup = "Expand group",
    situationsCount = { if (it == 1) "1 situation" else "$it situations" },
    situationName = { "Situation $it" },
    situationQuestions = { if (it == 1) "1 question" else "$it questions" },
    situationsNothingFound = "No situations in this exam",
    situationsDrillAll = "Drill every situation",
    situationsDrillName = "Situations drill",
    situationsResetAnswers = "Reset answers",
    situationsResetTitle = "Reset all the answers?",
    situationsResetMessage = "Every answer you have given about these situations is cleared, and a drill " +
        "that is under way begins again from the first question.",
    situationsResetConfirm = "Reset",
    situationsResetKeep = "Keep answers",
    questionNumber = { "Question $it" },
    answeredCorrectly = "Answered correctly",
    answeredWrongly = "Answered wrongly",
    change = "Change",
    testsFinishedLabel = "tests finished",
    progressEyebrow = "Driver in training",
    progressEyebrowPassed = "Expert driver",
    passed = "Passed",
    failed = "Failed",
    averageScore = "Average score",
    averageShort = "Average",
    randomTest = "Random test",
    randomTestShort = "Random",
    notPassed = "Not passed test",
    allTests = "All tests",
    notStarted = "Not started",
    resultSummary = { score, max, passed -> "$score / $max · ${if (passed) "Passed" else "Failed"}" },
    testName = { "Test $it" },
    unfinishedTest = "Unfinished test",
    resumeSummary = { answered, total -> "$answered of $total answered" },
    continueTest = "Continue",
    resumePromptTitle = "This test is already started",
    adGateTitle = "A short ad first",
    adGateMessage = { "Test $it starts after a short ad, which is what keeps the app free." },
    adGateWatch = "Watch the ad",
    adGateDismiss = "Not now",
    startOver = "Start over",
    passedPromptTitle = "This test is already passed",
    passedPromptMessage = { score, max ->
        "You scored $score / $max here, enough to pass. Redoing the test starts it from the " +
            "beginning, and you lose this score."
    },
    keepScore = "Keep my score",
    inProgress = "In progress",
    practiceMistakes = "Practice mistakes",
    mistakesToReview = { if (it == 0) "No mistakes to review" else "$it to review" },
    mistakesTestName = "Mistakes",
    rateCardTitle = "Enjoying the app?",
    rateCardMessage = { store -> "A quick rating in the $store helps other learner drivers find it." },
    rateCardAction = "Rate the app",
    rateCardDismiss = "Dismiss",
    questionsAnswered = "Questions answered",
    questionCounter = { number -> "$number" },
    questionCount = { total -> "/ $total" },
    answeredCount = { count -> "$count answered" },
    skippedCount = { count -> "$count skipped" },
    points = { if (it == 1) "Worth 1 point" else "Worth $it points" },
    questionList = "Question list",
    closeQuestionList = "Close question list",
    previousQuestion = "Previous question",
    nextQuestion = "Next question",
    previousSituation = "Previous situation",
    nextSituation = "Next situation",
    moreOptions = "More options",
    finishTest = "Finish test",

    answerAllRandomly = "Answer randomly (dev)",
    answerAllCorrectly = "Answer correctly (dev)",
    yourAnswer = "YOUR ANSWER",
    correctAnswer = "CORRECT ANSWER",
    enlargeImage = "Enlarge picture",
    closeImage = "Close picture",
    skippedQuestion = "SKIPPED QUESTION",
    allQuestionsAnswered = "All questions answered",
    overTime = { minutes -> "You're over time: a test is given $minutes minutes" },
    finish = "Finish",
    testFinished = "Test finished",
    showResults = "Results",
    leaveTestTitle = "Leave the test?",
    leaveTestMessage = "Your answers are saved, and you can come back to this test later.",
    leaveTest = "Leave",
    keepTesting = "Keep testing",
    resultsTitle = "Results",
    passedStamp = "Passed",
    failedStamp = "Failed",
    correctCount = "Correct",
    wrongCount = "Wrong",
    unansweredCount = "Unanswered",
    passMark = { "You need $it % to pass" },
    passMarkShort = { "pass mark $it %" },
    pointsLabel = "points",
    minutesShort = { "$it min" },
    bySection = "By section",
    reviewAnswers = "Review answers",
    backToTests = "Back to tests",
    backToList = "Back to list",
    drillPromptTitle = "This drill is already started",
    settingsTitle = "Settings",
    settingsLanguage = "App language",
    settingsAnswers = "Answers",
    settingsCheckImmediately = "Check answers immediately",
    settingsCheckImmediatelyDescription = "See whether your answer is right as soon as you pick it. " +
        "You can't change an answer after that.",
    settingsAdvanceAfterAnswer = "Move on after answering",
    settingsAdvanceAfterAnswerDescription = "The next question opens as soon as you pick an answer.\n" +
        "Available only when answers aren't checked immediately.",
    settingsExam = "Exam",
    settingsChangeExam = "Change exam",
    aboutTitle = "About",
    settingsApp = "App",
    aboutApp = "About the app",
    aboutVersion = { "Version $it" },
    settingsDeviceIdentifier = "Device identifier",
    settingsDeviceIdentifierDescription = "What we file this device's usage statistics and crash " +
        "reports under. It is kept here so you can quote it when you write to us, with the switches " +
        "on or off.",
    settingsDeviceIdentifierEmpty = "Nothing has been sent from this device, so there is nothing " +
        "filed under an identifier yet. One appears here the first time you turn on usage " +
        "statistics or crash reports.",
    aboutLibraries = "Open source libraries",
    aboutLibrariesIntro = "Naj Autoškola is built with these open source projects. Tap one to see " +
        "its license, and everyone who wrote it.",
    aboutLibrariesSearch = "Search libraries",
    aboutLibrariesNothingFound = "No library matches your search",
    feedbackTitle = "Send feedback",
    feedbackIntro = "Found a bug, have an idea, or want to ask about your data? Tell us about it.",
    feedbackSendTo = { "Send your feedback over email to: $it" },
    feedbackMessageHint = "Your message…",
    feedbackKindFeedback = "Feedback",
    feedbackKindPrivacyRequest = "Privacy request",
    feedbackRequestHint = "What you would like us to do — send you a copy of what we hold, for " +
        "example, or delete it.",
    feedbackPrivacyRequestNote = "The app puts the identifier from Settings in this email, so we can " +
        "find what we hold about you.",
    feedbackPrivacyRequestNoIdentifier = "Nothing has been sent from this device yet, so there is " +
        "no identifier to add. One appears once you turn on usage statistics or crash reports.",
    feedbackIncludeDeviceInfo = "Include device information",
    feedbackIncludeDeviceInfoDescription = "Your phone's model, its system version and the app version.",
    feedbackSend = "Send",
    feedbackMessageMissing = "Write a message first.",
    feedbackNoEmailApp = "No email app was found on this device.",
    feedbackIncludeDetails = "Include this in your email",
    feedbackCopy = "Copy",
    feedbackCopied = "Copied",
    feedbackSubject = { "Feedback: $it" },
    feedbackRequestSubject = { "Privacy request: $it" },
    feedbackVersionLine = { "App version: $it" },
    feedbackDeviceLine = { "Device: $it" },
    feedbackIdentifierLine = { "Device identifier: $it" },
)

private val SlovakStrings = Strings(
    appName = "Naj Autoškola",
    disclaimerTitleBefore = "Skôr než",
    disclaimerTitleAccent = "začnete",
    disclaimerIndependent = "Naj Autoškola je nezávislá aplikácia. Nevytvoril ju žiadny štátny orgán, " +
        "nie je s ním prepojená ani ním nie je schválená.",
    disclaimerSource = "Otázky a obrázky pochádzajú z oficiálnej databázy otázok, ktorú zverejňuje " +
        "Ministerstvo vnútra Slovenskej republiky (minv.sk). Preberáme ich v zverejnenom znení, ale ak sa " +
        "niečo líši, platí oficiálna skúška a aktuálne predpisy.",
    disclaimerAccept = "Rozumiem",
    consentTitle = "Vaše dáta, vaša voľba",
    consentSubtitle = "Aplikácia môže zobrazovať reklamy a odosielať dve veci, aby sme ju mohli " +
        "opraviť a vylepšiť.",
    consentAgeQuestion = "Máte 16 rokov alebo viac?",
    consentAgeOver16 = "16 rokov alebo viac",
    consentAgeUnder16 = "Menej ako 16",
    consentAgeUnder16Reason = "Kým nemáte 16 rokov, nemôžete s tým súhlasiť sami: vo vašom mene tak " +
        "môže urobiť len rodič alebo zákonný zástupca.",
    consentParentalConsent = "Rodič alebo zákonný zástupca s tým v mojom mene súhlasil",
    settingsAgeConfirmTitle = "Máte už 16 rokov?",
    settingsAgeConfirmMessage = "Odpoveďou áno sa odomknú tri prepínače nižšie, preto odpovedzte áno " +
        "len vtedy, ak je to pravda.",
    settingsAgeConfirmAction = "Áno, mám 16 rokov alebo viac",
    settingsAgeConfirmDismiss = "Ešte nie",
    settingsAdsOffTitle = "Vypnúť reklamy?",
    settingsAdsOffMessage = "Aplikácia je zadarmo vďaka reklámam. Ich vypnutím zmiznú reklamy aj " +
        "príjem, ktorý ju drží v prevádzke.",
    settingsAdsOffAction = "Vypnúť reklamy",
    settingsAdsKeep = "Nechať reklamy",
    consentParentalConsentDescription = "Prečítal si Zásady ochrany osobných údajov a Podmienky " +
        "používania a v mojom mene súhlasil s tromi odpoveďami nižšie.",
    consentCrashReportingTitle = "Hlásenia o zrúteniach",
    consentCrashReportingDescription = "Ak aplikácia spadne, pošle nám, čo sa pokazilo, model " +
        "zariadenia a verziu aplikácie.",
    consentAnalyticsTitle = "Štatistiky používania",
    consentAnalyticsDescription = "Sleduje štatistiky používania a udalosti počas používania aplikácie.",
    consentAdsTitle = "Reklamy",
    consentAdsDescription = "Zobrazuje reklamy, vďaka ktorým je aplikácia zadarmo.",
    consentAgree = "Súhlasím so Zásadami ochrany osobných údajov a Podmienkami používania",
    consentAgreeRequired = "Ak chcete pokračovať, zaškrtnite toto políčko.",
    legalPrivacyPolicy = "Zásady ochrany osobných údajov",
    legalTermsOfUse = "Podmienky používania",
    consentAcceptAll = "Prijať všetko",
    consentAcceptChosen = "Prijať vybrané",
    settingsPrivacy = "Súkromie",
    examSelectionCountryTitleBefore = "Na skúšku ktorej krajiny sa",
    examSelectionCountryTitleAccent = "pripravujete?",
    examSelectionCountrySubtitle = "Neskôr to môžete zmeniť.",
    examSelectionCountryComingSoon = "Čoskoro",
    examSelectionLanguageTitleBefore = "V akom",
    examSelectionLanguageTitleAccent = "jazyku?",
    examSelectionLanguageSubtitle = "Test je dostupný vo viacerých jazykoch. V preloženej verzii môže " +
        "chýbať niekoľko otázok.",
    examSelectionCategoryTitleBefore = "Akú skupinu vodičského",
    examSelectionCategoryTitleAccent = "oprávnenia?",
    countryName = { mapOf("SK" to "Slovensko", "CZ" to "Česko")[it] ?: it },
    navTests = "Testy",
    navLearn = "Učiť sa",
    navSettings = "Nastavenia",
    back = "Späť",
    learnTitle = "Učiť sa",
    learnRoadSigns = "Dopravné značky",
    learnRoadSignsSubtitle = "Všetky značky a ich význam",
    learnIntersections = "Križovatky",
    learnIntersectionsSubtitle = "Kto má prednosť a kedy",
    learnLegislation = "Legislatíva",
    learnLegislationSubtitle = "Pravidlá cestnej premávky",
    learnComingSoon = "Čoskoro",
    signsSearchPlaceholder = "Hľadať podľa názvu, čísla alebo významu",
    clearSearch = "Vymazať hľadanie",
    signsCount = { "$it " + westSlavicPlural(it, "značka", "značky", "značiek") },
    signsMatches = { matched, total -> "$matched z $total značiek" },
    signsNothingFound = "Žiadna značka nezodpovedá hľadaniu",
    signsCollapseAll = "Zbaliť všetky skupiny",
    signsExpandAll = "Rozbaliť všetky skupiny",
    signsCollapseGroup = "Zbaliť skupinu",
    signsExpandGroup = "Rozbaliť skupinu",
    situationsCount = { "$it " + westSlavicPlural(it, "situácia", "situácie", "situácií") },
    situationName = { "Situácia $it" },
    situationQuestions = { "$it " + westSlavicPlural(it, "otázka", "otázky", "otázok") },
    situationsNothingFound = "Táto skúška nemá situácie",
    situationsDrillAll = "Precvičiť všetky situácie",
    situationsDrillName = "Nácvik situácií",
    situationsResetAnswers = "Vymazať odpovede",
    situationsResetTitle = "Vymazať všetky odpovede?",
    situationsResetMessage = "Vymažú sa všetky odpovede, ktoré ste dali k týmto situáciám, a rozbehnutý " +
        "nácvik začne od prvej otázky.",
    situationsResetConfirm = "Vymazať",
    situationsResetKeep = "Ponechať odpovede",
    questionNumber = { "Otázka $it" },
    answeredCorrectly = "Zodpovedané správne",
    answeredWrongly = "Zodpovedané nesprávne",
    change = "Zmeniť",
    testsFinishedLabel = "dokončené testy",
    progressEyebrow = "Vodič v príprave",
    progressEyebrowPassed = "Skúsený vodič",
    passed = "Úspešné",
    failed = "Neúspešné",
    averageScore = "Priemerné skóre",
    averageShort = "Priemer",
    randomTest = "Náhodný test",
    randomTestShort = "Náhodný",
    notPassed = "Neúspešný test",
    allTests = "Všetky testy",
    notStarted = "Nezačatý",
    resultSummary = { score, max, passed -> "$score / $max · ${if (passed) "Úspešný" else "Neúspešný"}" },
    testName = { "Test $it" },
    unfinishedTest = "Nedokončený test",
    resumeSummary = { answered, total -> "zodpovedané $answered z $total" },
    continueTest = "Pokračovať",
    resumePromptTitle = "Tento test je už začatý",
    adGateTitle = "Najprv krátka reklama",
    adGateMessage = { "Test $it sa spustí po krátkej reklame, vďaka ktorej je aplikácia zadarmo." },
    adGateWatch = "Pozrieť reklamu",
    adGateDismiss = "Teraz nie",
    startOver = "Začať odznova",
    passedPromptTitle = "Tento test je už úspešný",
    passedPromptMessage = { score, max ->
        "Dosiahli ste tu $score / $max, čo stačí na úspech. Nový test začne od začiatku " +
            "a o toto skóre prídete."
    },
    keepScore = "Ponechať skóre",
    inProgress = "Rozpracovaný",
    practiceMistakes = "Precvičiť chyby",
    mistakesToReview = { if (it == 0) "Žiadne chyby na zopakovanie" else "Na zopakovanie: $it" },
    mistakesTestName = "Chyby",
    rateCardTitle = "Baví vás aplikácia?",
    rateCardMessage = { store -> "Rýchle hodnotenie v $store pomôže ďalším vodičom nájsť aplikáciu." },
    rateCardAction = "Ohodnotiť aplikáciu",
    rateCardDismiss = "Zavrieť",
    questionsAnswered = "Zodpovedané otázky",
    questionCounter = { number -> "$number" },
    questionCount = { total -> "/ $total" },
    answeredCount = { "$it " + westSlavicPlural(it, "zodpovedaná", "zodpovedané", "zodpovedaných") },
    skippedCount = { "$it " + westSlavicPlural(it, "nezodpovedaná", "nezodpovedané", "nezodpovedaných") },
    points = { "za $it " + westSlavicPlural(it, "bod", "body", "bodov") },
    questionList = "Zoznam otázok",
    closeQuestionList = "Zavrieť zoznam otázok",
    previousQuestion = "Predchádzajúca otázka",
    nextQuestion = "Ďalšia otázka",
    previousSituation = "Predchádzajúca situácia",
    nextSituation = "Nasledujúca situácia",
    moreOptions = "Ďalšie možnosti",
    finishTest = "Ukončiť test",

    answerAllRandomly = "Odpovedať náhodne (dev)",
    answerAllCorrectly = "Odpovedať správne (dev)",
    yourAnswer = "VAŠA ODPOVEĎ",
    correctAnswer = "SPRÁVNA ODPOVEĎ",
    enlargeImage = "Zväčšiť obrázok",
    closeImage = "Zavrieť obrázok",
    skippedQuestion = "NEZODPOVEDANÁ OTÁZKA",
    allQuestionsAnswered = "Všetky otázky sú zodpovedané",
    overTime = { minutes -> "Prekročili ste čas: test má $minutes minút" },
    finish = "Dokončiť",
    testFinished = "Test je dokončený",
    showResults = "Výsledky",
    leaveTestTitle = "Opustiť test?",
    leaveTestMessage = "Vaše odpovede sú uložené a k testu sa môžete vrátiť neskôr.",
    leaveTest = "Opustiť",
    keepTesting = "Pokračovať v teste",
    resultsTitle = "Výsledky",
    passedStamp = "Vyhovel",
    failedStamp = "Nevyhovel",
    correctCount = "Správne",
    wrongCount = "Nesprávne",
    unansweredCount = "Nezodpovedané",
    passMark = { "Na úspech potrebujete $it %" },
    passMarkShort = { "hranica $it %" },
    pointsLabel = "bodov",
    minutesShort = { "$it min" },
    bySection = "Podľa okruhov",
    reviewAnswers = "Prezrieť odpovede",
    backToTests = "Späť na testy",
    backToList = "Späť na zoznam",
    drillPromptTitle = "Tento nácvik už začal",
    settingsTitle = "Nastavenia",
    settingsLanguage = "Jazyk aplikácie",
    settingsAnswers = "Odpovede",
    settingsCheckImmediately = "Hneď vyhodnotiť odpoveď",
    settingsCheckImmediatelyDescription = "Po výbere odpovede hneď uvidíte, či je správna. " +
        "Odpoveď potom už nemôžete zmeniť.",
    settingsAdvanceAfterAnswer = "Po odpovedi prejsť ďalej",
    settingsAdvanceAfterAnswerDescription = "Ďalšia otázka sa otvorí hneď po výbere odpovede.\n" +
        "Dostupné len vtedy, keď sa odpoveď nevyhodnocuje hneď.",
    settingsExam = "Skúška",
    settingsChangeExam = "Zmeniť skúšku",
    aboutTitle = "O aplikácii",
    settingsApp = "Aplikácia",
    aboutApp = "O aplikácii",
    aboutVersion = { "Verzia $it" },
    settingsDeviceIdentifier = "Identifikátor zariadenia",
    settingsDeviceIdentifierDescription = "Pod týmto identifikátorom evidujeme štatistiky používania " +
        "a hlásenia o zrúteniach z tohto zariadenia. Zostáva tu, aby ste ho mohli uviesť, keď nám " +
        "napíšete — bez ohľadu na to, či sú prepínače zapnuté.",
    settingsDeviceIdentifierEmpty = "Z tohto zariadenia zatiaľ nebolo nič odoslané, takže pod žiadnym " +
        "identifikátorom nič neevidujeme. Zobrazí sa tu pri prvom zapnutí štatistík používania " +
        "alebo hlásení o zrúteniach.",
    aboutLibraries = "Knižnice s otvoreným kódom",
    aboutLibrariesIntro = "Naj Autoškola je postavená na týchto open source projektoch. Ťuknutím na " +
        "niektorý z nich zobrazíte jeho licenciu aj všetkých, ktorí ho napísali.",
    aboutLibrariesSearch = "Hľadať knižnice",
    aboutLibrariesNothingFound = "Hľadanému výrazu nezodpovedá žiadna knižnica",
    feedbackTitle = "Poslať spätnú väzbu",
    feedbackIntro = "Našli ste chybu, máte nápad alebo sa chcete opýtať na svoje údaje? Napíšte nám o tom.",
    feedbackSendTo = { "Pošlite svoju spätnú väzbu e-mailom na: $it" },
    feedbackMessageHint = "Vaša správa…",
    feedbackKindFeedback = "Spätná väzba",
    feedbackKindPrivacyRequest = "Žiadosť o údaje",
    feedbackRequestHint = "Čo by ste chceli, aby sme urobili — napríklad poslať kópiu toho, čo o vás " +
        "evidujeme, alebo to vymazať.",
    feedbackPrivacyRequestNote = "Aplikácia do tohto e-mailu vloží identifikátor z nastavení, aby sme " +
        "našli, čo o vás evidujeme.",
    feedbackPrivacyRequestNoIdentifier = "Z tohto zariadenia zatiaľ nebolo nič odoslané, takže nie " +
        "je čo pridať. Identifikátor sa zobrazí po zapnutí štatistík používania alebo hlásení o " +
        "zrúteniach.",
    feedbackIncludeDeviceInfo = "Pridať informácie o zariadení",
    feedbackIncludeDeviceInfoDescription = "Model telefónu, verzia systému a verzia aplikácie.",
    feedbackSend = "Odoslať",
    feedbackMessageMissing = "Najprv napíšte správu.",
    feedbackNoEmailApp = "V tomto zariadení sa nenašla e-mailová aplikácia.",
    feedbackIncludeDetails = "Pridajte to do e-mailu",
    feedbackCopy = "Kopírovať",
    feedbackCopied = "Skopírované",
    feedbackSubject = { "Spätná väzba: $it" },
    feedbackRequestSubject = { "Žiadosť o údaje: $it" },
    feedbackVersionLine = { "Verzia aplikácie: $it" },
    feedbackDeviceLine = { "Zariadenie: $it" },
    feedbackIdentifierLine = { "Identifikátor zariadenia: $it" },
)

private val HungarianStrings = Strings(
    appName = "Legjobb Autósiskola",
    disclaimerTitleBefore = "Mielőtt",
    disclaimerTitleAccent = "elkezdené",
    disclaimerIndependent = "A Legjobb Autósiskola független alkalmazás. Nem állami szerv készítette, " +
        "nem áll kapcsolatban egyetlen hatósággal sem, és egyetlen hatóság sem hagyta jóvá.",
    disclaimerSource = "A kérdések és a képek a Szlovák Köztársaság Belügyminisztériuma által közzétett " +
        "hivatalos kérdésbankból származnak (minv.sk). A közzétett formában vesszük át őket, de eltérés " +
        "esetén a hivatalos vizsga és a hatályos jogszabályok az irányadók.",
    disclaimerAccept = "Értem",
    consentTitle = "Az Ön adatai, az Ön döntése",
    consentSubtitle = "Az alkalmazás reklámokat jeleníthet meg, és két dolgot küldhet, hogy " +
        "javíthassuk és fejleszthessük.",
    consentAgeQuestion = "16 éves vagy idősebb?",
    consentAgeOver16 = "16 éves vagy idősebb",
    consentAgeUnder16 = "16 év alatti",
    consentAgeUnder16Reason = "16 év alatt ezekhez nem tud egyedül hozzájárulni: Ön helyett ezt csak " +
        "szülő vagy gondviselő teheti meg.",
    consentParentalConsent = "Szülő vagy gondviselő hozzájárult ehhez Ön helyett",
    settingsAgeConfirmTitle = "Már betöltötte a 16. életévét?",
    settingsAgeConfirmMessage = "Ha igennel válaszol, kinyílnak az alábbi három kapcsoló, ezért csak " +
        "akkor válaszoljon igennel, ha ez igaz.",
    settingsAgeConfirmAction = "Igen, 16 éves vagy idősebb vagyok",
    settingsAgeConfirmDismiss = "Még nem",
    settingsAdsOffTitle = "Kikapcsolja a reklámokat?",
    settingsAdsOffMessage = "Az alkalmazás a reklámok miatt ingyenes. Kikapcsolásukkal eltűnnek a " +
        "reklámok, és az a bevétel is, ami működteti.",
    settingsAdsOffAction = "Reklámok kikapcsolása",
    settingsAdsKeep = "Maradjanak a reklámok",
    consentParentalConsentDescription = "Elolvasta az Adatvédelmi szabályzatot és a Felhasználási " +
        "feltételeket, és Ön helyett hozzájárult a lenti három válaszhoz.",
    consentCrashReportingTitle = "Hibajelentések",
    consentCrashReportingDescription = "Ha az alkalmazás lefagy, elküldi, mi hibázott, a készülék " +
        "típusát és az alkalmazás verzióját.",
    consentAnalyticsTitle = "Használati statisztikák",
    consentAnalyticsDescription = "Használati mérőszámokat és eseményeket követ nyomon, miközben " +
        "használja az alkalmazást.",
    consentAdsTitle = "Reklámok",
    consentAdsDescription = "Reklámokat jelenít meg, ez teszi ingyenessé az alkalmazást.",
    consentAgree = "Elfogadom az Adatvédelmi szabályzatot és a Felhasználási feltételeket",
    consentAgreeRequired = "A folytatáshoz jelölje be ezt a négyzetet.",
    legalPrivacyPolicy = "Adatvédelmi szabályzat",
    legalTermsOfUse = "Felhasználási feltételek",
    consentAcceptAll = "Összes elfogadása",
    consentAcceptChosen = "Kiválasztottak elfogadása",
    settingsPrivacy = "Adatvédelem",
    examSelectionCountryTitleBefore = "Melyik ország vizsgájára",
    examSelectionCountryTitleAccent = "készül?",
    examSelectionCountrySubtitle = "Ezt később módosíthatja.",
    examSelectionCountryComingSoon = "Hamarosan",
    examSelectionLanguageTitleBefore = "Milyen",
    examSelectionLanguageTitleAccent = "nyelven?",
    examSelectionLanguageSubtitle = "A teszt több nyelven elérhető. A lefordított változatból hiányozhat " +
        "néhány kérdés.",
    examSelectionCategoryTitleBefore = "Melyik",
    examSelectionCategoryTitleAccent = "jogosítványkategóriára?",
    countryName = { mapOf("SK" to "Szlovákia", "CZ" to "Csehország")[it] ?: it },
    navTests = "Tesztek",
    navLearn = "Tanulás",
    navSettings = "Beállítások",
    back = "Vissza",
    learnTitle = "Tanulás",
    learnRoadSigns = "Közúti jelzőtáblák",
    learnRoadSignsSubtitle = "Minden tábla és a jelentése",
    learnIntersections = "Kereszteződések",
    learnIntersectionsSubtitle = "Ki megy előre, és mikor",
    learnLegislation = "Jogszabályok",
    learnLegislationSubtitle = "A közlekedés szabályai",
    learnComingSoon = "Hamarosan",
    signsSearchPlaceholder = "Keresés név, szám vagy jelentés szerint",
    clearSearch = "Keresés törlése",
    signsCount = { "$it tábla" },
    signsMatches = { matched, total -> "$matched / $total tábla" },
    signsNothingFound = "Nincs a keresésnek megfelelő tábla",
    signsCollapseAll = "Összes csoport összecsukása",
    signsExpandAll = "Összes csoport kibontása",
    signsCollapseGroup = "Csoport összecsukása",
    signsExpandGroup = "Csoport kibontása",
    situationsCount = { "$it helyzet" },
    situationName = { "$it. helyzet" },
    situationQuestions = { "$it kérdés" },
    situationsNothingFound = "Ennek a vizsgának nincs helyzete",
    situationsDrillAll = "Összes helyzet gyakorlása",
    situationsDrillName = "Helyzetgyakorlás",
    situationsResetAnswers = "Válaszok törlése",
    situationsResetTitle = "Törli az összes választ?",
    situationsResetMessage = "A helyzetekre adott összes válasz törlődik, és az elkezdett gyakorlás az " +
        "első kérdéstől indul újra.",
    situationsResetConfirm = "Törlés",
    situationsResetKeep = "Válaszok megtartása",
    questionNumber = { "$it. kérdés" },
    answeredCorrectly = "Helyesen megválaszolva",
    answeredWrongly = "Helytelenül megválaszolva",
    change = "Módosítás",
    testsFinishedLabel = "befejezett teszt",
    progressEyebrow = "Tanuló vezető",
    progressEyebrowPassed = "Tapasztalt vezető",
    passed = "Sikeres",
    failed = "Sikertelen",
    averageScore = "Átlagos eredmény",
    averageShort = "Átlag",
    randomTest = "Véletlen teszt",
    randomTestShort = "Véletlen",
    notPassed = "Sikertelen teszt",
    allTests = "Összes teszt",
    notStarted = "Nincs elkezdve",
    resultSummary = { score, max, passed -> "$score / $max · ${if (passed) "Sikeres" else "Sikertelen"}" },
    testName = { "$it. teszt" },
    unfinishedTest = "Befejezetlen teszt",
    resumeSummary = { answered, total -> "$answered / $total megválaszolva" },
    continueTest = "Folytatás",
    resumePromptTitle = "Ez a teszt már elkezdődött",
    adGateTitle = "Előbb egy rövid reklám",
    adGateMessage = { "A $it. teszt egy rövid reklám után indul, ez teszi ingyenessé az alkalmazást." },
    adGateWatch = "Reklám megtekintése",
    adGateDismiss = "Most nem",
    startOver = "Újrakezdés",
    passedPromptTitle = "Ez a teszt már sikeres",
    passedPromptMessage = { score, max ->
        "Itt $score / $max pontot ért el, ami elég a sikerhez. Az újrakezdéssel a teszt elölről " +
            "kezdődik, és elveszíti ezt az eredményt."
    },
    keepScore = "Pontszám megtartása",
    inProgress = "Folyamatban",
    practiceMistakes = "Hibák gyakorlása",
    mistakesToReview = { if (it == 0) "Nincs ismétlendő hiba" else "Ismétlendő: $it" },
    mistakesTestName = "Hibák",
    rateCardTitle = "Tetszik az alkalmazás?",
    rateCardMessage = { store -> "Értékelés a $store áruházban segít másoknak megtalálni az alkalmazást." },
    rateCardAction = "Alkalmazás értékelése",
    rateCardDismiss = "Bezárás",
    questionsAnswered = "Megválaszolt kérdések",
    questionCounter = { number -> "$number" },
    questionCount = { total -> "/ $total" },
    // Hungarian counts with a singular after the numeral: "6 megválaszolt", not "6 megválaszoltak".
    answeredCount = { "$it megválaszolt" },
    skippedCount = { "$it kihagyott" },
    points = { "$it pontot ér" },
    questionList = "Kérdések listája",
    closeQuestionList = "Kérdéslista bezárása",
    previousQuestion = "Előző kérdés",
    nextQuestion = "Következő kérdés",
    previousSituation = "Előző helyzet",
    nextSituation = "Következő helyzet",
    moreOptions = "További lehetőségek",
    finishTest = "Teszt befejezése",

    answerAllRandomly = "Véletlenszerű válasz (dev)",
    answerAllCorrectly = "Válaszolj helyesen (dev)",
    yourAnswer = "AZ ÖN VÁLASZA",
    correctAnswer = "HELYES VÁLASZ",
    enlargeImage = "Kép nagyítása",
    closeImage = "Kép bezárása",
    skippedQuestion = "KIHAGYOTT KÉRDÉS",
    allQuestionsAnswered = "Minden kérdést megválaszolt",
    overTime = { minutes -> "Túllépte a teszt $minutes perces időkeretét" },
    finish = "Befejezés",
    testFinished = "A teszt befejeződött",
    showResults = "Eredmények",
    leaveTestTitle = "Kilép a tesztből?",
    leaveTestMessage = "A válaszai mentve vannak, és a tesztet később folytathatja.",
    leaveTest = "Kilépés",
    keepTesting = "Teszt folytatása",
    resultsTitle = "Eredmények",
    passedStamp = "Sikeres",
    failedStamp = "Sikertelen",
    correctCount = "Helyes",
    wrongCount = "Helytelen",
    unansweredCount = "Megválaszolatlan",
    passMark = { "A sikerhez $it% szükséges" },
    passMarkShort = { "küszöb $it%" },
    pointsLabel = "pont",
    minutesShort = { "$it perc" },
    bySection = "Témakörönként",
    reviewAnswers = "Válaszok áttekintése",
    backToTests = "Vissza a tesztekhez",
    backToList = "Vissza a listához",
    drillPromptTitle = "Ez a gyakorlás már elkezdődött",
    settingsTitle = "Beállítások",
    settingsLanguage = "Az alkalmazás nyelve",
    settingsAnswers = "Válaszok",
    settingsCheckImmediately = "Válaszok azonnali értékelése",
    settingsCheckImmediatelyDescription = "A válasz kiválasztása után azonnal látja, hogy helyes-e. " +
        "A választ ezután nem módosíthatja.",
    settingsAdvanceAfterAnswer = "Továbblépés válasz után",
    settingsAdvanceAfterAnswerDescription = "A következő kérdés azonnal megnyílik, amint választ ad.\n" +
        "Csak akkor érhető el, ha a válaszokat nem értékeli ki azonnal.",
    settingsExam = "Vizsga",
    settingsChangeExam = "Vizsga módosítása",
    aboutTitle = "Névjegy",
    settingsApp = "Alkalmazás",
    aboutApp = "Az alkalmazásról",
    aboutVersion = { "$it verzió" },
    settingsDeviceIdentifier = "Eszközazonosító",
    settingsDeviceIdentifierDescription = "Ez az azonosító alatt tartjuk nyilván az eszköz használati " +
        "statisztikáit és összeomlás-jelentéseit. Itt marad, hogy megadd, amikor írsz nekünk — akár " +
        "be vannak kapcsolva a kapcsolók, akár nem.",
    settingsDeviceIdentifierEmpty = "Erről az eszközről még nem lett semmi elküldve, így még nincs, " +
        "ami alatt nyilvántartanánk. Az első alkalommal jelenik meg itt, amikor bekapcsolod a " +
        "használati statisztikákat vagy az összeomlás-jelentéseket.",
    aboutLibraries = "Nyílt forráskódú könyvtárak",
    aboutLibrariesIntro = "A Legjobb Autósiskola ezekre a nyílt forráskódú projektekre épül. " +
        "Koppintson az egyikre a licencéhez és a készítőihez.",
    aboutLibrariesSearch = "Keresés a könyvtárakban",
    aboutLibrariesNothingFound = "Egyetlen könyvtár sem felel meg a keresésnek",
    feedbackTitle = "Visszajelzés küldése",
    feedbackIntro = "Hibát találtál, ötleted van, vagy az adataidról kérdeznél? Írd meg nekünk.",
    feedbackSendTo = { "Küldd el a visszajelzésed e-mailben erre a címre: $it" },
    feedbackMessageHint = "Az üzeneted…",
    feedbackKindFeedback = "Visszajelzés",
    feedbackKindPrivacyRequest = "Adatkérés",
    feedbackRequestHint = "Mit szeretnél, mit tegyünk — például küldjünk másolatot arról, amit " +
        "nyilvántartunk, vagy töröljük.",
    feedbackPrivacyRequestNote = "Az alkalmazás beilleszti az e-mailbe a beállításokban látható " +
        "azonosítót, hogy megtaláljuk, mit tartunk nyilván rólad.",
    feedbackPrivacyRequestNoIdentifier = "Erről az eszközről még nem lett semmi elküldve, így nincs " +
        "mit hozzáadni. Az azonosító akkor jelenik meg, amikor bekapcsolod a használati " +
        "statisztikákat vagy az összeomlás-jelentéseket.",
    feedbackIncludeDeviceInfo = "Eszközadatok mellékelése",
    feedbackIncludeDeviceInfoDescription = "A telefon típusa, a rendszer verziója és az alkalmazás verziója.",
    feedbackSend = "Küldés",
    feedbackMessageMissing = "Előbb írj egy üzenetet.",
    feedbackNoEmailApp = "Ezen az eszközön nincs e-mail alkalmazás.",
    feedbackIncludeDetails = "Mellékeld az e-mailhez",
    feedbackCopy = "Másolás",
    feedbackCopied = "Másolva",
    feedbackSubject = { "Visszajelzés: $it" },
    feedbackRequestSubject = { "Adatkérés: $it" },
    feedbackVersionLine = { "Alkalmazás verziója: $it" },
    feedbackDeviceLine = { "Eszköz: $it" },
    feedbackIdentifierLine = { "Eszközazonosító: $it" },
)

private val CzechStrings = Strings(
    appName = "Nej Autoškola",
    disclaimerTitleBefore = "Než",
    disclaimerTitleAccent = "začnete",
    disclaimerIndependent = "Nej Autoškola je nezávislá aplikace. Nevytvořil ji žádný státní orgán, " +
        "není s ním spojena ani jím není schválena.",
    disclaimerSource = "Otázky a obrázky pocházejí z oficiální databáze otázek, kterou zveřejňuje " +
        "Ministerstvo vnitra Slovenské republiky (minv.sk). Přebíráme je ve zveřejněném znění, ale pokud se " +
        "něco liší, platí oficiální zkouška a aktuální předpisy.",
    disclaimerAccept = "Rozumím",
    consentTitle = "Vaše data, vaše volba",
    consentSubtitle = "Aplikace může zobrazovat reklamy a odesílat dvě věci, abychom ji mohli " +
        "opravit a vylepšit.",
    consentAgeQuestion = "Je vám 16 let nebo více?",
    consentAgeOver16 = "16 let nebo více",
    consentAgeUnder16 = "Méně než 16",
    consentAgeUnder16Reason = "Pod 16 let s tím nemůžete souhlasit sami: ve vašem jménu to může " +
        "udělat jen rodič nebo zákonný zástupce.",
    consentParentalConsent = "Rodič nebo zákonný zástupce s tím za mě souhlasil",
    settingsAgeConfirmTitle = "Už je vám 16 let?",
    settingsAgeConfirmMessage = "Odpovědí ano se odemknou tři přepínače níže, proto odpovězte ano jen " +
        "tehdy, pokud je to pravda.",
    settingsAgeConfirmAction = "Ano, je mi 16 let nebo více",
    settingsAgeConfirmDismiss = "Ještě ne",
    settingsAdsOffTitle = "Vypnout reklamy?",
    settingsAdsOffMessage = "Aplikace je zdarma díky reklamám. Jejich vypnutím zmizí reklamy i " +
        "příjem, který ji drží v provozu.",
    settingsAdsOffAction = "Vypnout reklamy",
    settingsAdsKeep = "Nechat reklamy",
    consentParentalConsentDescription = "Přečetl si Zásady ochrany osobních údajů a Podmínky použití " +
        "a souhlasil za mě se třemi odpověďmi níže.",
    consentCrashReportingTitle = "Hlášení o pádech",
    consentCrashReportingDescription = "Pokud aplikace spadne, pošle nám, co se pokazilo, model " +
        "zařízení a verzi aplikace.",
    consentAnalyticsTitle = "Statistiky používání",
    consentAnalyticsDescription = "Sleduje statistiky používání a události při používání aplikace.",
    consentAdsTitle = "Reklamy",
    consentAdsDescription = "Zobrazuje reklamy, díky kterým je aplikace zdarma.",
    consentAgree = "Souhlasím se Zásadami ochrany osobních údajů a Podmínkami použití",
    consentAgreeRequired = "Chcete-li pokračovat, zaškrtněte toto políčko.",
    legalPrivacyPolicy = "Zásady ochrany osobních údajů",
    legalTermsOfUse = "Podmínky použití",
    consentAcceptAll = "Přijmout vše",
    consentAcceptChosen = "Přijmout vybrané",
    settingsPrivacy = "Soukromí",
    examSelectionCountryTitleBefore = "Na zkoušku které země se",
    examSelectionCountryTitleAccent = "připravujete?",
    examSelectionCountrySubtitle = "Později to můžete změnit.",
    examSelectionCountryComingSoon = "Již brzy",
    examSelectionLanguageTitleBefore = "V jakém",
    examSelectionLanguageTitleAccent = "jazyce?",
    examSelectionLanguageSubtitle = "Test je dostupný ve více jazycích. V přeložené verzi může chybět " +
        "několik otázek.",
    examSelectionCategoryTitleBefore = "Jakou skupinu řidičského",
    examSelectionCategoryTitleAccent = "oprávnění?",
    countryName = { mapOf("SK" to "Slovensko", "CZ" to "Česko")[it] ?: it },
    navTests = "Testy",
    navLearn = "Učit se",
    navSettings = "Nastavení",
    back = "Zpět",
    learnTitle = "Učit se",
    learnRoadSigns = "Dopravní značky",
    learnRoadSignsSubtitle = "Všechny značky a jejich význam",
    learnIntersections = "Křižovatky",
    learnIntersectionsSubtitle = "Kdo má přednost a kdy",
    learnLegislation = "Legislativa",
    learnLegislationSubtitle = "Pravidla silničního provozu",
    learnComingSoon = "Již brzy",
    signsSearchPlaceholder = "Hledat podle názvu, čísla nebo významu",
    clearSearch = "Vymazat hledání",
    signsCount = { "$it " + westSlavicPlural(it, "značka", "značky", "značek") },
    signsMatches = { matched, total -> "$matched z $total značek" },
    signsNothingFound = "Žádná značka neodpovídá hledání",
    signsCollapseAll = "Sbalit všechny skupiny",
    signsExpandAll = "Rozbalit všechny skupiny",
    signsCollapseGroup = "Sbalit skupinu",
    signsExpandGroup = "Rozbalit skupinu",
    situationsCount = { "$it " + westSlavicPlural(it, "situace", "situace", "situací") },
    situationName = { "Situace $it" },
    situationQuestions = { "$it " + westSlavicPlural(it, "otázka", "otázky", "otázek") },
    situationsNothingFound = "Tato zkouška nemá situace",
    situationsDrillAll = "Procvičit všechny situace",
    situationsDrillName = "Nácvik situací",
    situationsResetAnswers = "Vymazat odpovědi",
    situationsResetTitle = "Vymazat všechny odpovědi?",
    situationsResetMessage = "Vymažou se všechny odpovědi, které jste k těmto situacím dali, a rozpracovaný " +
        "nácvik začne znovu od první otázky.",
    situationsResetConfirm = "Vymazat",
    situationsResetKeep = "Ponechat odpovědi",
    questionNumber = { "Otázka $it" },
    answeredCorrectly = "Zodpovězeno správně",
    answeredWrongly = "Zodpovězeno nesprávně",
    change = "Změnit",
    testsFinishedLabel = "dokončené testy",
    progressEyebrow = "Řidič v přípravě",
    progressEyebrowPassed = "Zkušený řidič",
    passed = "Úspěšné",
    failed = "Neúspěšné",
    averageScore = "Průměrné skóre",
    averageShort = "Průměr",
    randomTest = "Náhodný test",
    randomTestShort = "Náhodný",
    notPassed = "Neúspěšný test",
    allTests = "Všechny testy",
    notStarted = "Nezahájený",
    resultSummary = { score, max, passed -> "$score / $max · ${if (passed) "Úspěšný" else "Neúspěšný"}" },
    testName = { "Test $it" },
    unfinishedTest = "Nedokončený test",
    resumeSummary = { answered, total -> "zodpovězeno $answered z $total" },
    continueTest = "Pokračovat",
    resumePromptTitle = "Tento test je už zahájený",
    adGateTitle = "Nejdřív krátká reklama",
    adGateMessage = { "Test $it začne po krátké reklamě, díky které je aplikace zdarma." },
    adGateWatch = "Přehrát reklamu",
    adGateDismiss = "Teď ne",
    startOver = "Začít znovu",
    passedPromptTitle = "Tento test je už úspěšný",
    passedPromptMessage = { score, max ->
        "Dosáhli jste zde $score / $max, což stačí k úspěchu. Nový test začne od začátku " +
            "a o toto skóre přijdete."
    },
    keepScore = "Ponechat skóre",
    inProgress = "Rozpracovaný",
    practiceMistakes = "Procvičit chyby",
    mistakesToReview = { if (it == 0) "Žádné chyby k zopakování" else "K zopakování: $it" },
    mistakesTestName = "Chyby",
    rateCardTitle = "Baví vás aplikace?",
    rateCardMessage = { store -> "Rychlé hodnocení v $store pomůže dalším řidičům najít aplikaci." },
    rateCardAction = "Ohodnotit aplikaci",
    rateCardDismiss = "Zavřít",
    questionsAnswered = "Zodpovězené otázky",
    questionCounter = { number -> "$number" },
    questionCount = { total -> "/ $total" },
    answeredCount = { "$it " + westSlavicPlural(it, "zodpovězená", "zodpovězené", "zodpovězených") },
    skippedCount = { "$it " + westSlavicPlural(it, "nezodpovězená", "nezodpovězené", "nezodpovězených") },
    points = { "za $it " + westSlavicPlural(it, "bod", "body", "bodů") },
    questionList = "Seznam otázek",
    closeQuestionList = "Zavřít seznam otázek",
    previousQuestion = "Předchozí otázka",
    nextQuestion = "Další otázka",
    previousSituation = "Předchozí situace",
    nextSituation = "Následující situace",
    moreOptions = "Další možnosti",
    finishTest = "Ukončit test",

    answerAllRandomly = "Odpovědět náhodně (dev)",
    answerAllCorrectly = "Odpovědět správně (dev)",
    yourAnswer = "VAŠE ODPOVĚĎ",
    correctAnswer = "SPRÁVNÁ ODPOVĚĎ",
    enlargeImage = "Zvětšit obrázek",
    closeImage = "Zavřít obrázek",
    skippedQuestion = "NEZODPOVĚZENÁ OTÁZKA",
    allQuestionsAnswered = "Všechny otázky jsou zodpovězené",
    overTime = { minutes -> "Překročili jste čas: test má $minutes minut" },
    finish = "Dokončit",
    testFinished = "Test je dokončený",
    showResults = "Výsledky",
    leaveTestTitle = "Opustit test?",
    leaveTestMessage = "Vaše odpovědi jsou uložené a k testu se můžete vrátit později.",
    leaveTest = "Opustit",
    keepTesting = "Pokračovat v testu",
    resultsTitle = "Výsledky",
    passedStamp = "Vyhověl",
    failedStamp = "Nevyhověl",
    correctCount = "Správně",
    wrongCount = "Špatně",
    unansweredCount = "Nezodpovězené",
    passMark = { "K úspěchu potřebujete $it %" },
    passMarkShort = { "hranice $it %" },
    pointsLabel = "bodů",
    minutesShort = { "$it min" },
    bySection = "Podle okruhů",
    reviewAnswers = "Prohlédnout odpovědi",
    backToTests = "Zpět na testy",
    backToList = "Zpět na seznam",
    drillPromptTitle = "Tento nácvik už začal",
    settingsTitle = "Nastavení",
    settingsLanguage = "Jazyk aplikace",
    settingsAnswers = "Odpovědi",
    settingsCheckImmediately = "Ihned vyhodnotit odpověď",
    settingsCheckImmediatelyDescription = "Po výběru odpovědi hned uvidíte, zda je správná. " +
        "Odpověď pak už nelze změnit.",
    settingsAdvanceAfterAnswer = "Po odpovědi přejít dál",
    settingsAdvanceAfterAnswerDescription = "Další otázka se otevře hned po výběru odpovědi.\n" +
        "Dostupné jen tehdy, když se odpověď nevyhodnocuje ihned.",
    settingsExam = "Zkouška",
    settingsChangeExam = "Změnit zkoušku",
    aboutTitle = "O aplikaci",
    settingsApp = "Aplikace",
    aboutApp = "O aplikaci",
    aboutVersion = { "Verze $it" },
    settingsDeviceIdentifier = "Identifikátor zařízení",
    settingsDeviceIdentifierDescription = "Pod tímto identifikátorem evidujeme statistiky používání " +
        "a hlášení o pádech z tohoto zařízení. Zůstává tu, abyste ho mohli uvést, když nám " +
        "napíšete — bez ohledu na to, zda jsou přepínače zapnuté.",
    settingsDeviceIdentifierEmpty = "Z tohoto zařízení zatím nebylo nic odesláno, takže pod žádným " +
        "identifikátorem nic neevidujeme. Zobrazí se tu při prvním zapnutí statistik používání " +
        "nebo hlášení o pádech.",
    aboutLibraries = "Knihovny s otevřeným kódem",
    aboutLibrariesIntro = "Nej Autoškola staví na těchto open source projektech. Klepnutím na " +
        "některý z nich zobrazíte jeho licenci i všechny, kdo ho napsali.",
    aboutLibrariesSearch = "Hledat knihovny",
    aboutLibrariesNothingFound = "Hledanému výrazu neodpovídá žádná knihovna",
    feedbackTitle = "Poslat zpětnou vazbu",
    feedbackIntro = "Našli jste chybu, máte nápad nebo se chcete zeptat na svá data? Napište nám o tom.",
    feedbackSendTo = { "Pošlete svou zpětnou vazbu e-mailem na: $it" },
    feedbackMessageHint = "Vaše zpráva…",
    feedbackKindFeedback = "Zpětná vazba",
    feedbackKindPrivacyRequest = "Žádost o data",
    feedbackRequestHint = "Co byste chtěli, abychom udělali — například poslat kopii toho, co o vás " +
        "evidujeme, nebo to smazat.",
    feedbackPrivacyRequestNote = "Aplikace do tohoto e-mailu vloží identifikátor z nastavení, abychom " +
        "našli, co o vás evidujeme.",
    feedbackPrivacyRequestNoIdentifier = "Z tohoto zařízení zatím nebylo nic odesláno, takže není co " +
        "přidat. Identifikátor se objeví po zapnutí statistik používání nebo hlášení o pádech.",
    feedbackIncludeDeviceInfo = "Přidat informace o zařízení",
    feedbackIncludeDeviceInfoDescription = "Model telefonu, verze systému a verze aplikace.",
    feedbackSend = "Odeslat",
    feedbackMessageMissing = "Nejprve napište zprávu.",
    feedbackNoEmailApp = "V tomto zařízení nebyla nalezena e-mailová aplikace.",
    feedbackIncludeDetails = "Přidejte to do e-mailu",
    feedbackCopy = "Kopírovat",
    feedbackCopied = "Zkopírováno",
    feedbackSubject = { "Zpětná vazba: $it" },
    feedbackRequestSubject = { "Žádost o data: $it" },
    feedbackVersionLine = { "Verze aplikace: $it" },
    feedbackDeviceLine = { "Zařízení: $it" },
    feedbackIdentifierLine = { "Identifikátor zařízení: $it" },
)

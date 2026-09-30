package uno.lux.autoskola.ui

import uno.lux.autoskola.TestResult
import uno.lux.autoskola.bank.LicenseCategory
import uno.lux.autoskola.bank.PracticeTest
import uno.lux.autoskola.bank.Question
import uno.lux.autoskola.bank.QuestionBankInfo
import uno.lux.autoskola.bank.Section
import uno.lux.autoskola.home.HomeProgress
import uno.lux.autoskola.home.ResumableTest
import uno.lux.autoskola.home.ResumePrompt
import uno.lux.autoskola.learn.SignItem
import uno.lux.autoskola.onboarding.ExamLabel
import uno.lux.autoskola.practice.PracticeQuestion
import uno.lux.autoskola.stats.AttemptKind
import uno.lux.autoskola.stats.AttemptSummary
import uno.lux.autoskola.stats.SectionSummary

// The sample bank and progress the `@Preview`s are drawn from.
//
// A preview can't have a ViewModel, so every previewed screen is handed a `UiState` built here
// instead. The numbers are small and the text is short on purpose: a preview is there to show the
// layout, not to reproduce a whole exam.
//
// Question pictures are left out (`imageUri` stays `null`): the loader fetches from the bundled
// resources, which a preview has no way to reach, so an image would only ever draw its placeholder.
// The layout that holds the picture is the same either way.

/** The exam every preview is taken in: the Slovak car licence, shown in the English UI. */
internal val previewExam = ExamLabel(LicenseCategory.B, "sk")

/** The exam label of the English question bank, for the previews that show a bank's own name. */
internal val previewExamHungarian = ExamLabel(LicenseCategory.B, "hu")

/**
 * The signs the road-sign previews list. The pictures are not loadable from a preview, so their
 * URIs point nowhere: the row draws the same box it draws while an image is still loading.
 */
internal val previewSignItems = listOf(
    previewSignItem("140", "Chodci", "Značka upozorňuje na zvýšený pohyb chodcov na vozovke."),
    previewSignItem(
        "141",
        "Deti",
        "Značka upozorňuje na zvýšený pohyb detí a zvýšené riziko ich neočakávaného vstupu do vozovky.",
    ),
    previewSignItem("142", "Priechod pre chodcov", "Značka upozorňuje na priechod pre chodcov."),
    previewSignItem(
        "145",
        "Zver",
        "Značka upozorňuje na zvýšené nebezpečenstvo neočakávaného pohybu voľne žijúcej zveri cez cestu.",
    ),
    previewSignItem("230", "Zákaz vjazdu", "Značka zakazuje vjazd všetkými vozidlami."),
    previewSignItem("350", "", "Značka informuje o čísle cesty I. triedy alebo o čísle cesty II. triedy."),
)

internal fun previewSignItem(number: String, name: String, description: String) = SignItem(
    number = number,
    name = name,
    description = description,
    imageUri = "preview://minv-sk/obr3/dz/$number.jpg",
)

/** The sections the sample questions belong to, as the question list groups them. */
internal val previewSections = listOf(
    Section(id = 1, title = "Road signs", questionsPerTest = 2, points = 1),
    Section(id = 2, title = "Right of way", questionsPerTest = 1, points = 2),
)

/** One sample question; [id] decides the text, so a preview can pick the one it needs. */
internal fun previewQuestion(id: Int, section: Int = 1, points: Int = 1) = Question(
    id = id,
    section = section,
    text = previewQuestionText(id),
    answers = previewAnswers(id),
    correct = 0,
    points = points,
    groups = listOf(LicenseCategory.B),
)

/** Question text long enough to wrap, and short enough to fit a preview without scrolling to it. */
internal fun previewQuestionText(id: Int): String = when (id % 4) {
    0 -> "This traffic sign stands 50 m before a railway crossing. What must you do?"
    1 -> "You are driving at 60 km/h outside a built-up area. What is the highest speed allowed here?"
    2 -> "What does this sign mean?"
    else -> "Who has right of way at an unmarked crossroads?"
}

/** Answer 0 is the right one, as the app shows a correct and a wrong pick. */
internal fun previewAnswers(id: Int): List<String> = when (id % 4) {
    1 -> listOf("90 km/h for a car", "70 km/h for a car", "80 km/h for a car", "110 km/h for a car")
    2 -> listOf("No entry", "Give way to oncoming traffic", "Stop")
    else -> listOf("Give way to traffic from the right", "You have right of way", "Whoever is quicker")
}

/** [count] sample questions in the shape the practice screen receives them: the last one is from
 * the second section, so the question list has two groups. */
internal fun previewPracticeQuestions(count: Int = 4) = (1..count).map { id ->
    val section = if (id < count) 1 else 2
    PracticeQuestion(
        question = previewQuestion(id = id, section = section, points = if (section == 2) 2 else 1),
        imageUri = null,
    )
}

/** A numbered practice test of the sample questions. */
internal fun previewTest(number: Int, questionCount: Int = 4) = PracticeTest(
    number = number,
    questions = previewPracticeQuestions(questionCount).map { it.question },
)

/** The tests the home screen lists, numbered 1..[count] so a grid has something to fill. */
internal fun previewTests(count: Int = 8) = (1..count).map { previewTest(it) }

/** The exam banks the wizard offers, as `banks/index.json` lists them. */
internal fun previewBanks() = listOf(
    previewBankInfo("sk-sk", "SK", "sk"),
    previewBankInfo("sk-hu", "SK", "hu"),
    previewBankInfo("sk-en", "SK", "en"),
)

/** One entry of `banks/index.json`, with every category this fixture's exam has questions for. */
internal fun previewBankInfo(id: String, country: String = "SK", language: String = "sk") = QuestionBankInfo(
    id = id,
    version = 1,
    country = country,
    language = language,
    questionCounts = mapOf(
        LicenseCategory.A to 420,
        LicenseCategory.B to 1150,
        LicenseCategory.BE to 180,
        LicenseCategory.C to 640,
        LicenseCategory.D to 210,
        LicenseCategory.T to 96,
    ),
)

/** An unfinished test, ready to be picked up again. */
internal fun previewResumable(answered: Int = 12, total: Int = 40) = ResumableTest(
    attemptId = "preview-attempt",
    kind = AttemptKind.PracticeTest,
    testNumber = 7,
    answered = answered,
    total = total,
)

/** A test the user picked that is already under way, so the home screen asks what to do with it. */
internal fun previewResumePrompt(testNumber: Int = 7, answered: Int = 12, total: Int = 40) = ResumePrompt(
    attemptId = "preview-attempt",
    testNumber = testNumber,
    answered = answered,
    total = total,
)

/** A finished attempt's score, keyed by test number the way the home screen reads it. */
internal fun previewResults(vararg results: Pair<Int, TestResult>): Map<Int, TestResult> = mapOf(*results)

/**
 * The progress the home screen shows: [results] are the finished tests, [inProgress] the numbers
 * with an attempt still open, and [resumable] the one the resume card offers.
 */
internal fun previewProgress(
    results: Map<Int, TestResult> = emptyMap(),
    inProgress: Set<Int> = emptySet(),
    resumable: ResumableTest? = null,
    mistakeCount: Int = 0,
    questionsAnswered: Int = 0,
    questionPoolSize: Int = 1150,
) = HomeProgress(
    results = results,
    inProgress = inProgress,
    resumable = resumable,
    mistakeCount = mistakeCount,
    questionsAnswered = questionsAnswered,
    questionPoolSize = questionPoolSize,
)

/**
 * How a finished attempt went, as the results screen reads it: [correct] and [wrong] answers, the
 * rest left blank, and a per-section breakdown of the same.
 */
internal fun previewSummary(
    correct: Int = 36,
    wrong: Int = 3,
    unanswered: Int = 1,
    sections: List<SectionSummary> = previewSectionSummaries(correct, wrong, unanswered),
) = AttemptSummary(
    // Three points a question, so 36 of 40 is 90 % - the pass mark.
    result = TestResult(score = correct * 3, maxScore = (correct + wrong + unanswered) * 3),
    correct = correct,
    wrong = wrong,
    unanswered = unanswered,
    sections = sections,
)

/** A breakdown that adds up to the same counts, section 1 first. */
internal fun previewSectionSummaries(correct: Int = 36, wrong: Int = 3, unanswered: Int = 1): List<SectionSummary> {
    val firstCorrect = (correct + 1) / 2
    val secondCorrect = correct - firstCorrect
    val firstWrong = (wrong + 1) / 2
    val secondWrong = wrong - firstWrong
    val firstTotal = firstCorrect + firstWrong + (unanswered + 1) / 2
    val secondTotal = secondCorrect + secondWrong + unanswered / 2
    return listOf(
        SectionSummary(
            section = 1,
            correct = firstCorrect,
            total = firstTotal,
            score = firstCorrect * 3,
            maxScore = firstTotal * 3,
        ),
        SectionSummary(
            section = 2,
            correct = secondCorrect,
            total = secondTotal,
            score = secondCorrect * 3,
            maxScore = secondTotal * 3,
        ),
    )
}

package com.spiraclestudios.autoskola.screenshots

import android.app.Application
import android.graphics.Bitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.spiraclestudios.autoskola.MainActivity
import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.bank.PracticeTest
import com.spiraclestudios.autoskola.bank.Question
import com.spiraclestudios.autoskola.bank.QuestionBankInfo
import com.spiraclestudios.autoskola.data.answers.PracticeAnswerRepository
import com.spiraclestudios.autoskola.data.banks.QuestionBankRepository
import com.spiraclestudios.autoskola.data.exam.CurrentExam
import com.spiraclestudios.autoskola.data.exam.ExamRepository
import com.spiraclestudios.autoskola.data.exam.ExamSession
import com.spiraclestudios.autoskola.data.preferences.CURRENT_LEGAL_VERSION
import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository
import com.spiraclestudios.autoskola.data.preferences.TestSelection
import com.spiraclestudios.autoskola.data.review.ReviewRepository
import com.spiraclestudios.autoskola.di.appModule
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.navigation.StoredNavigationState
import com.spiraclestudios.autoskola.onboarding.bankTitle
import com.spiraclestudios.autoskola.onboarding.displayName
import com.spiraclestudios.autoskola.provideApplication
import com.spiraclestudios.autoskola.situations.situations
import com.spiraclestudios.autoskola.stats.TestAttempt
import com.spiraclestudios.autoskola.stats.score
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.io.File
import java.util.regex.Pattern

/** Marks the store-screenshot test, which a normal instrumented test run leaves out (see `build.gradle.kts`). */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class StoreScreenshot

/**
 * Takes the store listing's screenshots, in every language in [LANGUAGES], on whatever device runs
 * it. It checks nothing: it is a test only because an instrumented test is how something drives the
 * app on a device from outside. `tools/screenshots/take-screenshots` runs it on a phone and a tablet
 * emulator and pulls the files to `store-screenshots/`.
 *
 * Every screen starts from nothing: a fresh Koin over a preferences file of its own, so the app's own
 * data on the device is never touched, and with the ads and the store replaced (see
 * [screenshotModule]). What the screen needs is written through the app's own repositories, and the
 * screen is opened the way the app reopens itself after being killed: from the stored route.
 */
@StoreScreenshot
@RunWith(AndroidJUnit4::class)
class StoreScreenshots {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)

    /** The app's own line between a phone and a tablet layout (`ui/Adaptive.kt`). */
    private val isTablet = context.resources.configuration.smallestScreenWidthDp >= 600

    /**
     * Whether the practice screen's question list is a sheet over the questions rather than a panel
     * beside them: the practice screen's own rule, which gives it a panel only on an Expanded width
     * (840 dp, `ui/Adaptive.kt`) that is not also a Compact height (480 dp). A phone and the 7-inch
     * tablet in portrait have a sheet; the 10-inch tablet in landscape has a panel.
     */
    private val questionListIsASheet = context.resources.configuration.let {
        it.screenWidthDp < 840 || it.screenHeightDp < 480
    }

    /** On the device's shared storage, where `adb pull` can reach it without root. */
    private val output = File(context.getExternalFilesDir(null), OUTPUT_DIRECTORY)

    @Before
    fun setUp() {
        // What MainActivity does before its first frame, done here because the repositories are
        // built before any activity is.
        provideApplication(context.applicationContext as Application)
        output.deleteRecursively()
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun takeStoreScreenshots() {
        for (language in LANGUAGES) {
            val strings = language.strings
            val exam = chooseExam(language)
            capture(language, "02-home", exam) { session ->
                seedProgress(session!!)
                Target("home", waitFor = strings.allTests)
            }
            practice(language, exam)
            capture(language, "04-situation", exam) { session ->
                // Its questions answered, and answered right, so the screen shows the marks.
                val situation = situations(session!!.bank, session.category)[SHOWN_SITUATION - 1]
                val answers = get<PracticeAnswerRepository>()
                for (question in situation.questions) {
                    answers.answer(session.bank.id, session.bank.version, question.id, question.correct)
                }
                Target(
                    "home/learn/intersections/situation/$SHOWN_SITUATION",
                    waitFor = strings.situationName(SHOWN_SITUATION),
                )
            }
            capture(language, "05-results", exam) { session ->
                val id = seedProgress(session!!).passed.id
                // On top of its own test, which is where the app puts it.
                Target("home/practice/$id/results/$id", waitFor = strings.reviewAnswers)
            }
            // Where the list is a sheet it covers the question when open, which makes it a picture of its
            // own; where it is a panel beside the question, the practice screenshot already shows it.
            if (questionListIsASheet) {
                capture(
                    language,
                    "06-practice-questions",
                    exam,
                    beforeShot = {
                        tapDescribed(strings.questionList)
                        await(strings.closeQuestionList, by = By::desc)
                    },
                ) { session ->
                    val progress = seedProgress(session!!)
                    Target("home/practice/${progress.underWay.id}", waitFor = progress.resumedOn.prompt())
                }
            }
            capture(language, "07-signs", exam) {
                Target("home/learn/signs", waitFor = strings.learnRoadSigns)
            }
        }
    }

    /**
     * The test under way, on question [SHOWN_ON_PHONE] or [SHOWN_ON_TABLET] with its right answer
     * picked. It opens on the first question with no answer, as the app does, and steps back from
     * there with the previous-question button.
     */
    private fun practice(language: AppLanguage, exam: TestSelection) {
        val shown = if (isTablet) SHOWN_ON_TABLET else SHOWN_ON_PHONE
        lateinit var progress: Progress
        capture(
            language,
            "03-practice",
            exam,
            beforeShot = {
                repeat(ANSWERED_UNDER_WAY + 1 - shown) {
                    tapDescribed(language.strings.previousQuestion)
                    device.waitForIdle()
                }
                // The question and its right answer together: questions about signs share a text.
                val question = progress.questions[shown - 1]
                await(question.prompt())
                await(question.answers[question.correct].take(PROMPT_LENGTH))
            },
        ) { session ->
            progress = seedProgress(session!!)
            Target("home/practice/${progress.underWay.id}", waitFor = progress.resumedOn.prompt())
        }
    }

    /**
     * Walks the exam selection the way a user would - [COUNTRY], then its exam in [language] - and
     * takes the screenshot on its last page, where the license group is picked. Then it picks
     * [CATEGORY] there and returns what the app stored, which is the exam every other screenshot in
     * this language is taken with.
     *
     * The exam is in the app's language rather than in a language of its own, so a listing is in one
     * language throughout. A language with no exam in [COUNTRY] fails the run here.
     */
    private fun chooseExam(language: AppLanguage): TestSelection {
        val strings = language.strings
        lateinit var bank: QuestionBankInfo
        var chosen: TestSelection? = null
        capture(
            language,
            "01-exam-selection",
            exam = null,
            beforeShot = {
                tap(strings.countryName(COUNTRY))
                tap(bankTitle(bank.language))
                await(CATEGORY.displayName(bank.language))
            },
            afterShot = { koin ->
                tap(CATEGORY.displayName(bank.language))
                chosen = runBlocking {
                    withTimeout(LOAD_TIMEOUT_MILLIS) {
                        koin.get<PreferencesRepository>().preferences.mapNotNull { it.selection }.first()
                    }
                }
            },
        ) {
            bank = get<QuestionBankRepository>().index()
                .firstOrNull { it.country == COUNTRY && it.language == language.code }
                ?: error("There is no $COUNTRY exam in ${language.nativeName} to take its screenshots with")
            Target(route = null, waitFor = strings.countryName(COUNTRY))
        }
        return chosen!!
    }

    /** Which screen to open, and a text that is on it once it has loaded. */
    private class Target(val route: String?, val waitFor: String)

    /**
     * Opens one screen from a clean start and saves what the device shows as
     * `<language>/<name>.png`.
     *
     * [prepare] writes what the screen needs and says where to go. It is given the session of [exam],
     * or `null` while [exam] is `null` and the app is still asking for one. [beforeShot] runs once
     * the screen is showing, to get it to the state to shoot, and [afterShot] while it is still open.
     */
    private fun capture(
        language: AppLanguage,
        name: String,
        exam: TestSelection?,
        beforeShot: () -> Unit = {},
        afterShot: (Koin) -> Unit = {},
        prepare: suspend Koin.(ExamSession?) -> Target,
    ) {
        stopKoin()
        val settings = screenshotSettings(context)
        val koin = startKoin {
            allowOverride(true)
            modules(appModule, screenshotModule(settings))
        }.koin

        val target = runBlocking {
            val preferences = koin.get<PreferencesRepository>()
            // Past onboarding, which has screens of its own and is not what a listing shows.
            preferences.acceptLegal(CURRENT_LEGAL_VERSION)
            preferences.setDisclaimerRead(true)
            preferences.selectLanguage(language)
            // The card asking for a rating would sit on top of the home screen.
            koin.get<ReviewRepository>().rated()
            val session = exam?.let {
                preferences.select(it)
                withTimeout(LOAD_TIMEOUT_MILLIS) {
                    koin.get<ExamRepository>().current.filterIsInstance<CurrentExam.Ready>().first().session
                }
            }
            koin.prepare(session)
        }
        target.route?.let { StoredNavigationState(settings).record(it) }

        ActivityScenario.launch(MainActivity::class.java).use {
            await(target.waitFor, "$name (${language.code})")
            beforeShot()
            // The text is there; the pictures Coil loads, and the last frames of any animation, follow it.
            device.waitForIdle()
            Thread.sleep(SETTLE_MILLIS)
            save(language, name)
            afterShot(koin)
        }
    }

    /**
     * Waits until [text] is on screen - as an element's text, or whatever [by] matches instead, such
     * as a button's description. [what] names the screenshot in the failure.
     */
    private fun await(text: String, what: String = "The screen", by: (Pattern) -> BySelector = By::text) {
        val shown = device.wait(Until.hasObject(by(containing(text))), LOAD_TIMEOUT_MILLIS)
        check(shown) { "$what: \"$text\" never appeared on screen" }
    }

    /** Taps the element showing exactly [text], once it appears. */
    private fun tap(text: String) {
        val element = device.wait(Until.findObject(By.text(text)), LOAD_TIMEOUT_MILLIS)
            ?: error("\"$text\" never appeared on screen to be tapped")
        element.click()
    }

    /** Taps the button described as [description] - an icon has no text - once it appears. */
    private fun tapDescribed(description: String) {
        val element = device.wait(Until.findObject(By.desc(description)), LOAD_TIMEOUT_MILLIS)
            ?: error("No \"$description\" button appeared on screen to be tapped")
        element.click()
    }

    private fun save(language: AppLanguage, name: String) {
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
            ?: error("$name (${language.code}): the device returned no screenshot")
        val file = File(output, "${language.code}/$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** What [seedProgress] leaves behind for the screens to open. */
    private class Progress(val passed: TestAttempt, val underWay: TestAttempt, val questions: List<Question>) {
        /** The question the practice screen opens on: the first one with no answer. */
        val resumedOn: Question get() = questions[ANSWERED_UNDER_WAY]
    }

    /**
     * A user well into their preparation, and the same one every time: every test up to
     * [LAST_FINISHED] finished, most passed and [BADLY_FAILED_TEST] and the ones in [FAILED_TESTS]
     * failed, and test [UNDER_WAY_TEST] started and left after [ANSWERED_UNDER_WAY] questions - the
     * one the home screen offers to resume. It is what makes the home screen look used, and it gives the
     * practice and results screens an attempt each.
     */
    private suspend fun seedProgress(session: ExamSession): Progress {
        lateinit var passed: TestAttempt
        for (test in session.tests.filter { it.number <= LAST_FINISHED && it.number != UNDER_WAY_TEST }) {
            val failed = test.number in FAILED_TESTS || test.number == BADLY_FAILED_TEST
            val attempt = finish(
                session,
                test,
                wrong = when {
                    test.number == BADLY_FAILED_TEST -> test.wrongToScore(BADLY_FAILED_SCORE)
                    failed -> 7
                    else -> test.number % 4
                },
                // Somewhere between 18 and 30 minutes, the same for a test every time.
                elapsedSeconds = 18 * 60L + test.number * 97 % (12 * 60),
                expectPassed = !failed,
            )
            if (test.number == RESULTS_TEST) passed = attempt
        }

        val test = session.tests.first { it.number == UNDER_WAY_TEST }
        val underWay = session.startOrResume(test)
        val questions = session.questionsOf(underWay)
        questions.take(ANSWERED_UNDER_WAY).forEachIndexed { index, question ->
            val chosen = if (index + 1 in WRONG_UNDER_WAY) question.wrongAnswer else question.correct
            session.answer(underWay.id, question, chosen)
        }
        session.updateElapsed(underWay.id, 9 * 60 + 14)
        return Progress(passed, underWay, questions)
    }

    /**
     * Answers all of [test], [wrong] of them wrongly, and finishes it. The wrong ones are the cheapest
     * questions in a test meant to pass and the dearest in one meant to fail, so either comes out as
     * meant.
     */
    private suspend fun finish(
        session: ExamSession,
        test: PracticeTest,
        wrong: Int,
        elapsedSeconds: Long,
        expectPassed: Boolean,
    ): TestAttempt {
        val attempt = session.startOrResume(test)
        val questions = session.questionsOf(attempt)
        val byPoints = questions.sortedBy { it.points }
        val wrongOnes = if (expectPassed) byPoints.take(wrong) else byPoints.takeLast(wrong)
        val wrongIds = wrongOnes.mapTo(mutableSetOf()) { it.id }
        for (question in questions) {
            val chosen = if (question.id in wrongIds) question.wrongAnswer else question.correct
            session.answer(attempt.id, question, chosen)
        }
        val answered = session.attempt(attempt.id).first()!!
        check(answered.score(questions).passed == expectPassed) {
            "Test ${test.number} with $wrong wrong answers was meant to ${if (expectPassed) "pass" else "fail"}"
        }
        session.finish(attempt.id, elapsedSeconds)
        return answered
    }

    /**
     * How many wrong answers bring this test down to about [score] points: the dearest questions
     * first, until no more than [score] is left.
     */
    private fun PracticeTest.wrongToScore(score: Int): Int {
        var left = questions.sumOf { it.points }
        var wrong = 0
        for (question in questions.sortedByDescending { it.points }) {
            if (left <= score) break
            left -= question.points
            wrong++
        }
        return wrong
    }

    /** Always the same wrong answer: the one after the right one. */
    private val Question.wrongAnswer: Int get() = (correct + 1) % answers.size

    /** The start of the question's text, enough to find it on screen by. */
    private fun Question.prompt(): String = text.lineSequence().first().take(PROMPT_LENGTH)

    private fun containing(text: String): Pattern =
        Pattern.compile(".*" + Pattern.quote(text.trim()) + ".*", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)

    private companion object {
        /** The languages a listing is made in. Each takes its exam in the same language (see [chooseExam]). */
        val LANGUAGES = listOf(AppLanguage.Slovak, AppLanguage.English)

        /** The country whose exam the screenshots show, and the license group picked for it. */
        const val COUNTRY = "SK"
        val CATEGORY = LicenseCategory.B

        /** The tests [seedProgress] finishes: all of them up to this one, but for the one under way. */
        const val LAST_FINISHED = 34

        /** Of the finished tests, the ones that were failed. Every other one was passed. */
        val FAILED_TESTS = setOf(5, 11, 18, 29)

        /** A test failed by a long way, near the top of the home screen: [BADLY_FAILED_SCORE] points or just under. */
        const val BADLY_FAILED_TEST = 2
        const val BADLY_FAILED_SCORE = 45

        /** The test that was started and not finished, which the practice screenshots show. */
        const val UNDER_WAY_TEST = 23

        /** How many of its questions are answered, and which of them (counted from 1) wrongly. */
        const val ANSWERED_UNDER_WAY = 19
        val WRONG_UNDER_WAY = setOf(4, 9, 13, 16)

        /** The question of [UNDER_WAY_TEST] the practice screenshot shows, counted from 1. */
        const val SHOWN_ON_PHONE = 18
        const val SHOWN_ON_TABLET = 19

        /** The passed test whose results are shown: three wrong answers. */
        const val RESULTS_TEST = 3

        /** The intersection situation shown, counted from 1. */
        const val SHOWN_SITUATION = 27

        /** How much of a text is matched when looking for it on screen. */
        const val PROMPT_LENGTH = 30

        /** Under the app's external files directory; `take-screenshots` pulls it from there. */
        const val OUTPUT_DIRECTORY = "store-screenshots"

        const val LOAD_TIMEOUT_MILLIS = 30_000L
        const val SETTLE_MILLIS = 1_500L
    }
}

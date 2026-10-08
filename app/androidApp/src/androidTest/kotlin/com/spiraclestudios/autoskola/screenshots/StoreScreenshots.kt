package com.spiraclestudios.autoskola.screenshots

import android.app.Application
import android.graphics.Bitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.spiraclestudios.autoskola.MainActivity
import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.bank.PracticeTest
import com.spiraclestudios.autoskola.bank.Question
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
import com.spiraclestudios.autoskola.provideApplication
import com.spiraclestudios.autoskola.stats.TestAttempt
import com.spiraclestudios.autoskola.stats.score
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
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
        for ((language, bankId) in LANGUAGES) {
            val strings = language.strings
            capture(language, bankId, "01-exam-selection", examChosen = false) {
                Target(route = null, waitFor = strings.examSelectionCountrySubtitle)
            }
            capture(language, bankId, "02-signs") {
                Target("home/learn/signs", waitFor = strings.learnRoadSigns)
            }
            capture(language, bankId, "03-situation") {
                Target("home/learn/intersections/situation/1", waitFor = strings.situationName(1))
            }
            capture(language, bankId, "04-home") { session ->
                seedProgress(session!!)
                Target("home", waitFor = strings.allTests)
            }
            capture(language, bankId, "05-practice") { session ->
                val progress = seedProgress(session!!)
                val id = progress.underWay.id
                Target("home/practice/$id", waitFor = progress.nextQuestion.text.lineSequence().first().take(30))
            }
            capture(language, bankId, "06-results") { session ->
                val id = seedProgress(session!!).passed.id
                // On top of its own test, which is where the app puts it.
                Target("home/practice/$id/results/$id", waitFor = strings.reviewAnswers)
            }
        }
    }

    /** Which screen to open, and a text that is on it once it has loaded. */
    private class Target(val route: String?, val waitFor: String)

    /**
     * Opens one screen from a clean start and saves what the device shows as
     * `<language>/<name>.png`. [prepare] writes what the screen needs - given the exam, unless
     * [examChosen] is false and the app is still asking for one - and says where to go.
     */
    private fun capture(
        language: AppLanguage,
        bankId: String,
        name: String,
        examChosen: Boolean = true,
        prepare: suspend (ExamSession?) -> Target,
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
            val session = if (examChosen) {
                preferences.select(TestSelection(bankId, LicenseCategory.B))
                withTimeout(LOAD_TIMEOUT_MILLIS) {
                    koin.get<ExamRepository>().current.filterIsInstance<CurrentExam.Ready>().first().session
                }
            } else {
                null
            }
            prepare(session)
        }
        target.route?.let { StoredNavigationState(settings).record(it) }

        ActivityScenario.launch(MainActivity::class.java).use {
            val shown = device.wait(Until.hasObject(By.text(containing(target.waitFor))), LOAD_TIMEOUT_MILLIS)
            check(shown) { "$name (${language.code}): \"${target.waitFor}\" never appeared on screen" }
            // The text is there; the pictures Coil loads, and the last frames of any animation, follow it.
            device.waitForIdle()
            Thread.sleep(SETTLE_MILLIS)
            save(language, name)
        }
    }

    private fun save(language: AppLanguage, name: String) {
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
            ?: error("$name (${language.code}): the device returned no screenshot")
        val file = File(output, "${language.code}/$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** What [seedProgress] leaves behind for the screens to open. */
    private class Progress(val passed: TestAttempt, val underWay: TestAttempt, val nextQuestion: Question)

    /**
     * A user some way into their preparation: two tests passed, one failed, and a fourth under way. It
     * is what makes the home screen look used, and it gives the practice and results screens an
     * attempt each.
     */
    private suspend fun seedProgress(session: ExamSession): Progress {
        val tests = session.tests
        finish(session, tests[0], wrong = 0, elapsedSeconds = 21 * 60 + 12, expectPassed = true)
        val passed = finish(session, tests[1], wrong = 2, elapsedSeconds = 24 * 60 + 35, expectPassed = true)
        finish(session, tests[2], wrong = 7, elapsedSeconds = 28 * 60 + 3, expectPassed = false)

        val underWay = session.startOrResume(tests[3])
        val questions = session.questionsOf(underWay)
        // Stops before a question with a picture, so that is the one the practice screen opens on.
        val stopAt = questions.indices.firstOrNull { it >= 5 && questions[it].image != null } ?: 5
        questions.take(stopAt).forEach { session.answer(underWay.id, it, it.correct) }
        session.updateElapsed(underWay.id, 6 * 60 + 48)
        return Progress(passed, underWay, questions[stopAt])
    }

    /**
     * Answers all of [test], [wrong] of them wrongly - the ones worth the most points, so a failed
     * test is certain to fail - and finishes it.
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
        val wrongIds = questions.sortedByDescending { it.points }.take(wrong).mapTo(mutableSetOf()) { it.id }
        for (question in questions) {
            val wrongAnswer = (question.correct + 1) % question.answers.size
            val chosen = if (question.id in wrongIds) wrongAnswer else question.correct
            session.answer(attempt.id, question, chosen)
        }
        val answered = session.attempt(attempt.id).first()!!
        check(answered.score(questions).passed == expectPassed) {
            "Test ${test.number} with $wrong wrong answers was meant to ${if (expectPassed) "pass" else "fail"}"
        }
        session.finish(attempt.id, elapsedSeconds)
        return answered
    }

    private fun containing(text: String): Pattern =
        Pattern.compile(".*" + Pattern.quote(text.trim()) + ".*", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)

    private companion object {
        /** The languages a listing is made in, each with the question bank shown under it. */
        val LANGUAGES = listOf(
            AppLanguage.Slovak to "sk-sk",
            AppLanguage.English to "sk-en",
        )

        /** Under the app's external files directory; `take-screenshots` pulls it from there. */
        const val OUTPUT_DIRECTORY = "store-screenshots"

        const val LOAD_TIMEOUT_MILLIS = 30_000L
        const val SETTLE_MILLIS = 1_500L
    }
}

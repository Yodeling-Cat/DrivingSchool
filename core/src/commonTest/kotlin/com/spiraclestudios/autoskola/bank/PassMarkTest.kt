package com.spiraclestudios.autoskola.bank

import com.spiraclestudios.autoskola.TestResult
import com.spiraclestudios.autoskola.stats.TestAttempt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PassMarkTest {

    private val czech = PassMark(points = 43, outOf = 50)

    @Test
    fun aScoreAtThePassMarkPassesAndOneBelowItFails() {
        assertTrue(czech.isMetBy(score = 43, maxScore = 50))
        assertFalse(czech.isMetBy(score = 42, maxScore = 50))
        assertTrue(PassMark.Slovak.isMetBy(score = 90, maxScore = 100))
        assertFalse(PassMark.Slovak.isMetBy(score = 89, maxScore = 100))
    }

    @Test
    fun aShorterTestIsMeasuredAgainstTheSameShare() {
        // A retest of mistakes is worth fewer points than the exam; it passes at the same 86 %.
        assertTrue(czech.isMetBy(score = 13, maxScore = 14))
        assertFalse(czech.isMetBy(score = 12, maxScore = 14))
    }

    @Test
    fun aTestWorthNothingIsNotPassed() {
        assertFalse(czech.isMetBy(score = 0, maxScore = 0))
    }

    @Test
    fun thePercentIsRoundedUpToTheWholePercentThatPasses() {
        assertEquals(86, czech.percent)
        assertEquals(90, PassMark.Slovak.percent)
        assertEquals(67, PassMark(points = 2, outOf = 3).percent)
    }

    @Test
    fun aResultIsPassedAgainstItsOwnPassMark() {
        assertTrue(TestResult(score = 44, maxScore = 50, passMark = czech).passed)
        assertFalse(TestResult(score = 44, maxScore = 50).passed)
    }

    @Test
    fun aBankReadsItsPassMark() {
        val bank = QuestionBank.decode(bankJson(""","passMark":{"points":43,"outOf":50}"""))

        assertEquals(czech, bank.passMark)
    }

    @Test
    fun aBankWithoutAPassMarkHasTheSlovakOne() {
        assertEquals(PassMark.Slovak, QuestionBank.decode(bankJson("")).passMark)
    }

    @Test
    fun aResultStoredBeforeResultsKeptTheirPassMarkIsGradedAsItWasThen() {
        // Every attempt saved before banks carried a pass mark was a Slovak one, graded at 90 %.
        val attempt = TestAttempt.decode(
            """{"id":"a","bankId":"sk-sk","bankVersion":1,"category":"B","kind":"PracticeTest",""" +
                """"questionIds":[1],"startedAt":0,"result":{"score":44,"maxScore":50}}""",
        )

        assertEquals(PassMark.Slovak, attempt.result?.passMark)
        assertFalse(attempt.result!!.passed)
    }

    private fun bankJson(extra: String) =
        """{"id":"cz-cs","version":1,"country":"CZ","language":"cs","imageDir":"md-cz",""" +
            """"sections":[],"questions":[]$extra}"""
}

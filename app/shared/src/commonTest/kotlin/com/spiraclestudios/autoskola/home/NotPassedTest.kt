package com.spiraclestudios.autoskola.home

import com.spiraclestudios.autoskola.TestResult
import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.bank.practiceTests
import com.spiraclestudios.autoskola.data.banks.testBank
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotPassedTest {

    private val tests = testBank().practiceTests(LicenseCategory.B)

    private fun result(passed: Boolean) = TestResult(score = if (passed) 38 else 30, maxScore = 40)

    @Test
    fun testsThatWereNeverTakenAreNotPassed() {
        assertEquals(tests, notPassedTests(tests, results = emptyMap()))
    }

    @Test
    fun aPassedTestLeavesTheList() {
        val results = mapOf(1 to result(passed = true))

        assertEquals(listOf(2), notPassedTests(tests, results).map { it.number })
    }

    @Test
    fun aFailedTestIsStillNotPassed() {
        val results = mapOf(1 to result(passed = false))

        assertEquals(listOf(1, 2), notPassedTests(tests, results).map { it.number })
    }

    @Test
    fun passingEveryTestLeavesNothingUnpassed() {
        val results = tests.associate { it.number to result(passed = true) }

        assertEquals(emptyList(), notPassedTests(tests, results))
    }

    @Test
    fun everyTestPassedOnlyOnceNothingIsLeft() {
        assertFalse(everyTestPassed(tests, results = emptyMap()))
        assertFalse(everyTestPassed(tests, mapOf(1 to result(passed = true))))
        assertFalse(everyTestPassed(tests, mapOf(1 to result(passed = true), 2 to result(passed = false))))

        val allPassed = tests.associate { it.number to result(passed = true) }
        assertTrue(everyTestPassed(tests, allPassed))
    }

    @Test
    fun anExamWithNoTestsIsNotAPassedOne() {
        assertFalse(everyTestPassed(tests = emptyList(), results = emptyMap()))
    }
}

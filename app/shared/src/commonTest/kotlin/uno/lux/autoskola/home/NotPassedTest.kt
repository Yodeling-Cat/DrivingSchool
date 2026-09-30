package uno.lux.autoskola.home

import uno.lux.autoskola.TestResult
import uno.lux.autoskola.bank.LicenseCategory
import uno.lux.autoskola.bank.practiceTests
import uno.lux.autoskola.data.banks.testBank
import kotlin.test.Test
import kotlin.test.assertEquals

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
}

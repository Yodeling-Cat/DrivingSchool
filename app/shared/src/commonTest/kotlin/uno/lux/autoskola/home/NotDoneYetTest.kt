package uno.lux.autoskola.home

import uno.lux.autoskola.TestResult
import uno.lux.autoskola.bank.LicenseCategory
import uno.lux.autoskola.bank.practiceTests
import uno.lux.autoskola.data.banks.testBank
import kotlin.test.Test
import kotlin.test.assertEquals

class NotDoneYetTest {

    private val tests = testBank().practiceTests(LicenseCategory.B)

    private fun result(passed: Boolean) = TestResult(score = if (passed) 38 else 30, maxScore = 40)

    @Test
    fun testsThatWereNeverTakenAreNotDoneYet() {
        assertEquals(tests, notDoneTests(tests, results = emptyMap()))
    }

    @Test
    fun aPassedTestIsDoneAndLeavesTheList() {
        val results = mapOf(1 to result(passed = true))

        assertEquals(listOf(2), notDoneTests(tests, results).map { it.number })
    }

    @Test
    fun aFailedTestIsStillNotDoneYet() {
        val results = mapOf(1 to result(passed = false))

        assertEquals(listOf(1, 2), notDoneTests(tests, results).map { it.number })
    }

    @Test
    fun passingEveryTestLeavesNothingToDo() {
        val results = tests.associate { it.number to result(passed = true) }

        assertEquals(emptyList(), notDoneTests(tests, results))
    }
}

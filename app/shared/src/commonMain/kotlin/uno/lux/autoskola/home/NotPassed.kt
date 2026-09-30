package uno.lux.autoskola.home

import uno.lux.autoskola.TestResult
import uno.lux.autoskola.bank.PracticeTest

/**
 * The tests that have not been passed, which is what the "not passed" pill offers: the ones never
 * taken, the ones under way (an open attempt has no result), and the ones finished below the pass
 * mark. A failed test can be taken again, so it stays on the list; only passing every test empties
 * it.
 */
fun notPassedTests(tests: List<PracticeTest>, results: Map<Int, TestResult>): List<PracticeTest> =
    tests.filter { results[it.number]?.passed != true }

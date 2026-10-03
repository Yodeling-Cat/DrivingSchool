package com.spiraclestudios.autoskola.home

import com.spiraclestudios.autoskola.TestResult
import com.spiraclestudios.autoskola.bank.PracticeTest

/**
 * The tests that have not been passed, which is what the "not passed" pill offers: the ones never
 * taken, the ones under way (an open attempt has no result), and the ones finished below the pass
 * mark. A failed test can be taken again, so it stays on the list; only passing every test empties
 * it.
 */
fun notPassedTests(tests: List<PracticeTest>, results: Map<Int, TestResult>): List<PracticeTest> =
    tests.filter { results[it.number]?.passed != true }

/**
 * Whether the driver has finished and passed every test of the exam, which is the last rung of the
 * rating card's ladder: it is only ever true of an exam that has tests in it, so a bank with none
 * is not a license the driver has earned.
 */
fun everyTestPassed(tests: List<PracticeTest>, results: Map<Int, TestResult>): Boolean =
    tests.isNotEmpty() && notPassedTests(tests, results).isEmpty()

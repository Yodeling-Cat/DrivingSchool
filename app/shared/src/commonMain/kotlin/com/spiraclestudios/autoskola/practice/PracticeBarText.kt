package com.spiraclestudios.autoskola.practice

// The two lines a phone in landscape reads inside the bar, where a taller screen shows them apart:
// the test's name is set beside the question number, and the points beside the clock.

/** E.g. "Test 2 · ", the question the test is on following it in the same line. */
internal fun barQuestionPrefix(testName: String): String = "$testName · "

/**
 * The count a question is drawn against, one space behind the question's own number, e.g. " / 50".
 * The space belongs here rather than in the translation, so a language only holds the numbers.
 */
internal fun barQuestionCount(count: String): String = " $count"

/** E.g. "za 3 body · ", the clock it introduces following in the same line. */
internal fun barPointsLine(points: String): String = "$points · "

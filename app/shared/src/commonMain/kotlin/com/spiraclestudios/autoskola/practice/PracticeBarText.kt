package com.spiraclestudios.autoskola.practice

// The pieces the practice screen's own text is built from: the two lines a phone in landscape reads
// inside the bar (the test's name beside the question number, the points beside the clock) and the
// counts the bar and the question list keep of a test.

/** E.g. "Test 2 · ", the question the test is on following it in the same line. */
internal fun barQuestionPrefix(testName: String): String = "$testName · "

/**
 * The count a question is drawn against, one space behind the question's own number, e.g. " / 50".
 * The space belongs here rather than in the translation, so a language only holds the numbers.
 */
internal fun questionCountRun(count: String): String = " $count"

/**
 * E.g. "7 / 50": the question the bar is on, or how much of a section the user has answered, out of
 * how many the two of them hold.
 */
internal fun questionCounterLabel(number: String, count: String): String = number + questionCountRun(count)

/** E.g. "6 zodpovedaných · 1 nezodpovedaná": how much of the test is behind the user. */
internal fun barAnsweredLine(answered: String, skipped: String): String = "$answered · $skipped"

/** E.g. "za 3 body · ", the clock it introduces following in the same line. */
internal fun barBeforeClock(label: String): String = "$label · "

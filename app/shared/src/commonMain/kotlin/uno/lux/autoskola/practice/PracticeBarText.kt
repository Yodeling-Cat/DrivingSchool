package uno.lux.autoskola.practice

// The two lines a phone in landscape reads inside the bar, where a taller screen shows them apart:
// the test's name is set beside the question number, and the points beside the clock.

/** E.g. "Test 2 · Otázka 9 z 40". */
internal fun barQuestionLine(testName: String, questionOutOf: String): String = "$testName · $questionOutOf"

/** E.g. "za 3 body · ", the clock it introduces following in the same line. */
internal fun barPointsLine(points: String): String = "$points · "

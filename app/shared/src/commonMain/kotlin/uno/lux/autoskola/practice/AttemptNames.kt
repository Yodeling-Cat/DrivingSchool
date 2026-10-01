package uno.lux.autoskola.practice

import androidx.compose.runtime.Composable
import uno.lux.autoskola.i18n.strings
import uno.lux.autoskola.stats.AttemptKind

/** E.g. "Test 5", the name of a mistakes retest, or of a run through the situations. */
@Composable
fun attemptName(kind: AttemptKind, testNumber: Int?): String = when (kind) {
    AttemptKind.PracticeTest -> strings.testName(testNumber ?: 0)
    AttemptKind.Mistakes -> strings.mistakesTestName
    AttemptKind.Situations -> strings.situationsDrillName
}

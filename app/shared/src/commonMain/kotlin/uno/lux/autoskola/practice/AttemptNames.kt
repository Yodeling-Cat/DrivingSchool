package uno.lux.autoskola.practice

import androidx.compose.runtime.Composable
import uno.lux.autoskola.i18n.strings
import uno.lux.autoskola.stats.AttemptKind

/** E.g. "Test 5", or the name of a mistakes retest. */
@Composable
fun attemptName(kind: AttemptKind, testNumber: Int?): String =
    if (kind == AttemptKind.Mistakes) strings.mistakesTestName else strings.testName(testNumber ?: 0)

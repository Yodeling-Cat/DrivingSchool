package com.spiraclestudios.autoskola.practice

import androidx.compose.runtime.Composable
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.stats.AttemptKind

/** E.g. "Test 5", the name of a mistakes retest, or of a run through the situations. */
@Composable
fun attemptName(kind: AttemptKind, testNumber: Int?): String = when (kind) {
    AttemptKind.PracticeTest -> strings.testName(testNumber ?: 0)
    AttemptKind.Mistakes -> strings.mistakesTestName
    AttemptKind.Situations -> strings.situationsDrillName
}

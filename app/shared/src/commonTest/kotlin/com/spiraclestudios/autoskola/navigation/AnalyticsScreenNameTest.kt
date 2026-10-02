package com.spiraclestudios.autoskola.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * The screen names are a contract with the dashboard, and an omission is silent: a key with no name
 * reports nothing, and the numbers simply read lower. These two hold the line.
 */
class AnalyticsScreenNameTest {

    /** Every destination the back stack can hold. A new key belongs in this list. */
    private val everyKey: List<NavKey> = listOf(
        HomeKey,
        LearnKey,
        SignsKey,
        IntersectionsKey,
        SituationKey(1),
        LegislationKey,
        SettingsKey,
        ChangeExamKey,
        AboutKey,
        FeedbackKey,
        PracticeKey("attempt"),
        ResultsKey("attempt"),
    )

    @Test
    fun everyScreenNamesItself() {
        everyKey.forEach { key ->
            assertNotEquals(
                "unknown",
                key.analyticsScreenName(),
                "${key::class.simpleName} has no name in NavKey.analyticsScreenName()",
            )
        }
    }

    @Test
    fun everyScreenNamesItselfDifferently() {
        val names = everyKey.map { it.analyticsScreenName() }

        assertEquals(names.size, names.toSet().size, "two screens report the same name: $names")
    }
}

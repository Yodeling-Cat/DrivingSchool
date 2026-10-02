package com.spiraclestudios.autoskola.data.attempts

import com.russhwolf.settings.MapSettings
import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.stats.AttemptKind
import com.spiraclestudios.autoskola.stats.TestAttempt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsAttemptDataSourceTest {

    private val settings = MapSettings()

    private fun attempt(id: String, bankVersion: Int = 1, category: LicenseCategory = LicenseCategory.B) = TestAttempt(
        id = id,
        bankId = "sk-sk",
        bankVersion = bankVersion,
        category = category,
        kind = AttemptKind.PracticeTest,
        testNumber = 1,
        questionIds = listOf(1, 2),
        startedAt = 0,
    )

    @Test
    fun upsertedAttemptsSurviveRestartAndReplaceByID() = runTest {
        SettingsAttemptDataSource(settings).apply {
            upsert(attempt("a"))
            upsert(attempt("b"))
            upsert(attempt("a").copy(discardedAt = 5))
        }

        val restored = SettingsAttemptDataSource(settings)
        assertEquals(listOf("a", "b"), restored.observe("sk-sk", 1, LicenseCategory.B).first().map { it.id })
        assertEquals(5, restored.observe("a").first()?.discardedAt)
    }

    @Test
    fun observeIsScopedToBankVersionAndGroup() = runTest {
        val source = SettingsAttemptDataSource(settings)
        source.upsert(attempt("v1"))
        source.upsert(attempt("v2", bankVersion = 2))
        source.upsert(attempt("c", category = LicenseCategory.C))

        assertEquals(listOf("v1"), source.observe("sk-sk", 1, LicenseCategory.B).first().map { it.id })
    }

    @Test
    fun unreadableAttemptsAreSkipped() = runTest {
        SettingsAttemptDataSource(settings).upsert(attempt("good"))
        settings.putString("attempts", "good,broken")
        settings.putString("attempt_broken", "{not json")

        val source = SettingsAttemptDataSource(settings)
        assertEquals(listOf("good"), source.observe("sk-sk", 1, LicenseCategory.B).first().map { it.id })
        assertNull(source.observe("broken").first())
    }
}

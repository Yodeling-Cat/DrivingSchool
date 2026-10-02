package com.spiraclestudios.autoskola.data.attempts

import com.russhwolf.settings.Settings
import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.stats.TestAttempt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Stores attempts as one JSON document per key in [Settings], plus an index of ids. All
 * attempts are kept in memory, which is fine for thousands of them.
 */
class SettingsAttemptDataSource(private val settings: Settings) : AttemptDataSource {

    private val attempts = MutableStateFlow(
        settings.getString(KEY_INDEX, "").split(",").filter { it.isNotEmpty() }.mapNotNull { id ->
            settings.getStringOrNull(attemptKey(id))?.let { runCatching { TestAttempt.decode(it) }.getOrNull() }
        },
    )

    override fun observe(bankId: String, bankVersion: Int, category: LicenseCategory): Flow<List<TestAttempt>> =
        attempts.map { all ->
            all.filter { it.bankId == bankId && it.bankVersion == bankVersion && it.category == category }
        }.distinctUntilChanged()

    override fun observe(id: String): Flow<TestAttempt?> =
        attempts.map { all -> all.firstOrNull { it.id == id } }.distinctUntilChanged()

    override suspend fun upsert(attempt: TestAttempt) {
        settings.putString(attemptKey(attempt.id), attempt.encode())
        val all = attempts.value
        if (all.any { it.id == attempt.id }) {
            attempts.value = all.map { if (it.id == attempt.id) attempt else it }
        } else {
            attempts.value = all + attempt
            settings.putString(KEY_INDEX, attempts.value.joinToString(",") { it.id })
        }
    }

    private companion object {
        const val KEY_INDEX = "attempts"

        fun attemptKey(id: String) = "attempt_$id"
    }
}

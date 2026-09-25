package uno.lux.autoskola.data.attempts

import kotlinx.coroutines.flow.Flow
import uno.lux.autoskola.bank.LicenseCategory
import uno.lux.autoskola.stats.TestAttempt

/**
 * Where attempts are stored. Plain storage only: the rules about what may change live in
 * [AttemptRepository]. Shaped like database queries, so a SQLDelight implementation can
 * replace [SettingsAttemptDataSource] (see plans/sqldelight.md).
 */
interface AttemptDataSource {
    /** Attempts of one bank version and license group, in the order they were started. */
    fun observe(bankId: String, bankVersion: Int, category: LicenseCategory): Flow<List<TestAttempt>>

    fun observe(id: String): Flow<TestAttempt?>

    /** Inserts [attempt], or replaces the stored attempt with the same id. */
    suspend fun upsert(attempt: TestAttempt)
}

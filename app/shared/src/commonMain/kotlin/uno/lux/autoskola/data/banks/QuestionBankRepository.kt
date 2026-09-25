package uno.lux.autoskola.data.banks

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uno.lux.autoskola.bank.Question
import uno.lux.autoskola.bank.QuestionBank
import uno.lux.autoskola.bank.QuestionBankInfo

/** The question banks, each read once and then kept in memory. */
class QuestionBankRepository(private val dataSource: QuestionBankDataSource) {
    private val mutex = Mutex()
    private var index: List<QuestionBankInfo>? = null
    private val banks = mutableMapOf<String, QuestionBank?>()

    suspend fun index(): List<QuestionBankInfo> = mutex.withLock {
        index ?: dataSource.readIndex().also { index = it }
    }

    /** The latest version of bank [id], or `null` if there's no such bank. */
    suspend fun latestBank(id: String): QuestionBank? {
        val version = index().firstOrNull { it.id == id }?.version ?: return null
        return bank(id, version)
    }

    /** A specific version, e.g. to show an old attempt; older versions stay bundled. */
    suspend fun bank(id: String, version: Int): QuestionBank? = mutex.withLock {
        val key = "$id-v$version"
        if (key in banks) banks[key] else dataSource.readBank(id, version).also { banks[key] = it }
    }

    /** URI of the image of [question], for the image loader, or `null` for text-only questions. */
    fun imageUri(bank: QuestionBank, question: Question): String? =
        question.image?.let { dataSource.imageUri(bank.imageDir, it) }
}

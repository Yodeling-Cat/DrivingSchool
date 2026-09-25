package uno.lux.autoskola.data.banks

import uno.lux.autoskola.bank.QuestionBank
import uno.lux.autoskola.bank.QuestionBankInfo

/** Where question banks come from. Today they're bundled with the app (see docs/importing-questions.md). */
interface QuestionBankDataSource {
    /** The latest version of every bank. */
    suspend fun readIndex(): List<QuestionBankInfo>

    /** One bank version, or `null` if it doesn't exist. */
    suspend fun readBank(id: String, version: Int): QuestionBank?

    /** URI of an image referenced by a question, for the image loader (Coil). */
    fun imageUri(imageDir: String, path: String): String
}

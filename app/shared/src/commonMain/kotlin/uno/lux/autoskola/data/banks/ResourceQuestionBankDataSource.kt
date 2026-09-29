package uno.lux.autoskola.data.banks

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import najautoskola.app.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import uno.lux.autoskola.bank.QuestionBank
import uno.lux.autoskola.bank.QuestionBankInfo
import kotlin.coroutines.cancellation.CancellationException

/** Reads the banks and images that `tools/import` writes into the app's Compose resources. */
@OptIn(ExperimentalResourceApi::class)
class ResourceQuestionBankDataSource : QuestionBankDataSource {

    override suspend fun readIndex(): List<QuestionBankInfo> =
        decode("files/banks/index.json", QuestionBankInfo::decodeList)

    override suspend fun readBank(id: String, version: Int): QuestionBank? =
        readOrNull { decode("files/banks/$id-v$version.json", QuestionBank::decode) }

    override fun imageUri(imageDir: String, path: String): String = Res.getUri("files/images/$imageDir/$path")

    private suspend fun <T> decode(path: String, parse: (String) -> T): T {
        val json = Res.readBytes(path).decodeToString()
        return withContext(Dispatchers.Default) { parse(json) }
    }
}

/**
 * Runs [read] and answers `null` when it fails, which is how a bank version that isn't bundled (or
 * that can no longer be parsed) shows up. Cancellation is not a failure: it means the caller lost
 * interest, and reporting it as a missing bank would push the user back into onboarding.
 */
internal suspend fun <T> readOrNull(read: suspend () -> T): T? = try {
    read()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    null
}

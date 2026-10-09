package com.spiraclestudios.autoskola.data.banks

import com.spiraclestudios.autoskola.bank.QuestionBank
import com.spiraclestudios.autoskola.bank.QuestionBankInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import najautoskola.app.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.coroutines.cancellation.CancellationException

/** Reads the banks and images that `tools/import` writes into the app's Compose resources. */
class ResourceQuestionBankDataSource : QuestionBankDataSource {

    override suspend fun readIndex(): List<QuestionBankInfo> =
        readResource("files/banks/index.json", QuestionBankInfo::decodeList)

    override suspend fun readBank(id: String, version: Int): QuestionBank? =
        readOrNull { readResource("files/banks/$id-v$version.json", QuestionBank::decode) }

    override fun imageUri(imageDir: String, path: String): String = bundledImageUri(imageDir, path)
}

/**
 * Reads the bundled resource at [path] as text and turns it into a value with [parse], which runs
 * off the main thread: a whole bank is a lot of JSON.
 */
@OptIn(ExperimentalResourceApi::class)
internal suspend fun <T> readResource(path: String, parse: (String) -> T): T {
    val json = Res.readBytes(path).decodeToString()
    return withContext(Dispatchers.Default) { parse(json) }
}

/** The URI of a bundled image: [path] inside the image directory [imageDir] that the importer wrote. */
@OptIn(ExperimentalResourceApi::class)
internal fun bundledImageUri(imageDir: String, path: String): String = Res.getUri("files/images/$imageDir/$path")

/**
 * Runs [read] and answers `null` when it fails, which is how a bank version that isn't bundled (or
 * that can no longer be parsed) shows up. Cancellation is not a failure: it means the caller lost
 * interest, and reporting it as a missing bank would push the user back into onboarding.
 */
internal suspend fun <T> readOrNull(read: suspend () -> T): T? = try {
    read()
} catch (canceled: CancellationException) {
    throw canceled
} catch (_: Exception) {
    null
}

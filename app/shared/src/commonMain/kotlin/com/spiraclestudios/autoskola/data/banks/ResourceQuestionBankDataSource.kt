package com.spiraclestudios.autoskola.data.banks

import com.spiraclestudios.autoskola.bank.QuestionBank
import com.spiraclestudios.autoskola.bank.QuestionBankInfo
import com.spiraclestudios.autoskola.content.BundledContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * Reads the banks and images that `tools/import` writes into the [content] of the country the app
 * is built for.
 */
class ResourceQuestionBankDataSource(private val content: BundledContent) : QuestionBankDataSource {

    override suspend fun readIndex(): List<QuestionBankInfo> =
        content.readJson("files/banks/index.json", QuestionBankInfo::decodeList)

    override suspend fun readBank(id: String, version: Int): QuestionBank? =
        readOrNull { content.readJson("files/banks/$id-v$version.json", QuestionBank::decode) }

    override fun imageUri(imageDir: String, path: String): String = content.imageUri(imageDir, path)
}

/**
 * Reads the bundled file at [path] as text and turns it into a value with [parse], which runs off
 * the main thread: a whole bank is a lot of JSON.
 */
internal suspend fun <T> BundledContent.readJson(path: String, parse: (String) -> T): T {
    val json = readBytes(path).decodeToString()
    return withContext(Dispatchers.Default) { parse(json) }
}

/** The URI of a bundled picture or video: [path] inside the image directory [imageDir] that the importer wrote. */
internal fun BundledContent.imageUri(imageDir: String, path: String): String = uri("files/images/$imageDir/$path")

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

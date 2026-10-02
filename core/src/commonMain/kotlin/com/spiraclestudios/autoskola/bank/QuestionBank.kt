package com.spiraclestudios.autoskola.bank

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * One country's official question bank in one language, as written by `tools/import`.
 *
 * See docs/data-sources.md for where the data comes from.
 */
@Serializable
data class QuestionBank(
    val id: String,
    /** See [QuestionBankInfo.version]. */
    val version: Int,
    /** ISO 3166-1 alpha-2 code of the country whose exam this is. */
    val country: String,
    /** ISO 639-1 code of the language the questions are written in. */
    val language: String,
    /** Directory under `files/images/` holding the images the questions reference. */
    val imageDir: String,
    val sections: List<Section>,
    val questions: List<Question>,
) {
    fun section(id: Int): Section? = sections.firstOrNull { it.id == id }

    companion object {
        fun decode(json: String): QuestionBank = BankJson.decodeFromString(serializer(), json)
    }
}

/** A topic ("okruh") of the exam. Every exam draws [questionsPerTest] questions from it. */
@Serializable
data class Section(
    val id: Int,
    val title: String,
    val questionsPerTest: Int,
    val points: Int,
)

@Serializable
data class Question(
    val id: Int,
    val section: Int,
    val text: String,
    val answers: List<String>,
    /** Index into [answers]. */
    val correct: Int,
    val points: Int,
    /** Path relative to the bank's image directory, or `null` for text-only questions. */
    val image: String? = null,
    val groups: List<LicenseCategory>,
)

/** Entry of `banks/index.json`: what a bank contains without loading the whole bank. */
@Serializable
data class QuestionBankInfo(
    val id: String,
    /**
     * Edition of the official question set. A new version is imported when the authority
     * publishes updated questions; older versions stay bundled so recorded attempts keep
     * pointing at the questions they were taken with. Question ids are only unique within
     * a bank version.
     */
    val version: Int,
    val country: String,
    val language: String,
    val questionCounts: Map<LicenseCategory, Int>,
) {
    companion object {
        fun decodeList(json: String): List<QuestionBankInfo> =
            BankJson.decodeFromString(kotlinx.serialization.builtins.ListSerializer(serializer()), json)
    }
}

private val BankJson = Json { ignoreUnknownKeys = true }

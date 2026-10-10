package com.spiraclestudios.autoskola.bank

import com.spiraclestudios.autoskola.LenientJson
import kotlinx.serialization.Serializable

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
    /** What its exam takes to pass. A bank file that names none is a Slovak one. */
    val passMark: PassMark = PassMark.Slovak,
) {
    fun section(id: Int): Section? = sections.firstOrNull { it.id == id }

    companion object {
        fun decode(json: String): QuestionBank = LenientJson.decodeFromString(serializer(), json)
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
    /**
     * Path relative to the bank's image directory, or `null` for text-only questions. For a
     * question about a [video], the still that stands for it until it plays.
     */
    val image: String? = null,
    val groups: List<LicenseCategory>,
    /** The video the question is about, or `null` when it has none. */
    val video: QuestionVideo? = null,
    /**
     * The pictures the answers are, one per answer and in the same order, when the answers are
     * pictures rather than words ("which of these signs…"). Their text is then empty.
     */
    val answerImages: List<String>? = null,
) {
    /**
     * Whether the exam for [category] can ask this question: the question is in that exam's
     * groups, and it has something to choose between. Every test, drill and count of the exam's
     * pool is drawn from the questions this holds for.
     */
    fun isAskedIn(category: LicenseCategory): Boolean = category in groups && answers.size > 1
}

/** A video a question is about, in the bank's image directory. */
@Serializable
data class QuestionVideo(
    val file: String,
    /**
     * Whether it is a short clip that plays over and over beside the question. One that does not
     * loop is a situation to watch from start to end before answering, so it plays when asked to.
     */
    val loops: Boolean,
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
            LenientJson.decodeFromString(kotlinx.serialization.builtins.ListSerializer(serializer()), json)
    }
}

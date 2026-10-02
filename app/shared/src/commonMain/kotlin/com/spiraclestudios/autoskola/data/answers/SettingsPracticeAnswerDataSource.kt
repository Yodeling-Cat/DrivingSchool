package com.spiraclestudios.autoskola.data.answers

import com.russhwolf.settings.Settings
import com.spiraclestudios.autoskola.answers.PracticeAnswers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * Keeps the answers as one JSON document per bank version in [Settings], read once when the app
 * starts. A document per version rather than per question: there are a few hundred answers at most,
 * they are always wanted together, and a question id only means something inside its own version.
 *
 * A document that says more than this build keeps - one written while the checked marks were kept
 * as well - still reads: what it says about the answers is what the answers are.
 */
class SettingsPracticeAnswerDataSource(private val settings: Settings) : PracticeAnswerDataSource {

    private val stored = MutableStateFlow(read())

    override fun answers(bankId: String, bankVersion: Int): Flow<PracticeAnswers> =
        stored.map { it[answersKey(bankId, bankVersion)] ?: PracticeAnswers() }

    override suspend fun answer(
        bankId: String,
        bankVersion: Int,
        questionId: Int,
        chosen: Int,
    ) {
        val key = answersKey(bankId, bankVersion)
        val updated = (stored.value[key] ?: PracticeAnswers()).withAnswer(questionId, chosen)
        settings.putString(key, AnswersJson.encodeToString(PracticeAnswers.serializer(), updated))
        stored.value = stored.value + (key to updated)
    }

    override suspend fun reset(bankId: String, bankVersion: Int) {
        val key = answersKey(bankId, bankVersion)
        // The document goes rather than being written empty: what was never answered is not stored.
        settings.remove(key)
        stored.value = stored.value - key
    }

    private fun read(): Map<String, PracticeAnswers> =
        settings.keys.filter { it.startsWith(KEY_PREFIX) }.associateWith { key ->
            // A document that can't be read is answers we don't have, not a reason to fail the app.
            settings.getStringOrNull(key)
                ?.let { runCatching { AnswersJson.decodeFromString(PracticeAnswers.serializer(), it) }.getOrNull() }
                ?: PracticeAnswers()
        }

    private companion object {
        const val KEY_PREFIX = "answers_"

        val AnswersJson = Json { ignoreUnknownKeys = true }

        fun answersKey(bankId: String, bankVersion: Int) = "$KEY_PREFIX$bankId-v$bankVersion"
    }
}

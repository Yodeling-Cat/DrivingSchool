package uno.lux.autoskola.data.answers

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import uno.lux.autoskola.answers.PracticeAnswers

/**
 * Keeps the answers as one JSON document per bank version in [Settings], read once when the app
 * starts. A document per version rather than per question: there are a few hundred answers at most,
 * they are always wanted together, and a question id only means something inside its own version.
 *
 * A document written before the checked marks were kept is still read: what it doesn't say is no
 * checked marks yet.
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
    ) = update(bankId, bankVersion) { it.withAnswer(questionId, chosen) }

    override suspend fun reveal(bankId: String, bankVersion: Int, questionId: Int) =
        update(bankId, bankVersion) { it.withReveal(questionId) }

    private fun update(bankId: String, bankVersion: Int, change: (PracticeAnswers) -> PracticeAnswers) {
        val key = answersKey(bankId, bankVersion)
        val updated = change(stored.value[key] ?: PracticeAnswers())
        settings.putString(key, Json.encodeToString(PracticeAnswers.serializer(), updated))
        stored.value = stored.value + (key to updated)
    }

    private fun read(): Map<String, PracticeAnswers> =
        settings.keys.filter { it.startsWith(KEY_PREFIX) }.associateWith { key ->
            // A document that can't be read is answers we don't have, not a reason to fail the app.
            settings.getStringOrNull(key)
                ?.let { runCatching { Json.decodeFromString(PracticeAnswers.serializer(), it) }.getOrNull() }
                ?: PracticeAnswers()
        }

    private companion object {
        const val KEY_PREFIX = "answers_"

        fun answersKey(bankId: String, bankVersion: Int) = "$KEY_PREFIX$bankId-v$bankVersion"
    }
}

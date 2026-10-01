package uno.lux.autoskola.data.answers

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PracticeAnswerRepositoryTest {

    private val repository = PracticeAnswerRepository(SettingsPracticeAnswerDataSource(MapSettings()))

    @Test
    fun aReadFollowsTheAnswersThatAreGiven() = runTest {
        val answers = repository.answers("sk-sk", 1)

        repository.answer("sk-sk", 1, questionId = 1813, chosen = 1)

        assertEquals(1, answers.first().chosenFor(1813))
    }

    @Test
    fun twoAnswersInQuickSuccessionAreBothKept() = runTest {
        // Writes are serialized, so the second answer can't overwrite the first with a stale copy.
        repository.answer("sk-sk", 1, questionId = 1813, chosen = 1)
        repository.answer("sk-sk", 1, questionId = 1814, chosen = 2)

        val answers = repository.answers("sk-sk", 1).first()
        assertEquals(1, answers.chosenFor(1813))
        assertEquals(2, answers.chosenFor(1814))
    }
}

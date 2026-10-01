package uno.lux.autoskola.learn

import kotlinx.coroutines.test.runTest
import uno.lux.autoskola.bank.LicenseCategory
import uno.lux.autoskola.data.banks.testSituationsBank
import uno.lux.autoskola.data.preferences.TestSelection
import uno.lux.autoskola.testing.TestRepositories
import uno.lux.autoskola.testing.ViewModelTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class IntersectionsViewModelTest : ViewModelTest() {

    /** The exam's situations, with the licence group B selected. */
    private fun viewModel(
        repositories: TestRepositories = TestRepositories(testSituationsBank()),
    ): IntersectionsViewModel {
        repositories.preferences.select(TestSelection("sk-sk", LicenseCategory.B))
        return IntersectionsViewModel(repositories.exams, repositories.banks)
    }

    @Test
    fun noExamSelectedLeavesTheScreenLoading() = runTest {
        val fresh = TestRepositories(testSituationsBank())

        val state = observe(IntersectionsViewModel(fresh.exams, fresh.banks).uiState)

        assertEquals(IntersectionsUiState.Loading, state.value)
    }

    @Test
    fun theSituationsAreListedInTheBanksOrderWithTheirDrawings() = runTest {
        val state = observe(viewModel().uiState)

        val ready = assertIs<IntersectionsUiState.Ready>(state.value)
        assertEquals(listOf(1, 2), ready.situations.map { it.number })
        assertEquals(
            listOf("test://minv-sk/obr3/ds/01.png", "test://minv-sk/obr3/ds/02.png"),
            ready.situations.map { it.imageUri },
        )
        assertEquals(listOf(2, 1), ready.situations.map { it.questions.size })
        assertEquals(3, ready.questionCount)
    }

    @Test
    fun aQuestionIsCarriedWithItsAnswersAndTheAnswerThatIsRight() = runTest {
        val state = observe(viewModel().uiState)

        val question = assertIs<IntersectionsUiState.Ready>(state.value).situations[0].questions[0]

        assertEquals("Q1813", question.text)
        assertEquals(listOf("right", "wrong", "wrong"), question.answers)
        assertEquals(0, question.correct)
    }

    @Test
    fun onlyTheSituationsTheLicenceGroupIsAskedAreListed() = runTest {
        // The third drawing is asked of group A alone.
        val state = observe(viewModel().uiState)

        assertEquals(
            listOf("test://minv-sk/obr3/ds/01.png", "test://minv-sk/obr3/ds/02.png"),
            assertIs<IntersectionsUiState.Ready>(state.value).situations.map { it.imageUri },
        )
    }

    @Test
    fun anExamWhoseBankAsksAboutNoSituationsIsReadyAndEmpty() = runTest {
        // The bundled banks all ask about situations; a bank that doesn't still has to answer.
        val repositories = TestRepositories()
        repositories.preferences.select(TestSelection("sk-sk", LicenseCategory.B))

        val state = observe(IntersectionsViewModel(repositories.exams, repositories.banks).uiState)

        val ready = assertIs<IntersectionsUiState.Ready>(state.value)
        assertTrue(ready.situations.isEmpty())
        assertEquals(0, ready.questionCount)
    }
}

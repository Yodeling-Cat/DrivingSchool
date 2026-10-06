package com.spiraclestudios.autoskola.learn

import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.data.banks.testSituationsBank
import com.spiraclestudios.autoskola.data.preferences.TestSelection
import com.spiraclestudios.autoskola.practice.ChipState
import com.spiraclestudios.autoskola.premium.PremiumStatus
import com.spiraclestudios.autoskola.premium.PurchaseSource
import com.spiraclestudios.autoskola.testing.TestRepositories
import com.spiraclestudios.autoskola.testing.ViewModelTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SituationViewModelTest : ViewModelTest() {

    private fun viewModel(
        number: Int,
        repositories: TestRepositories = TestRepositories(testSituationsBank()),
    ): SituationViewModel {
        repositories.preferences.select(TestSelection("sk-sk", LicenseCategory.B))
        return SituationViewModel(
            number,
            repositories.exams,
            repositories.banks,
            repositories.answers,
            repositories.ads,
        )
    }

    @Test
    fun theBannerIsDrawnUntilPremiumIsBought() = runTest {
        val repositories = TestRepositories(testSituationsBank())
        val state = observe(viewModel(1, repositories).uiState)

        // Ads are shown to everyone who has not paid for them to stop, whatever they answered about
        // personalizing them.
        assertTrue(assertIs<SituationUiState.Ready>(state.value).showAds)

        repositories.premium.update(PremiumStatus.Premium(PurchaseSource.Play))

        assertFalse(assertIs<SituationUiState.Ready>(state.value).showAds)
    }

    @Test
    fun anAnsweredQuestionIsMarkedRightOrWrongStraightAway() = runTest {
        val fresh = TestRepositories(testSituationsBank())

        val state = observe(
            SituationViewModel(1, fresh.exams, fresh.banks, fresh.answers, fresh.ads).uiState,
        )

        assertEquals(SituationUiState.Loading, state.value)
    }

    @Test
    fun theSituationTheListNumberedIsTheOneItOpensOn() = runTest {
        val state = observe(viewModel(number = 2).uiState)

        val ready = assertIs<SituationUiState.Ready>(state.value)
        assertEquals(1, ready.initialPage)
        assertEquals(2, ready.situations[ready.initialPage].number)
        assertEquals("test://minv-sk/obr3/ds/02.png", ready.situations[1].imageUri)
        assertEquals(listOf("Q1815"), ready.situations[1].questions.map { it.text })
    }

    @Test
    fun everySituationIsCarriedSoTheScreenCanMoveBetweenThem() = runTest {
        // Which one is being read is the screen's own business; what the situations are is not, so
        // the state holds them all and the screen pages through them without asking the ViewModel.
        val state = observe(viewModel(number = 1).uiState)

        val ready = assertIs<SituationUiState.Ready>(state.value)
        assertEquals(0, ready.initialPage)
        assertEquals(listOf(1, 2), ready.situations.map { it.number })
        assertEquals(
            listOf(listOf("Q1813", "Q1814"), listOf("Q1815")),
            ready.situations.map { situation -> situation.questions.map { it.text } },
        )
    }

    @Test
    fun theFirstSituationCarriesEveryQuestionAskedAboutIt() = runTest {
        val state = observe(viewModel(number = 1).uiState)

        assertEquals(
            listOf("Q1813", "Q1814"),
            assertIs<SituationUiState.Ready>(state.value).situations[0].questions.map { it.text },
        )
    }

    @Test
    fun aSituationTheMaterialDoesNotHaveClosesTheScreen() = runTest {
        // A screen restored from a route the bank no longer answers shows nothing rather than
        // another situation's drawing.
        val state = observe(viewModel(number = 9).uiState)

        assertEquals(SituationUiState.Closed, state.value)
    }

    @Test
    fun nothingIsAnsweredUntilTheDriverAnswers() = runTest {
        val state = observe(viewModel(number = 1).uiState)

        assertTrue(assertIs<SituationUiState.Ready>(state.value).answers.isEmpty())
    }

    @Test
    fun answeringIsShownInTheState() = runTest {
        val viewModel = viewModel(number = 1)
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SituationUiEvent.Answer(questionId = 1813, chosen = 1))

        assertEquals(mapOf(1813 to 1), assertIs<SituationUiState.Ready>(state.value).answers)
    }

    @Test
    fun answeringAgainReplacesTheAnswer() = runTest {
        // Practice, not a test: an answer is never locked, so the driver can change it as often as
        // they like - and the screen follows what they last picked.
        val viewModel = viewModel(number = 1)
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SituationUiEvent.Answer(questionId = 1813, chosen = 1))
        viewModel.onEvent(SituationUiEvent.Answer(questionId = 1813, chosen = 0))

        assertEquals(mapOf(1813 to 0), assertIs<SituationUiState.Ready>(state.value).answers)
    }

    @Test
    fun whatTheDriverAnsweredIsThereTheNextTimeTheSituationIsOpened() = runTest {
        val repositories = TestRepositories(testSituationsBank())
        val first = viewModel(number = 1, repositories = repositories)
        observe(first.uiState)
        first.onEvent(SituationUiEvent.Answer(questionId = 1814, chosen = 2))

        val opened = observe(viewModel(number = 1, repositories = repositories).uiState)

        assertEquals(2, assertIs<SituationUiState.Ready>(opened.value).answers[1814])
    }

    @Test
    fun aQuestionStandsAsItsTabMarksIt() {
        // The tabs say how each question of a situation is doing, in the state the question list
        // gives a question number: right, wrong, and nothing at all while it hasn't been answered.
        val questions = listOf(
            question(id = 1813, correct = 0),
            question(id = 1814, correct = 1),
            question(id = 1815, correct = 2),
        )

        // 1813 answered right, 1814 answered wrong (it wants 1), 1815 not answered at all.
        assertEquals(
            listOf(ChipState.Correct, ChipState.Incorrect, ChipState.Unanswered),
            questionStates(questions, mapOf(1813 to 0, 1814 to 0)),
        )
        assertEquals(
            listOf(ChipState.Unanswered, ChipState.Unanswered, ChipState.Unanswered),
            questionStates(questions, emptyMap()),
        )
    }

    private fun question(id: Int, correct: Int) = SituationQuestion(
        questionId = id,
        text = "Q$id",
        answers = listOf("a", "b", "c"),
        correct = correct,
    )
}

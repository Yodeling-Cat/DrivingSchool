package com.spiraclestudios.autoskola.data.answers

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsPracticeAnswerDataSourceTest {

    private val settings = MapSettings()

    private fun source() = SettingsPracticeAnswerDataSource(settings)

    @Test
    fun anAnswerIsReadBackAfterItIsGiven() = runTest {
        val source = source()

        source.answer("sk-sk", 1, questionId = 1813, chosen = 2)

        assertEquals(2, source.answers("sk-sk", 1).first().chosenFor(1813))
    }

    @Test
    fun theAnswersAreStillThereTheNextTimeTheAppIsOpened() = runTest {
        // The point of storing them: a driver who comes back to a situation finds what they answered.
        source().answer("sk-sk", 1, questionId = 1813, chosen = 2)

        assertEquals(2, source().answers("sk-sk", 1).first().chosenFor(1813))
    }

    @Test
    fun answeringAgainReplacesTheAnswerThatWasThere() = runTest {
        val source = source()
        source.answer("sk-sk", 1, questionId = 1813, chosen = 2)

        source.answer("sk-sk", 1, questionId = 1813, chosen = 0)

        assertEquals(0, source.answers("sk-sk", 1).first().chosenFor(1813))
    }

    @Test
    fun theAnswersOfOneBankVersionAreNotTheOnesOfAnother() = runTest {
        val source = source()
        source.answer("sk-sk", 1, questionId = 1813, chosen = 2)

        // Question ids only mean something inside their own version, so a new one starts fresh.
        assertNull(source.answers("sk-sk", 2).first().chosenFor(1813))
        assertNull(source.answers("sk-hu", 1).first().chosenFor(1813))
    }

    @Test
    fun whatCannotBeReadIsNoAnswersRatherThanAFailure() = runTest {
        settings.putString("answers_sk-sk-v1", "not answers at all")

        assertNull(source().answers("sk-sk", 1).first().chosenFor(1813))
    }

    @Test
    fun answersStoredWithMoreThanTheAnswersStillRead() = runTest {
        // A document from a build that kept which answers had been checked as well: the answers in
        // it are the answers, and the rest is something this build no longer has an opinion about.
        settings.putString("answers_sk-sk-v1", """{"byQuestion":{"1813":2},"revealed":[1813]}""")

        assertEquals(2, source().answers("sk-sk", 1).first().chosenFor(1813))
    }
}

package com.spiraclestudios.autoskola.home

import com.spiraclestudios.autoskola.TestResult
import kotlin.test.Test
import kotlin.test.assertEquals

class TrainingStageTest {
    private fun result(passed: Boolean) = TestResult(score = if (passed) 48 else 30, maxScore = 50)

    @Test
    fun someoneWithNoTestsBehindThemIsStillInTraining() {
        assertEquals(TrainingStage.InTraining, trainingStage(results = emptyMap(), testCount = 42))
    }

    @Test
    fun passingSomeTestsIsNotEnough() {
        val results = mapOf(1 to result(passed = true), 2 to result(passed = true))
        assertEquals(TrainingStage.InTraining, trainingStage(results, testCount = 42))
    }

    @Test
    fun oneFailedTestKeepsThemInTraining() {
        val results = (1..42).associateWith { result(passed = it != 7) }
        assertEquals(TrainingStage.InTraining, trainingStage(results, testCount = 42))
    }

    @Test
    fun passingEveryTestMakesAnExpert() {
        val results = (1..42).associateWith { result(passed = true) }
        assertEquals(TrainingStage.Expert, trainingStage(results, testCount = 42))
    }

    @Test
    fun aBankWithNoTestsCannotBePassed() {
        assertEquals(TrainingStage.InTraining, trainingStage(results = emptyMap(), testCount = 0))
    }
}

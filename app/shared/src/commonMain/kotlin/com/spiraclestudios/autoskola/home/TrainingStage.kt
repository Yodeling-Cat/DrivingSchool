package com.spiraclestudios.autoskola.home

import com.spiraclestudios.autoskola.TestResult

/**
 * How far along the person preparing for a license is. The progress card names the stage, so the
 * screen only has to look it up.
 */
enum class TrainingStage { InTraining, Expert }

/**
 * The stage a license is at, from its finished tests: passing every test of the license makes an
 * expert. A test that has not been taken is not a passed one, so an incomplete set stays in training.
 */
fun trainingStage(results: Map<Int, TestResult>, testCount: Int): TrainingStage =
    if (testCount > 0 && results.size == testCount && results.values.all { it.passed }) {
        TrainingStage.Expert
    } else {
        TrainingStage.InTraining
    }

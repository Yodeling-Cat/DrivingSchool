package com.spiraclestudios.autoskola

import com.spiraclestudios.autoskola.bank.PassMark
import kotlinx.serialization.Serializable

/**
 * Outcome of a single practice-test attempt, graded against the [passMark] of the bank it was taken
 * from. A result saved before results kept their pass mark reads as Slovak, which it was.
 */
@Serializable
data class TestResult(val score: Int, val maxScore: Int, val passMark: PassMark = PassMark.Slovak) {
    val passed: Boolean get() = passMark.isMetBy(score, maxScore)

    val percent: Int get() = if (maxScore == 0) 0 else score * 100 / maxScore
}

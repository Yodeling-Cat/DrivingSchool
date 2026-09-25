package uno.lux.autoskola

import kotlinx.serialization.Serializable

/** Outcome of a single practice-test attempt. */
@Serializable
data class TestResult(val score: Int, val maxScore: Int) {
    /** The official exam requires at least 90 of 100 points. */
    val passed: Boolean get() = maxScore > 0 && score * 100 >= maxScore * PASS_PERCENT

    val percent: Int get() = if (maxScore == 0) 0 else score * 100 / maxScore

    companion object {
        const val PASS_PERCENT = 90
    }
}

package com.spiraclestudios.autoskola.bank

import kotlinx.serialization.Serializable

/**
 * What an exam takes to pass: at least [points] of every [outOf] points it is worth. Slovakia's is
 * 90 of 100, Czechia's 43 of 50. A test worth fewer points, such as a retest of mistakes, passes at
 * the same share.
 */
@Serializable
data class PassMark(val points: Int, val outOf: Int) {
    fun isMetBy(score: Int, maxScore: Int): Boolean = maxScore > 0 && score * outOf >= maxScore * points

    /** The pass mark as the whole percent a score has to reach: 86 for 43 of 50. */
    val percent: Int get() = (points * 100 + outOf - 1) / outOf

    companion object {
        /**
         * The Slovak exam's 90 of 100. It is also what a bank or a result that names no pass mark
         * was graded against: before Czechia, every bank was Slovak.
         */
        val Slovak = PassMark(points = 90, outOf = 100)
    }
}

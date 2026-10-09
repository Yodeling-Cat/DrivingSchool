package com.spiraclestudios.autoskola.situations

import com.spiraclestudios.autoskola.answers.PracticeAnswers
import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.bank.Question
import com.spiraclestudios.autoskola.bank.QuestionBank

/** One situation at an intersection: the bank's drawing of it, and the questions asked about it. */
data class Situation(
    /** The drawing, a path relative to the bank's image directory. */
    val image: String,
    val questions: List<Question>,
)

/**
 * The topic of the exam that asks about situations at intersections. Every bank numbers it 4, and
 * its questions are what this feature is: the topic, not the folder of drawings, because two of its
 * questions carry a drawing the bank keeps elsewhere.
 */
const val SITUATIONS_SECTION = 4

/**
 * [bank]'s situations for [category], in the order the bank lists them, with the questions that
 * share a drawing kept together as one situation.
 *
 * A question the driver can't be asked - the exam's pool is what has something to choose between -
 * and a question without a drawing of its own are not part of one.
 */
fun situations(bank: QuestionBank, category: LicenseCategory): List<Situation> {
    val asked = bank.questions.filter {
        it.section == SITUATIONS_SECTION && it.isAskedIn(category) && it.image != null
    }
    return asked.groupBy { it.image!! }.map { (image, questions) -> Situation(image, questions) }
}

/**
 * Whether the driver has answered every question about this situation, and answered each of them
 * correctly. That is the state worth marking: a situation worked out in full is one there is
 * nothing left to practice in.
 */
fun Situation.isSolvedBy(answers: PracticeAnswers): Boolean =
    questions.isNotEmpty() && questions.all { answers.chosenFor(it.id) == it.correct }

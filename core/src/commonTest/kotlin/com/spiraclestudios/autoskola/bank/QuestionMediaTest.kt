package com.spiraclestudios.autoskola.bank

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QuestionMediaTest {

    @Test
    fun aQuestionReadsTheVideoItIsAbout() {
        val question = decode(""","image":"still.jpg","video":{"file":"clip.mp4","loops":false}""")

        assertEquals(QuestionVideo(file = "clip.mp4", loops = false), question.video)
        assertEquals("still.jpg", question.image)
    }

    @Test
    fun aQuestionReadsThePicturesItsAnswersAre() {
        val question = decode(""","answerImages":["a.jpg","b.jpg","c.jpg"]""")

        assertEquals(listOf("a.jpg", "b.jpg", "c.jpg"), question.answerImages)
    }

    @Test
    fun aQuestionWithNeitherHasNone() {
        val question = decode("")

        assertNull(question.video)
        assertNull(question.answerImages)
    }

    private fun decode(extra: String): Question = QuestionBank.decode(
        """{"id":"cz-cs","version":1,"country":"CZ","language":"cs","imageDir":"md-cz","sections":[],""" +
            """"questions":[{"id":1,"section":1,"text":"Q","answers":["","",""],"correct":0,"points":1,""" +
            """"groups":["B"]$extra}]}""",
    ).questions.single()
}

package com.spiraclestudios.autoskola.practice

import kotlin.test.Test
import kotlin.test.assertEquals

class BackActionTest {

    @Test
    fun backClosesTheOpenQuestionListFirst() {
        assertEquals(
            BackAction.CloseQuestionList,
            backAction(questionListExpanded = true, finished = false),
        )
        assertEquals(
            BackAction.CloseQuestionList,
            backAction(questionListExpanded = true, finished = true),
        )
    }

    @Test
    fun backLeavesAFinishedTestWithoutAsking() {
        assertEquals(BackAction.Leave, backAction(questionListExpanded = false, finished = true))
    }

    @Test
    fun backAsksBeforeLeavingAnUnfinishedTest() {
        assertEquals(BackAction.ConfirmLeave, backAction(questionListExpanded = false, finished = false))
    }
}

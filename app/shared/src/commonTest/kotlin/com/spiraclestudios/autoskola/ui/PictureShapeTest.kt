package com.spiraclestudios.autoskola.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PictureShapeTest {
    @Test
    fun aTrafficSituationDrawingIsOneThatRunsToItsOwnEdges() {
        assertTrue(isSituationDrawing("test://minv-sk/obr3/ds/01.png"))
        assertTrue(
            isSituationDrawing(
                "file:///android_asset/composeResources/najautoskola.app.shared/files/images/minv-sk/obr3/ds/12.png",
            ),
        )
    }

    @Test
    fun aSignAPictogramAndEverythingElseKeepsItsRoundedCorners() {
        assertFalse(isSituationDrawing("test://minv-sk/obr3/dz/110.jpg"))
        assertFalse(isSituationDrawing("test://minv-sk/obr3/tvv/01.png"))
        assertFalse(isSituationDrawing("test://minv-sk/obr3/pcp/15_2a.png"))
        assertFalse(isSituationDrawing("test://minv-sk/obr3/upp_or/27_30.jpg"))
    }

    @Test
    fun aQuestionWithoutAPictureHasNothingToDraw() {
        assertFalse(isSituationDrawing(null))
    }

    @Test
    fun theFolderIsFoundWhicheverWayThePathIsWritten() {
        assertTrue(isSituationDrawing("files\\images\\minv-sk\\obr3\\ds\\01.png"))
    }

    @Test
    fun anotherBanksFolderWithTheSameNameIsNotOne() {
        assertFalse(isSituationDrawing("test://minv-sk/obr3/dsx/01.png"))
    }

    @Test
    fun aDrawingIsCutSquareAndEveryOtherPictureIsRounded() {
        assertEquals(RectangleShape, pictureShape("test://minv-sk/obr3/ds/01.png"))
        assertEquals(RoundedCornerShape(ImageCornerRadius), pictureShape("test://minv-sk/obr3/dz/110.jpg"))
    }
}

package com.spiraclestudios.autoskola.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class EnlargeableImageTest {

    @Test
    fun aPictureIsDrawnHalfAgainAsBigAsItIs() {
        assertSize(DpSize(300.dp, 294.dp), drawn(DpSize(200.dp, 196.dp)))
    }

    @Test
    fun aPictureIsNeverDrawnWiderThanTheRoomItHas() {
        // A photo across the page: half again as wide would overflow, so it fills the column.
        assertSize(DpSize(720.dp, 450.dp), drawn(DpSize(800.dp, 500.dp), maxHeight = 600.dp))
    }

    @Test
    fun aTallPictureIsCappedByTheHeightOfTheRoom() {
        assertSize(DpSize(150.dp, 300.dp), drawn(DpSize(200.dp, 400.dp), maxHeight = 300.dp))
    }

    @Test
    fun aSmallSignIsNotBlownUpPastHalfAgainItsSize() {
        assertSize(DpSize(60.dp, 60.dp), drawn(DpSize(40.dp, 40.dp)))
    }

    @Test
    fun aPictureWeKnowNoSizeOfIsNotDrawn() {
        assertSize(DpSize.Zero, scaledImageSize(DpSize.Unspecified, maxWidth = 720.dp, maxHeight = 390.dp))
        assertSize(DpSize.Zero, scaledImageSize(DpSize.Zero, maxWidth = 720.dp, maxHeight = 390.dp))
    }

    @Test
    fun aSignInTheListsNarrowColumnFitsThatColumn() {
        // A bank sign is 200 x 176 px, which is 76 x 67dp on a phone at density 2.6. Half again as
        // wide is 114dp, which the 72dp column of the road-sign list hasn't room for, so the
        // picture is shrunk until it fits the column's height.
        assertSize(DpSize(58.99.dp, 52.dp), drawn(DpSize(76.dp, 67.dp), maxWidth = 72.dp, maxHeight = 52.dp))
    }

    @Test
    fun theEnlargedCopyFillsTheScreen() {
        // A sign of the list, in the room a phone leaves once the copy's own margin is taken off:
        // the width is what limits it, and the height follows the picture's aspect ratio.
        assertSize(
            DpSize(318.dp, 280.34.dp),
            enlargedImageSize(DpSize(76.dp, 67.dp), viewport = DpSize(318.dp, 768.dp)),
        )
    }

    @Test
    fun theEnlargedCopyIsBiggerThanThePictureInTheList() {
        val intrinsic = DpSize(76.dp, 67.dp)
        val listed = scaledImageSize(intrinsic, maxWidth = 72.dp, maxHeight = 52.dp)

        val copy = enlargedImageSize(intrinsic, viewport = DpSize(318.dp, 768.dp))

        assert(copy.width > listed.width && copy.height > listed.height) { "$copy against $listed" }
    }

    @Test
    fun theEnlargedCopyIsNeverBiggerThanTheScreen() {
        // A picture bigger than the screen it's shown on: the height is what limits it, and the
        // width follows the picture's aspect ratio.
        assertSize(
            DpSize(400.dp, 300.dp),
            enlargedImageSize(DpSize(1600.dp, 1200.dp), viewport = DpSize(400.dp, 300.dp)),
        )
    }

    @Test
    fun anEnlargedCopyOfAPictureWithNoKnownSizeFillsTheScreen() {
        assertSize(
            DpSize(400.dp, 300.dp),
            enlargedImageSize(DpSize.Zero, viewport = DpSize(400.dp, 300.dp)),
        )
    }

    /** The size [scaledImageSize] draws [intrinsic] at on a desktop-sized page. */
    private fun drawn(intrinsic: DpSize, maxWidth: Dp = 720.dp, maxHeight: Dp = 390.dp) =
        scaledImageSize(intrinsic, maxWidth = maxWidth, maxHeight = maxHeight)

    /** Sizes are scaled, so they're compared with the rounding that scaling brings with it. */
    private fun assertSize(expected: DpSize, actual: DpSize, message: String? = null) {
        assertEquals(expected.width.value, actual.width.value, 0.01f, message)
        assertEquals(expected.height.value, actual.height.value, 0.01f, message)
    }
}

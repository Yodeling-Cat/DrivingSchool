package uno.lux.autoskola.practice

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class QuestionImageSizeTest {

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
        assertSize(DpSize.Zero, questionImageSize(DpSize.Unspecified, maxWidth = 720.dp, maxHeight = 390.dp))
        assertSize(DpSize.Zero, questionImageSize(DpSize.Zero, maxWidth = 720.dp, maxHeight = 390.dp))
    }

    @Test
    fun theEnlargedCopyIsUpToTwiceTheSizeOnScreen() {
        assertSize(
            DpSize(600.dp, 588.dp),
            enlargedImageSize(DpSize(300.dp, 294.dp), viewport = DpSize(900.dp, 600.dp)),
        )
    }

    @Test
    fun theEnlargedCopyIsNeverBiggerThanTheScreen() {
        // Twice 300x294 is 600 tall, which doesn't fit a 300-tall screen: the height is what
        // limits the copy, and the width follows the picture's aspect ratio.
        assertSize(
            DpSize(306.12.dp, 300.dp),
            enlargedImageSize(DpSize(300.dp, 294.dp), viewport = DpSize(400.dp, 300.dp)),
        )
    }

    @Test
    fun anEnlargedCopyOfAPictureWithNoKnownSizeFillsTheScreen() {
        assertSize(
            DpSize(400.dp, 300.dp),
            enlargedImageSize(DpSize.Zero, viewport = DpSize(400.dp, 300.dp)),
        )
    }

    /** The size [questionImageSize] draws [intrinsic] at on a desktop-sized page. */
    private fun drawn(intrinsic: DpSize, maxWidth: Dp = 720.dp, maxHeight: Dp = 390.dp) =
        questionImageSize(intrinsic, maxWidth = maxWidth, maxHeight = maxHeight)

    /** Sizes are scaled, so they're compared with the rounding that scaling brings with it. */
    private fun assertSize(expected: DpSize, actual: DpSize, message: String? = null) {
        assertEquals(expected.width.value, actual.width.value, 0.01f, message)
        assertEquals(expected.height.value, actual.height.value, 0.01f, message)
    }
}

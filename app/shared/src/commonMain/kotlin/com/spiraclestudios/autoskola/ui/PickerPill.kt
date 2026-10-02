package com.spiraclestudios.autoskola.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiraclestudios.autoskola.ui.theme.Naj

/** The height the white picker pills are drawn at, wherever they appear. */
val PickerPillHeight = 36.dp

/** The 13sp bold caption the design sets these pills in - wider than the 12sp status caption. */
private const val PILL_CAPTION_SIZE = 13

/**
 * The white pill that names the choice it stands for and opens the list of them: the exam in the
 * home app bar and the language in settings are the same control, so they can't drift apart.
 */
@Composable
fun PickerPill(
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = PickerPillHeight,
) {
    val colors = Naj.colors
    Box(modifier.clip(RoundedCornerShape(percent = 50)).clickable(onClick = onClick)) {
        WhitePill(height = height) {
            // The label gives way in the middle when the room it is given can't hold it: the exam's
            // category is at the front and its language at the back, so both ends are worth keeping.
            FittingText(
                text = text,
                style = Naj.type.tileStatus.copy(fontSize = PILL_CAPTION_SIZE.sp),
                color = colors.ink,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = contentDescription,
                tint = colors.ink,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ── Previews ──

@Preview(widthDp = 320, heightDp = 220)
@Composable
private fun PickerPillPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("AT THE USUAL HEIGHT, AND TALLER")
            PreviewRow {
                PickerPill(text = "Auto · SK", contentDescription = "Change exam", onClick = {})
                PickerPill(text = "Slovenčina", contentDescription = "Language", onClick = {}, height = 44.dp)
                PickerPill(text = "Nákladiak · SK", contentDescription = "Change exam", onClick = {})
            }
            PreviewLabel("A LABEL LONGER THAN THE ROOM IT IS GIVEN")
            PickerPill(
                text = "Tehergépkocsi pótkocsival (C+E) · HU",
                contentDescription = "Change exam",
                onClick = {},
            )
        }
    }
}

package com.spiraclestudios.autoskola.practice

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.spiraclestudios.autoskola.ui.AppPreview
import com.spiraclestudios.autoskola.ui.PreviewBoard
import com.spiraclestudios.autoskola.ui.PreviewLabel
import com.spiraclestudios.autoskola.ui.PreviewRow
import com.spiraclestudios.autoskola.ui.theme.Naj

internal val IconArrowBack: ImageVector = ImageVector.Builder(
    name = "ArrowBack",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(20f, 11f)
        horizontalLineToRelative(-12.17f)
        lineToRelative(5.59f, -5.59f)
        lineTo(12f, 4f)
        lineToRelative(-8f, 8f)
        lineToRelative(8f, 8f)
        lineToRelative(1.41f, -1.41f)
        lineTo(7.83f, 13f)
        horizontalLineTo(20f)
        verticalLineToRelative(-2f)
        close()
    }
}.build()

internal val IconMenu: ImageVector = ImageVector.Builder(
    name = "Menu",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(3f, 18f)
        horizontalLineToRelative(18f)
        verticalLineToRelative(-2f)
        horizontalLineTo(3f)
        verticalLineToRelative(2f)
        close()
        moveTo(3f, 13f)
        horizontalLineToRelative(18f)
        verticalLineToRelative(-2f)
        horizontalLineTo(3f)
        verticalLineToRelative(2f)
        close()
        moveTo(3f, 6f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(18f)
        verticalLineTo(6f)
        horizontalLineTo(3f)
        close()
    }
}.build()

internal val IconMoreVert: ImageVector = ImageVector.Builder(
    name = "MoreVert",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 8f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f)
        reflectiveCurveToRelative(-2f, 0.9f, -2f, 2f)
        reflectiveCurveToRelative(0.9f, 2f, 2f, 2f)
        close()
        moveTo(12f, 10f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        reflectiveCurveToRelative(0.9f, 2f, 2f, 2f)
        reflectiveCurveToRelative(2f, -0.9f, 2f, -2f)
        reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f)
        close()
        moveTo(12f, 16f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        reflectiveCurveToRelative(0.9f, 2f, 2f, 2f)
        reflectiveCurveToRelative(2f, -0.9f, 2f, -2f)
        reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f)
        close()
    }
}.build()

internal val IconArrowDropDown: ImageVector = ImageVector.Builder(
    name = "ArrowDropDown",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(7f, 10f)
        lineToRelative(5f, 5f)
        lineToRelative(5f, -5f)
        close()
    }
}.build()

internal val IconClose: ImageVector = ImageVector.Builder(
    name = "Close",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(19f, 6.41f)
        lineTo(17.59f, 5f)
        lineTo(12f, 10.59f)
        lineTo(6.41f, 5f)
        lineTo(5f, 6.41f)
        lineTo(10.59f, 12f)
        lineTo(5f, 17.59f)
        lineTo(6.41f, 19f)
        lineTo(12f, 13.41f)
        lineTo(17.59f, 19f)
        lineTo(19f, 17.59f)
        lineTo(13.41f, 12f)
        close()
    }
}.build()

// ── Previews ──

/** The icons the practice screen draws itself, at the size the bars give them. */
@Preview(widthDp = 360, heightDp = 160)
@Composable
private fun PracticeIconsPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("ARROW BACK, MENU, MORE, DROP DOWN, CLOSE")
            PreviewRow {
                listOf(IconArrowBack, IconMenu, IconMoreVert, IconArrowDropDown, IconClose).forEach { icon ->
                    Icon(icon, contentDescription = null, tint = Naj.colors.ink, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

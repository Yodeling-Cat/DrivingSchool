package com.spiraclestudios.autoskola.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spiraclestudios.autoskola.ui.theme.Naj

/** How wide a gutter has to be for a [PageArrow] to fit in it beside the page. */
val PageArrowSize = 56.dp

/**
 * The round arrow beside a page, for a window with the room to hold one: a step to the next page
 * rather than a scroll. The page's edges don't move when there is no page that way - the arrow
 * stays where it is, faded - because the arrows are drawn over the page, not in its layout.
 */
@Composable
fun PageArrow(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Naj.colors
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(colors.card)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = description,
                tint = colors.ink.copy(alpha = if (enabled) 1f else 0.3f),
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

/**
 * The two [PageArrow]s beside a pager, one in each [gutter] at the sides of the box they are drawn
 * over: back to the page before [currentPage] and on to the one after it, each dead where there is
 * no page that way.
 */
@Composable
fun BoxScope.PagerSideArrows(
    gutter: Dp,
    currentPage: Int,
    pageCount: Int,
    previousDescription: String,
    nextDescription: String,
    goTo: (Int) -> Unit,
) {
    PageArrow(
        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
        description = previousDescription,
        enabled = currentPage > 0,
        onClick = { goTo(currentPage - 1) },
        modifier = Modifier.align(Alignment.CenterStart).width(gutter).fillMaxHeight(),
    )
    PageArrow(
        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        description = nextDescription,
        enabled = currentPage < pageCount - 1,
        onClick = { goTo(currentPage + 1) },
        modifier = Modifier.align(Alignment.CenterEnd).width(gutter).fillMaxHeight(),
    )
}

// ── Previews ──

/** Both arrows, live and at the ends of a list, where one of them is dead. */
@Preview(widthDp = 360, heightDp = 200)
@Composable
private fun PageArrowPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("both ways to go")
            PreviewRow {
                PageArrow(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous", enabled = true, onClick = {})
                PageArrow(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next", enabled = true, onClick = {})
            }
            PreviewLabel("the first page: nothing before it")
            PreviewRow {
                PageArrow(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous", enabled = false, onClick = {})
                PageArrow(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next", enabled = true, onClick = {})
            }
        }
    }
}

/** The arrows beside a pager on its first page, where only the one forward is live. */
@Preview(widthDp = 600, heightDp = 200)
@Composable
private fun PagerSideArrowsPreview() {
    AppPreview {
        Box(Modifier.fillMaxWidth().height(160.dp)) {
            PagerSideArrows(
                gutter = PageArrowSize,
                currentPage = 0,
                pageCount = 3,
                previousDescription = "Previous",
                nextDescription = "Next",
                goTo = {},
            )
        }
    }
}

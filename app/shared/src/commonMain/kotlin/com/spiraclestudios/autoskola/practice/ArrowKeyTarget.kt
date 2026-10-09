package com.spiraclestudios.autoskola.practice

import androidx.compose.foundation.focusable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * The page an arrow key steps to, or null when the key doesn't move between pages or there is no
 * page that way. Left and up go back, right and down go forward, so the keys read the same way as
 * the arrows beside the page: the practice screen's questions and the situations both page with it.
 */
fun arrowKeyTarget(key: Key, currentPage: Int, pageCount: Int): Int? {
    val step = when (key) {
        Key.DirectionLeft, Key.DirectionUp -> -1
        Key.DirectionRight, Key.DirectionDown -> 1
        else -> return null
    }
    return (currentPage + step).takeIf { it in 0 until pageCount }
}

/**
 * Steps through a pager with the arrow keys on a desktop keyboard, the way the arrows beside the
 * page do: [goTo] is asked for the page [arrowKeyTarget] finds from [currentPage]. The page takes
 * the focus through [focus], which the caller requests on the way in so the keys work without a
 * click first; a child that takes focus by being clicked still hands its key events up to here,
 * because they are asked for as preview events. A modified arrow key belongs to the platform.
 */
fun Modifier.arrowKeyPaging(
    focus: FocusRequester,
    currentPage: Int,
    pageCount: Int,
    goTo: (Int) -> Unit,
): Modifier = focusRequester(focus)
    .focusable()
    .onPreviewKeyEvent { event ->
        val plainKeyDown = event.type == KeyEventType.KeyDown &&
            !event.isCtrlPressed &&
            !event.isAltPressed &&
            !event.isMetaPressed
        if (!plainKeyDown) return@onPreviewKeyEvent false
        val target = arrowKeyTarget(event.key, currentPage, pageCount) ?: return@onPreviewKeyEvent false
        goTo(target)
        true
    }

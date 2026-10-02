package com.spiraclestudios.autoskola.practice

import androidx.compose.ui.input.key.Key

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

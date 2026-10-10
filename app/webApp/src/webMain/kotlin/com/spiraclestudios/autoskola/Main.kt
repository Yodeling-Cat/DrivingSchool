package com.spiraclestudios.autoskola

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.spiraclestudios.autoskola.content.bundledContent

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        App(bundledContent)
    }
}

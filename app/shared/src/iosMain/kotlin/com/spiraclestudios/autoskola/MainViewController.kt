package com.spiraclestudios.autoskola

import androidx.compose.ui.window.ComposeUIViewController
import com.spiraclestudios.autoskola.content.bundledContent

// Named like a type because Swift calls it as a constructor-style factory (see ContentView.swift).
@Suppress("ktlint:standard:function-naming")
fun MainViewController() = ComposeUIViewController { App(bundledContent) }

package com.spiraclestudios.autoskola.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.ui.theme.Naj

/** Shown while a screen waits for its first state. */
@Composable
fun LoadingScreen() {
    Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** Shown in place of a screen's content while it waits for what it lists, below the screen's header. */
@Composable
fun ContentLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Naj.colors.primaryDeep)
    }
}

/** Shown in place of a list that has nothing in it, saying why. */
@Composable
fun EmptyMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.TopCenter) {
        Text(text, style = Naj.type.body, color = Naj.colors.muted)
    }
}

// ── Previews ──

@Preview(widthDp = 360, heightDp = 600)
@Composable
private fun LoadingScreenPreview() {
    AppPreview { LoadingScreen() }
}

@Preview(widthDp = 360, heightDp = 260)
@Composable
private fun ContentPlaceholdersPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("CONTENT LOADING")
            ContentLoading(Modifier.height(60.dp))
            PreviewLabel("EMPTY MESSAGE")
            EmptyMessage(strings.signsNothingFound)
        }
    }
}

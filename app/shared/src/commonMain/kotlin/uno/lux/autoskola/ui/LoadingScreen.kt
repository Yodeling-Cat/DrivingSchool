package uno.lux.autoskola.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

/** Shown while a screen waits for its first state. */
@Composable
fun LoadingScreen() {
    Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

// ── Previews ──

@Preview(widthDp = 360, heightDp = 600)
@Composable
private fun LoadingScreenPreview() {
    AppPreview { LoadingScreen() }
}

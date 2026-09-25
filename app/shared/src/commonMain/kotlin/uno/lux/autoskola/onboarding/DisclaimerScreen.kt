package uno.lux.autoskola.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import uno.lux.autoskola.i18n.AppLanguage
import uno.lux.autoskola.i18n.strings
import uno.lux.autoskola.settings.LanguageMenu

private val Accent = Color(0xFF5E35B1)
private val Body = Color(0xFF424242)

@Composable
fun DisclaimerRoute(viewModel: DisclaimerViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DisclaimerScreen(state, viewModel::onEvent)
}

/** First screen after install. Shown until the user confirms it. */
@Composable
fun DisclaimerScreen(state: DisclaimerUiState, onEvent: (DisclaimerUiEvent) -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.White).safeDrawingPadding()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 480.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Outlined.Policy,
                    contentDescription = null,
                    tint = Accent.copy(alpha = 0.6f),
                    modifier = Modifier.size(64.dp),
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    strings.disclaimerTitle,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Paragraph(strings.disclaimerIndependent)
                Spacer(Modifier.height(12.dp))
                Paragraph(strings.disclaimerSource)
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { onEvent(DisclaimerUiEvent.Accept) },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text(strings.disclaimerAccept, fontSize = 16.sp)
                }
            }
        }
        LanguageMenu(
            current = state.language,
            onSelect = { onEvent(DisclaimerUiEvent.SelectLanguage(it)) },
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        )
    }
}

@Composable
private fun Paragraph(text: String) {
    Text(text, color = Body, fontSize = 16.sp, lineHeight = 24.sp, textAlign = TextAlign.Center)
}

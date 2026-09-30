package uno.lux.autoskola.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import uno.lux.autoskola.i18n.strings
import uno.lux.autoskola.settings.LanguageMenu
import uno.lux.autoskola.ui.ContentWidth
import uno.lux.autoskola.ui.PrimaryPill
import uno.lux.autoskola.ui.ScreenHeader
import uno.lux.autoskola.ui.theme.AccentedText
import uno.lux.autoskola.ui.theme.GRAIN_CARD
import uno.lux.autoskola.ui.theme.Naj
import uno.lux.autoskola.ui.theme.accentWord
import uno.lux.autoskola.ui.theme.grain
import uno.lux.autoskola.ui.theme.paperBackground
import uno.lux.autoskola.ui.theme.paperGlows
import uno.lux.autoskola.ui.theme.rememberGrainShader

@Composable
fun DisclaimerRoute(viewModel: DisclaimerViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DisclaimerScreen(state, viewModel::onEvent)
}

/** First screen after install. Shown until the user confirms it. */
@Composable
fun DisclaimerScreen(state: DisclaimerUiState, onEvent: (DisclaimerUiEvent) -> Unit) {
    val colors = Naj.colors
    Box(Modifier.fillMaxSize().paperBackground(paperGlows(colors))) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            ScreenHeader(
                trailing = {
                    LanguageMenu(
                        current = state.language,
                        onSelect = { onEvent(DisclaimerUiEvent.SelectLanguage(it)) },
                    )
                },
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 52.dp),
            )
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = ContentWidth.Narrow)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    PlatformBadge()
                    Spacer(Modifier.height(32.dp))
                    AccentedText(
                        accentWord(strings.disclaimerTitleBefore, strings.disclaimerTitleAccent),
                        style = Naj.type.stepTitle,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Paragraph(strings.disclaimerIndependent)
                    Spacer(Modifier.height(12.dp))
                    Paragraph(strings.disclaimerSource)
                }
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp)) {
                PrimaryPill(
                    text = strings.disclaimerAccept,
                    onClick = { onEvent(DisclaimerUiEvent.Accept) },
                    height = 56.dp,
                    fillWidth = true,
                )
            }
        }
    }
}

/** The seal that sits above the title: the licence gradient, its grain, and the policy mark. */
@Composable
private fun PlatformBadge() {
    val colors = Naj.colors
    val shader = rememberGrainShader()
    Box(
        modifier = Modifier
            .size(96.dp)
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(32.dp),
                ambientColor = colors.ink.copy(alpha = 0.5f),
                spotColor = colors.ink.copy(alpha = 0.5f),
            )
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(colors.licenceGradient))
            .grain(GRAIN_CARD, shader),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.Policy,
            contentDescription = null,
            tint = colors.ink,
            modifier = Modifier.size(48.dp),
        )
    }
}

@Composable
private fun Paragraph(text: String) {
    Text(
        text,
        style = Naj.type.body,
        color = Naj.colors.muted,
        textAlign = TextAlign.Center,
    )
}

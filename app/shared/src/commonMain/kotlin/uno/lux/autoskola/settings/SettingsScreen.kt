package uno.lux.autoskola.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import uno.lux.autoskola.i18n.strings
import uno.lux.autoskola.ui.ContentWidth

private val Background = Color(0xFFF5F5F5)
private val Subdued = Color(0xFF616161)
private val Accent = Color(0xFF5E35B1)
private val Divider = Color(0xFFEEEEEE)

/** Connects [SettingsScreen] to its [SettingsViewModel] and to navigation. */
@Composable
fun SettingsRoute(onChangeExam: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(state, viewModel::onEvent, onChangeExam)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: SettingsUiState, onEvent: (SettingsUiEvent) -> Unit, onChangeExam: () -> Unit) {
    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsTitle, fontWeight = FontWeight.SemiBold) },
                actions = {
                    LanguageMenu(
                        current = state.language,
                        onSelect = { onEvent(SettingsUiEvent.SelectLanguage(it)) },
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .widthIn(max = ContentWidth.Narrow)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                SectionTitle(strings.settingsExam)
                Card {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onChangeExam)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(strings.settingsChangeExam, fontSize = 16.sp)
                            Text(
                                state.exam?.let {
                                    "${it.name} · ${it.title}"
                                }.orEmpty(),
                                color = Subdued,
                                fontSize = 13.sp,
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Subdued)
                    }
                }

                SectionTitle(strings.settingsAnswers)
                Card {
                    SettingsSwitchRow(
                        title = strings.settingsCheckImmediately,
                        description = strings.settingsCheckImmediatelyDescription,
                        checked = state.checkAnswersImmediately,
                        onCheckedChange = { onEvent(SettingsUiEvent.SetCheckAnswersImmediately(it)) },
                    )
                    HorizontalDivider(color = Divider)
                    SettingsSwitchRow(
                        title = strings.settingsAdvanceAfterAnswer,
                        description = strings.settingsAdvanceAfterAnswerDescription,
                        checked = state.advanceAfterAnswer,
                        enabled = state.canAdvanceAfterAnswer,
                        onCheckedChange = { onEvent(SettingsUiEvent.SetAdvanceAfterAnswer(it)) },
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        color = Subdued,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.fillMaxWidth().clip(shape).background(Color.White)) { content() }
}

/**
 * One setting with its switch; the whole row toggles it. [enabled] is false for a setting that
 * another one rules out, which greys the row out and leaves the switch off.
 */
@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                onValueChange = onCheckedChange,
                role = Role.Switch,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = if (enabled) Color.Unspecified else Subdued)
            Spacer(Modifier.height(2.dp))
            Text(description, color = Subdued, fontSize = 13.sp)
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = Accent),
        )
    }
}

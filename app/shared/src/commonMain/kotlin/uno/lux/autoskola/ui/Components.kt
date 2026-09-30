package uno.lux.autoskola.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uno.lux.autoskola.i18n.strings
import uno.lux.autoskola.practice.IconClose
import uno.lux.autoskola.ui.theme.GRAIN_CARD
import uno.lux.autoskola.ui.theme.Naj
import uno.lux.autoskola.ui.theme.NajColors
import uno.lux.autoskola.ui.theme.grain
import uno.lux.autoskola.ui.theme.rememberGrainShader

// ── Shapes and measurements from the design system's "Shape and space" block ──

val CardRadius = 22.dp
val LicenceRadius = 24.dp
val TileRadius = 18.dp
val ChipRadius = 12.dp
val PillHeight = 52.dp
val PrimaryButtonHeight = 56.dp
val PagePadding = 16.dp

/**
 * The white card every list and panel sits on: rounded, borderless, floating on a soft shadow.
 *
 * [tint] paints a gradient behind the content instead of plain white (the row that is selected,
 * the resume card's yellow).
 */
@Composable
fun NajCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(CardRadius),
    tint: List<Color>? = null,
    grain: Float = 0f,
    shadow: Float = 0.35f,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = Naj.colors
    val shader = rememberGrainShader()
    val shadowModifier = Modifier.shadow(
        elevation = 10.dp,
        shape = shape,
        ambientColor = colors.ink.copy(alpha = shadow),
        spotColor = colors.ink.copy(alpha = shadow),
    )
    Column(
        modifier = modifier
            .then(shadowModifier)
            .clip(shape)
            .background(
                tint?.let {
                    Brush.linearGradient(it)
                } ?: Brush.linearGradient(listOf(colors.card, colors.card)),
            )
            .then(if (grain > 0f) Modifier.grain(grain, shader) else Modifier)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
    ) {
        content()
    }
}

/** A row inside a [NajCard]; cards stack rows with a hairline divider, as the design does. */
@Composable
fun CardDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Naj.colors.divider))
}

/** The 44pt round white button used for back, close and picture actions. */
@Composable
fun RoundButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    background: Color? = null,
    iconSize: Dp = 22.dp,
    tint: Color? = null,
) {
    val colors = Naj.colors
    val bg = background ?: colors.card
    Box(
        modifier = modifier
            .size(size)
            .shadow(6.dp, CircleShape, spotColor = colors.ink.copy(alpha = 0.3f))
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = tint ?: colors.ink,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** A transparent icon button, for the ones that live inside a bar. */
@Composable
fun BarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(24.dp))
    }
}

/** The secondary action: a translucent white pill with an ink hairline outline. */
@Composable
fun OutlinePill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = PillHeight,
) {
    val colors = Naj.colors
    val shape = RoundedCornerShape(height / 2)
    Row(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(colors.whiteVeil)
            .border(BorderStroke(1.5.dp, colors.inkAt(if (enabled) 0.3f else 0.12f)), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = colors.ink, modifier = Modifier.size(20.dp))
            Box(Modifier.width(8.dp))
        }
        Text(text, style = Naj.type.button, color = colors.ink)
    }
}

/** The main action: the teal gradient pill. */
@Composable
fun PrimaryPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    /** A mark to draw instead of [icon], for one that a vector can't express (see [DashedCircleIcon]). */
    iconContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    height: Dp = PillHeight,
    fillWidth: Boolean = false,
) {
    val colors = Naj.colors
    val shape = RoundedCornerShape(height / 2)
    Row(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .height(height)
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = colors.primaryDeep.copy(alpha = 0.8f),
                spotColor = colors.primaryDeep.copy(alpha = 0.8f),
            )
            .clip(shape)
            .background(
                if (enabled) {
                    Brush.verticalGradient(colors.primaryGradient)
                } else {
                    Brush.verticalGradient(listOf(colors.notStartedDot, colors.notStartedDot))
                },
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Box(Modifier.width(8.dp))
        }
        if (iconContent != null) {
            iconContent()
            Box(Modifier.width(8.dp))
        }
        Text(text, style = Naj.type.primaryButton, color = Color.White, textAlign = TextAlign.Center)
    }
}

/**
 * The floating pill a screen's bar sits in: the navy navigation bar and the question bar.
 * [content] lays out the items inside it.
 */
@Composable
fun NavyPill(
    modifier: Modifier = Modifier,
    height: Dp = 72.dp,
    arrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = Naj.colors
    val shape = RoundedCornerShape(height / 2)
    Row(
        modifier = modifier
            .height(height)
            .shadow(
                elevation = 16.dp,
                shape = shape,
                ambientColor = colors.ink.copy(alpha = 0.7f),
                spotColor = colors.ink.copy(alpha = 0.7f),
            )
            .clip(shape)
            .background(Brush.verticalGradient(listOf(colors.navyLight, colors.navy)))
            .padding(horizontal = 8.dp),
        horizontalArrangement = arrangement,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** A small white pill that floats over the page: the exam switcher and the question timer. */
@Composable
fun WhitePill(
    modifier: Modifier = Modifier,
    height: Dp = 36.dp,
    tint: Color? = null,
    content: @Composable () -> Unit,
) {
    val colors = Naj.colors
    val shape = RoundedCornerShape(height / 2)
    Row(
        modifier = modifier
            .height(height)
            .shadow(8.dp, shape, spotColor = colors.ink.copy(alpha = 0.25f))
            .clip(shape)
            .background(tint ?: colors.card)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = { content() },
    )
}

/** A rounded square holding an icon or a few letters, with its own tint. */
@Composable
fun IconTile(
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    background: Brush,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.size(size).clip(RoundedCornerShape(14.dp)).background(background),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

/** The letter that starts an answer row. */
@Composable
fun AnswerLetter(letter: String, selected: Boolean, modifier: Modifier = Modifier) {
    val colors = Naj.colors
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(ChipRadius))
            .background(if (selected) colors.primaryDeep else colors.neutralTint),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            letter,
            style = Naj.type.answerLetter,
            color = if (selected) Color.White else colors.neutralOn,
        )
    }
}

/** An 11pt uppercase label with wide letter spacing: section headings and eyebrows. */
@Composable
fun EyebrowLabel(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle? = null,
    color: Color? = null,
) {
    Text(
        text,
        style = style ?: Naj.type.eyebrowTight,
        color = color ?: Naj.colors.inkAt(0.75f),
        modifier = modifier,
    )
}

/** The status of a test, and the tint that goes with it on a tile or a badge. */
enum class TestStatus {
    Passed,
    Failed,
    InProgress,
    NotStarted,
}

/** The four status tints: fill top, fill bottom, text, and the dot beside the label. */
class StatusTint(
    val top: Color,
    val bottom: Color,
    val on: Color,
    val dot: Color,
    val number: Color,
)

fun statusTint(colors: NajColors, status: TestStatus): StatusTint = when (status) {
    TestStatus.Passed -> StatusTint(
        colors.passedTop,
        colors.passedBottom,
        colors.passedOn,
        colors.passedDot,
        colors.passedOn,
    )

    TestStatus.Failed -> StatusTint(
        colors.wrongTop,
        colors.wrongBottom,
        colors.wrongOn,
        colors.wrongDot,
        colors.wrongOn,
    )

    TestStatus.InProgress -> StatusTint(
        colors.skippedTint.copy(alpha = 0.55f),
        colors.skippedTint,
        colors.skippedOn,
        colors.skippedDot,
        colors.skippedOn,
    )

    TestStatus.NotStarted -> StatusTint(
        colors.card,
        colors.notStartedTint,
        colors.notStartedOn,
        colors.notStartedDot,
        colors.ink,
    )
}

/** The 76pt test tile: a number, a status dot and a label, on the status gradient. */
@Composable
fun TestTile(
    number: String,
    status: TestStatus,
    statusLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Naj.colors
    val tint = statusTint(colors, status)
    Box(
        modifier = modifier
            .height(76.dp)
            .clip(RoundedCornerShape(TileRadius))
            .background(Brush.linearGradient(listOf(tint.top, tint.bottom)))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(number, style = Naj.type.tileNumber, color = tint.number)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(tint.dot))
                Text(statusLabel, style = Naj.type.tileStatus, color = tint.on)
            }
        }
    }
}

/** A 40pt chip holding a question number, tinted by how that question stands. */
@Composable
fun QuestionChip(
    number: String,
    background: Color,
    textColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    shadowed: Boolean = false,
) {
    val shape = RoundedCornerShape(ChipRadius)
    Box(
        modifier = modifier
            .size(40.dp)
            .then(
                if (shadowed) {
                    Modifier.shadow(6.dp, shape, spotColor = Naj.colors.ink.copy(alpha = 0.8f))
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(background)
            .then(if (outlined) Modifier.border(1.5.dp, borderColor, shape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(number, style = Naj.type.chipLetter, color = textColor)
    }
}

/** The app's switch: a 52x32 pill on the primary gradient, or the warm grey when off. */
@Composable
fun NajSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    val colors = Naj.colors
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .width(52.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                when {
                    !enabled -> Brush.linearGradient(listOf(colors.trackOff, colors.trackOff))
                    checked -> Brush.verticalGradient(colors.primaryGradient)
                    else -> Brush.linearGradient(listOf(colors.trackOff, colors.trackOff))
                },
            )
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = onCheckedChange,
            )
            .padding(4.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .size(24.dp)
                .shadow(4.dp, CircleShape, spotColor = colors.ink.copy(alpha = 0.3f))
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

/** A section label above a card, e.g. "ANSWERS" in settings. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = Naj.type.capsLabel,
        color = Naj.colors.muted,
        modifier = modifier.defaultMinSize(minHeight = 32.dp).padding(top = 24.dp, bottom = 10.dp),
    )
}

// ── Previews ──

@Preview(widthDp = 400, heightDp = 400)
@Composable
private fun NajCardPreview() {
    AppPreview {
        val colors = Naj.colors
        PreviewBoard {
            PreviewLabel("PLAIN, TINTED, TAPPABLE")
            NajCard(Modifier.fillMaxWidth()) {
                CardRow("White card", "The default everywhere")
                CardDivider()
                CardRow("Grain", "A card that carries its own texture")
            }
            NajCard(
                Modifier.fillMaxWidth(),
                tint = listOf(colors.resumeTop, colors.resumeBottom),
                grain = GRAIN_CARD,
                onClick = {},
            ) {
                CardRow("Tinted", "The yellow resume card")
            }
            NajCard(Modifier.fillMaxWidth(), tint = listOf(colors.mistakesTop, colors.mistakesBottom)) {
                CardRow("Tinted again", "Any two colours make the gradient")
            }
        }
    }
}

/** One line inside a previewed card: a name, and what the variant is for. */
@Composable
private fun CardRow(title: String, subtitle: String) {
    val colors = Naj.colors
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(title, style = Naj.type.rowTitle, color = colors.ink)
        Text(subtitle, style = Naj.type.rowSubtitle, color = colors.muted)
    }
}

@Preview(widthDp = 300, heightDp = 160)
@Composable
private fun RoundButtonPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("ROUND BUTTON")
            PreviewRow {
                RoundButton(IconClose, "Close", onClick = {})
                RoundButton(IconClose, "Close", onClick = {}, size = 36.dp, iconSize = 18.dp)
                RoundButton(IconClose, "On ink", onClick = {}, background = Naj.colors.ink, tint = Color.White)
            }
        }
    }
}

@Preview(widthDp = 300, heightDp = 160)
@Composable
private fun BarIconButtonPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("BAR ICON BUTTON, ENABLED AND NOT")
            NavyPill(Modifier.fillMaxWidth(), height = 56.dp) {
                BarIconButton(IconClose, "Close", onClick = {})
                BarIconButton(Icons.Filled.Shuffle, "Disabled", onClick = {}, enabled = false)
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 300)
@Composable
private fun PillPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("OUTLINE, ENABLED AND DISABLED")
            OutlinePill(strings.randomTest, onClick = {}, icon = Icons.Filled.Shuffle)
            OutlinePill(strings.randomTest, onClick = {}, icon = Icons.Filled.Shuffle, enabled = false)
            PreviewLabel("PRIMARY: ENABLED, DISABLED, FULL WIDTH, SMALL")
            PrimaryPill(strings.notPassed, onClick = {}, iconContent = { DashedCircleIcon(Color.White) })
            PrimaryPill(strings.notPassed, onClick = {}, enabled = false)
            PrimaryPill(strings.continueTest, onClick = {}, fillWidth = true)
            PrimaryPill(strings.finish, onClick = {}, height = 40.dp)
        }
    }
}

@Preview(widthDp = 360, heightDp = 200)
@Composable
private fun PillBarsPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("NAVY PILL")
            NavyPill(Modifier.fillMaxWidth(), height = 72.dp) {
                BarIconButton(IconClose, "Close", onClick = {})
                Text("Bar content", style = Naj.type.barNumber, color = Color.White)
            }
            PreviewLabel("WHITE PILL")
            PreviewRow {
                WhitePill {
                    Icon(Icons.Filled.Timer, null, tint = Naj.colors.primaryDeep, modifier = Modifier.size(16.dp))
                }
                WhitePill(height = 44.dp, tint = Naj.colors.passedBottom) {
                    Text("07:42", style = Naj.type.tileStatus, color = Naj.colors.passedOn)
                }
            }
        }
    }
}

@Preview(widthDp = 300, heightDp = 220)
@Composable
private fun IconTilePreview() {
    AppPreview {
        val colors = Naj.colors
        PreviewBoard {
            PreviewLabel("ICON TILE")
            PreviewRow {
                IconTile(background = Brush.linearGradient(listOf(colors.mistakesTop, colors.mistakesBottom))) {
                    Text("4", style = Naj.type.badgeNumber, color = Color.White)
                }
                IconTile(size = 40.dp, background = Brush.linearGradient(listOf(colors.mintGlow, colors.mintGlow))) {
                    Icon(Icons.Filled.Check, null, tint = colors.primaryDeep, modifier = Modifier.size(22.dp))
                }
            }
            PreviewLabel("ANSWER LETTER")
            PreviewRow {
                AnswerLetter("A", selected = true)
                AnswerLetter("B", selected = false)
            }
        }
    }
}

@Preview(widthDp = 320, heightDp = 140)
@Composable
private fun LabelsPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("EYEBROW LABEL")
            EyebrowLabel("DRIVER IN TRAINING")
            EyebrowLabel("YOUR ANSWER", color = Naj.colors.primaryDeep)
            SectionLabel("ANSWERS")
        }
    }
}

@Preview(widthDp = 400, heightDp = 420)
@Composable
private fun TestTilePreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("PASSED, FAILED, IN PROGRESS, NOT STARTED")
            TestStatus.entries.chunked(2).forEach { row ->
                PreviewRow {
                    row.forEach { status ->
                        TestTile(
                            number = "${TestStatus.entries.indexOf(status) + 1}",
                            status = status,
                            statusLabel = when (status) {
                                TestStatus.Passed -> strings.resultSummary(36, 40, true)
                                TestStatus.Failed -> strings.resultSummary(31, 40, false)
                                TestStatus.InProgress -> strings.inProgress
                                TestStatus.NotStarted -> strings.notStarted
                            },
                            onClick = {},
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 340)
@Composable
private fun QuestionChipPreview() {
    AppPreview {
        val colors = Naj.colors
        PreviewBoard {
            // The five states the question list draws a number in, plus the current one.
            PreviewLabel("CORRECT, INCORRECT, SKIPPED, ANSWERED, UNANSWERED, CURRENT")
            PreviewRow {
                QuestionChip("1", colors.correctTint, colors.correctOn, colors.correctTint, onClick = {})
                QuestionChip("2", colors.wrongBottom, colors.wrongOn, colors.wrongBottom, onClick = {})
                QuestionChip("3", colors.skippedTint, colors.skippedOn, colors.skippedTint, onClick = {})
                QuestionChip("4", colors.selectedTint, colors.ink, colors.selectedTint, onClick = {})
                QuestionChip("5", colors.card, colors.muted, colors.emptyOutline, onClick = {}, outlined = true)
                QuestionChip("6", colors.ink, colors.roadYellow, colors.ink, onClick = {}, shadowed = true)
            }
        }
    }
}

@Preview(widthDp = 320, heightDp = 200)
@Composable
private fun NajSwitchPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("ON, OFF, DISABLED")
            PreviewRow {
                NajSwitch(checked = true, onCheckedChange = {})
                NajSwitch(checked = false, onCheckedChange = {})
                NajSwitch(checked = true, onCheckedChange = {}, enabled = false)
            }
        }
    }
}

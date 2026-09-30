package uno.lux.autoskola.learn

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import uno.lux.autoskola.i18n.strings
import uno.lux.autoskola.practice.IconArrowBack
import uno.lux.autoskola.ui.AppPreview
import uno.lux.autoskola.ui.CardDivider
import uno.lux.autoskola.ui.ContentWidth
import uno.lux.autoskola.ui.EnlargeableImage
import uno.lux.autoskola.ui.PreviewBoard
import uno.lux.autoskola.ui.PreviewLabel
import uno.lux.autoskola.ui.RoundButton
import uno.lux.autoskola.ui.previewSignGroups
import uno.lux.autoskola.ui.theme.Naj
import uno.lux.autoskola.ui.theme.paperBackground

/** The width of the column a sign's picture and number sit in, and how tall the picture may get. */
private val SignImageColumnWidth = 72.dp
private val SignImageMaxHeight = 52.dp

/** Connects [SignsScreen] to its [SignsViewModel] and to navigation. */
@Composable
fun SignsRoute(onBack: () -> Unit, viewModel: SignsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SignsScreen(state, viewModel::onEvent, onBack)
}

/**
 * The road-sign catalogue: every sign with its picture, its name and what it means, searched by
 * name, number or meaning. A picture opens bigger when it's tapped, as a question's does.
 */
@Composable
fun SignsScreen(state: SignsUiState, onEvent: (SignsUiEvent) -> Unit, onBack: () -> Unit) {
    val colors = Naj.colors
    // Measured here, outside the list, so an enlarged picture is never drawn bigger than the
    // screen showing it.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val page = DpSize(maxWidth, maxHeight)
        Box(Modifier.fillMaxSize().paperBackground()) {
            Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = ContentWidth.Reading).fillMaxWidth()) {
                    SignsHeader(state, onEvent, onBack)
                    SignSearchField(
                        query = (state as? SignsUiState.Ready)?.query.orEmpty(),
                        onQueryChange = { onEvent(SignsUiEvent.Search(it)) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    when (state) {
                        SignsUiState.Loading -> SignsLoading(Modifier.weight(1f))

                        is SignsUiState.Ready ->
                            if (state.groups.isEmpty()) {
                                SignListEmpty(Modifier.weight(1f))
                            } else {
                                SignList(state.groups, page, onEvent, Modifier.weight(1f))
                            }
                    }
                }
            }
        }
    }
}

/**
 * Back, what the list holds, and the action that folds every group away or opens them all. The
 * action is one button whose icon follows the list: it offers to fold while nothing is folded, and
 * to open while anything is.
 */
@Composable
private fun SignsHeader(state: SignsUiState, onEvent: (SignsUiEvent) -> Unit, onBack: () -> Unit) {
    val colors = Naj.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RoundButton(icon = IconArrowBack, contentDescription = strings.back, onClick = onBack)
        Column(Modifier.weight(1f)) {
            Text(strings.learnRoadSigns, style = Naj.type.screenTitle, color = colors.ink)
            (state as? SignsUiState.Ready)?.let { ready ->
                Text(
                    text = if (ready.query.isBlank()) {
                        strings.signsCount(ready.total)
                    } else {
                        strings.signsMatches(ready.shown, ready.total)
                    },
                    style = Naj.type.secondary,
                    color = colors.muted,
                )
            }
        }
        // Nothing to fold while a search has left no groups, so the button goes with them.
        val ready = state as? SignsUiState.Ready
        if (ready != null && ready.groups.isNotEmpty()) {
            RoundButton(
                icon = if (ready.allExpanded) Icons.Filled.UnfoldLess else Icons.Filled.UnfoldMore,
                contentDescription = if (ready.allExpanded) strings.signsCollapseAll else strings.signsExpandAll,
                onClick = { onEvent(SignsUiEvent.ToggleAllGroups) },
            )
        }
    }
}

/**
 * The catalogue, one sign a row, in the regulation's own order, under the headings it lists them
 * in. The heading stays on screen while its signs scroll past, so it is always clear which group
 * the signs belong to - including while a search has left signs of several groups in the list. A
 * folded group keeps its heading and drops its signs, and its heading opens it again.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SignList(
    groups: List<SignGroupItem>,
    page: DpSize,
    onEvent: (SignsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 24.dp)) {
        groups.forEachIndexed { index, group ->
            stickyHeader(key = "header-$index") {
                SignGroupHeading(
                    kind = group.kind,
                    group = group.group,
                    folded = group.folded,
                    onToggle = { onEvent(SignsUiEvent.ToggleGroup(group.kind, group.group)) },
                )
            }
            if (!group.folded) {
                items(group.signs, key = { it.number }) { sign ->
                    SignRow(sign, page)
                    CardDivider()
                }
            }
        }
    }
}

/**
 * The heading a group of signs is listed under: the part of the catalogue it belongs to, and the
 * heading inside it, with the chevron that says whether its signs are showing. Tapping it folds the
 * group away or opens it, so a reader can put the catalogue's headings in front of them and open
 * only what they are looking at.
 *
 * It is drawn on the paper rather than over it, so the signs scrolling past stay under the heading
 * instead of showing through it.
 */
@Composable
private fun SignGroupHeading(
    kind: String,
    group: String,
    folded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Naj.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.paper)
            .clickable(onClick = onToggle)
            .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(kind, style = Naj.type.capsLabel, color = colors.muted)
            if (group.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(group, style = Naj.type.sectionHeading, color = colors.ink)
            }
        }
        Icon(
            imageVector = if (folded) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (folded) strings.signsExpandGroup else strings.signsCollapseGroup,
            tint = colors.muted,
            modifier = Modifier.size(24.dp),
        )
    }
    CardDivider()
}

/** One sign: its picture and number, its name, and what the regulation says it means. */
@Composable
private fun SignRow(sign: SignItem, page: DpSize, modifier: Modifier = Modifier) {
    val colors = Naj.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.width(SignImageColumnWidth), horizontalAlignment = Alignment.CenterHorizontally) {
            EnlargeableImage(uri = sign.imageUri, page = page, maxHeight = SignImageMaxHeight)
            Spacer(Modifier.height(4.dp))
            Text(sign.number, style = Naj.type.secondary, color = colors.muted)
        }
        Column(Modifier.weight(1f)) {
            // A few signs are a number plate: the picture is their name, and the number is the text.
            if (sign.name.isNotBlank()) {
                Text(sign.name, style = Naj.type.rowTitle, color = colors.ink)
                Spacer(Modifier.height(3.dp))
            }
            Text(sign.description, style = Naj.type.option, color = colors.muted)
        }
    }
}

@Composable
private fun SignSearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = Naj.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(colors.card)
            .border(BorderStroke(1.5.dp, colors.emptyOutline), shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = colors.muted, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(strings.signsSearchPlaceholder, style = Naj.type.option, color = colors.muted)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = Naj.type.option.copy(color = colors.ink),
                cursorBrush = SolidColor(colors.primaryDeep),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onQueryChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = strings.clearSearch,
                    tint = colors.muted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun SignListEmpty(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.TopCenter) {
        Text(strings.signsNothingFound, style = Naj.type.body, color = Naj.colors.muted)
    }
}

/** Nothing to list until the catalogue has been read. */
@Composable
private fun SignsLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Naj.colors.primaryDeep)
    }
}

// ── Previews ──

/** How many signs the previews claim the catalogue holds. */
private val previewSignTotal = 120

/** The catalogue as it opens: every sign, under the headings the regulation lists them in. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun SignsScreenPreview() {
    AppPreview {
        SignsScreen(
            state = SignsUiState.Ready(groups = previewSignGroups, query = "", total = previewSignTotal),
            onEvent = {},
            onBack = {},
        )
    }
}

/** A search whose results still fall into several groups, so the list keeps its headings. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun SignsScreenSearchPreview() {
    AppPreview {
        SignsScreen(
            state = SignsUiState.Ready(
                groups = listOf(
                    previewSignGroups[0].copy(
                        signs = previewSignGroups[0].signs.filter { it.number == "140" },
                    ),
                    previewSignGroups[2].copy(
                        signs = previewSignGroups[2].signs.filter { it.name.startsWith("Zákaz") },
                    ),
                ),
                query = "zakaz",
                total = previewSignTotal,
            ),
            onEvent = {},
            onBack = {},
        )
    }
}

/** A group the regulation lists under a heading of its own kind, open and folded, and one it does not. */
@Preview(widthDp = 400, heightDp = 480)
@Composable
private fun SignGroupHeadingPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("open, with a heading inside the kind")
            SignGroupHeading(previewSignGroups[2].kind, previewSignGroups[2].group, folded = false, onToggle = {})
            PreviewLabel("folded")
            SignGroupHeading(previewSignGroups[2].kind, previewSignGroups[2].group, folded = true, onToggle = {})
            PreviewLabel("without a heading inside the kind")
            SignGroupHeading(previewSignGroups[3].kind, previewSignGroups[3].group, folded = false, onToggle = {})
        }
    }
}

/** The bar's action while nothing is folded, and while anything is. */
@Preview(widthDp = 400, heightDp = 300)
@Composable
private fun SignsHeaderPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("nothing folded: the action folds")
            SignsHeader(
                state = SignsUiState.Ready(groups = previewSignGroups, query = "", total = previewSignTotal),
                onEvent = {},
                onBack = {},
            )
            PreviewLabel("anything folded: the action opens")
            SignsHeader(
                state = SignsUiState.Ready(
                    groups = previewSignGroups.map { it.copy(folded = true) },
                    query = "",
                    total = previewSignTotal,
                ),
                onEvent = {},
                onBack = {},
            )
            PreviewLabel("no groups: no action")
            SignsHeader(
                state = SignsUiState.Ready(groups = emptyList(), query = "vrtuľník", total = previewSignTotal),
                onEvent = {},
                onBack = {},
            )
        }
    }
}

/** Every group folded: the list is its headings, and one of them opens its own signs. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun SignsScreenFoldedPreview() {
    AppPreview {
        SignsScreen(
            state = SignsUiState.Ready(
                groups = previewSignGroups.map { it.copy(folded = true) },
                query = "",
                total = previewSignTotal,
            ),
            onEvent = {},
            onBack = {},
        )
    }
}

/** One group open among folded ones, so both states of a heading are on screen together. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun SignsScreenOneGroupOpenPreview() {
    AppPreview {
        SignsScreen(
            state = SignsUiState.Ready(
                groups = previewSignGroups.mapIndexed { index, group -> group.copy(folded = index != 2) },
                query = "",
                total = previewSignTotal,
            ),
            onEvent = {},
            onBack = {},
        )
    }
}

/** A search that matched nothing. */
@Preview(widthDp = 400, heightDp = 600)
@Composable
private fun SignsScreenNoMatchPreview() {
    AppPreview {
        SignsScreen(
            state = SignsUiState.Ready(groups = emptyList(), query = "vrtuľník", total = previewSignTotal),
            onEvent = {},
            onBack = {},
        )
    }
}

/** The catalogue still being read. */
@Preview(widthDp = 400, heightDp = 600)
@Composable
private fun SignsScreenLoadingPreview() {
    AppPreview { SignsScreen(state = SignsUiState.Loading, onEvent = {}, onBack = {}) }
}

/** A tablet, where the reading column is capped and centred. */
@Preview(widthDp = 900, heightDp = 700)
@Composable
private fun SignsScreenWidePreview() {
    AppPreview {
        SignsScreen(
            state = SignsUiState.Ready(groups = previewSignGroups, query = "", total = previewSignTotal),
            onEvent = {},
            onBack = {},
        )
    }
}

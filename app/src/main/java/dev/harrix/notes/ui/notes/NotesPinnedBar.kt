package dev.harrix.notes.ui.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.harrix.notes.NotesListDensity
import dev.harrix.notes.NotesPinnedItem
import dev.harrix.notes.NotesPinnedKind
import dev.harrix.notes.R

private const val PinnedLabelMaxLines = 2
private const val PinnedLabelFontStepSp = 0.5f
private val PinnedBarHorizontalPadding = 8.dp

@Composable
fun NotesPinnedBar(
    items: List<NotesPinnedItem>,
    maxSlots: Int,
    density: NotesListDensity,
    onOpen: (NotesPinnedItem) -> Unit,
    onUnpin: (NotesPinnedItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val slots = maxSlots.coerceAtLeast(1)
    val emptyCount = (slots - items.size).coerceAtLeast(0)
    var showHowToPin by remember { mutableStateOf(false) }
    val minItemWidth = density.pinnedItemWidthDp.dp
    val iconSize = density.pinnedIconSizeDp.dp
    val labelMinFont = density.pinnedLabelMinSp.sp
    val labelMaxFont = density.pinnedLabelMaxSp.sp
    val labelHeight = density.pinnedLabelHeightDp.dp
    val barPadding = density.pinnedBarVerticalPaddingDp.dp

    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider()
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val availableWidth = maxWidth - PinnedBarHorizontalPadding * 2
            val needsScroll = minItemWidth * slots > availableWidth
            val scrollState = rememberScrollState()

            Column(modifier = Modifier.fillMaxWidth()) {
                if (needsScroll) {
                    Row(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                            .padding(horizontal = PinnedBarHorizontalPadding, vertical = barPadding),
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        NotesPinnedBarSlots(
                            items = items,
                            emptyCount = emptyCount,
                            slotModifier = { Modifier.width(minItemWidth) },
                            iconSize = iconSize,
                            labelMinFont = labelMinFont,
                            labelMaxFont = labelMaxFont,
                            labelHeight = labelHeight,
                            onOpen = onOpen,
                            onUnpin = onUnpin,
                            onEmptyClick = { showHowToPin = true },
                        )
                    }
                    NotesPinnedHorizontalScrollbar(
                        state = scrollState,
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PinnedBarHorizontalPadding, vertical = 2.dp),
                    )
                } else {
                    Row(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PinnedBarHorizontalPadding, vertical = barPadding),
                        verticalAlignment = Alignment.Top,
                    ) {
                        NotesPinnedBarSlots(
                            items = items,
                            emptyCount = emptyCount,
                            slotModifier = { Modifier.weight(1f) },
                            iconSize = iconSize,
                            labelMinFont = labelMinFont,
                            labelMaxFont = labelMaxFont,
                            labelHeight = labelHeight,
                            onOpen = onOpen,
                            onUnpin = onUnpin,
                            onEmptyClick = { showHowToPin = true },
                        )
                    }
                }
            }
        }
    }

    if (showHowToPin) {
        AlertDialog(
            onDismissRequest = { showHowToPin = false },
            title = { Text(stringResource(R.string.markdown_notes_pin_how_title)) },
            text = { Text(stringResource(R.string.markdown_notes_pin_how_message)) },
            confirmButton = {
                TextButton(onClick = { showHowToPin = false }) {
                    Text(stringResource(R.string.markdown_notes_pin_how_ok))
                }
            },
        )
    }
}

@Composable
private fun RowScope.NotesPinnedBarSlots(
    items: List<NotesPinnedItem>,
    emptyCount: Int,
    slotModifier: RowScope.() -> Modifier,
    iconSize: Dp,
    labelMinFont: TextUnit,
    labelMaxFont: TextUnit,
    labelHeight: Dp,
    onOpen: (NotesPinnedItem) -> Unit,
    onUnpin: (NotesPinnedItem) -> Unit,
    onEmptyClick: () -> Unit,
) {
    items.forEach { item ->
        NotesPinnedBarItem(
            item = item,
            modifier = slotModifier(),
            iconSize = iconSize,
            labelMinFont = labelMinFont,
            labelMaxFont = labelMaxFont,
            labelHeight = labelHeight,
            onOpen = { onOpen(item) },
            onUnpin = { onUnpin(item) },
        )
    }
    repeat(emptyCount) {
        NotesPinnedEmptySlot(
            modifier = slotModifier(),
            iconSize = iconSize,
            labelMinFont = labelMinFont,
            labelMaxFont = labelMaxFont,
            labelHeight = labelHeight,
            onClick = onEmptyClick,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotesPinnedBarItem(
    item: NotesPinnedItem,
    modifier: Modifier,
    iconSize: Dp,
    labelMinFont: TextUnit,
    labelMaxFont: TextUnit,
    labelHeight: Dp,
    onOpen: () -> Unit,
    onUnpin: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val label =
        when {
            item.kind == NotesPinnedKind.Home || item.id == NotesPinnedItem.HOME_ID -> {
                stringResource(R.string.nav_drawer_home)
            }
            else -> item.title.ifBlank { item.documentId }
        }

    Box(modifier = modifier) {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onOpen,
                    onLongClick = { menuExpanded = true },
                ).padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NotesPinnedGlyph(item = item, iconSize = iconSize)
            Spacer(modifier = Modifier.height(4.dp))
            NotesPinnedAutoSizeLabel(
                text = label,
                muted = false,
                minFont = labelMinFont,
                maxFont = labelMaxFont,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = labelHeight * 0.85f, max = labelHeight),
            )
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.markdown_notes_unpin)) },
                onClick = {
                    menuExpanded = false
                    onUnpin()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = null,
                    )
                },
            )
        }
    }
}

@Composable
private fun NotesPinnedEmptySlot(
    modifier: Modifier,
    iconSize: Dp,
    labelMinFont: TextUnit,
    labelMaxFont: TextUnit,
    labelHeight: Dp,
    onClick: () -> Unit,
) {
    Column(
        modifier =
        modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.PushPin,
            contentDescription = stringResource(R.string.markdown_notes_pin_empty),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.size(iconSize),
        )
        Spacer(modifier = Modifier.height(4.dp))
        NotesPinnedAutoSizeLabel(
            text = stringResource(R.string.markdown_notes_pin_empty),
            muted = true,
            minFont = labelMinFont,
            maxFont = labelMaxFont,
            modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = labelHeight * 0.85f, max = labelHeight),
        )
    }
}

@Composable
private fun NotesPinnedGlyph(
    item: NotesPinnedItem,
    iconSize: Dp,
) {
    when (item.kind) {
        NotesPinnedKind.Home -> {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(iconSize),
            )
        }
        NotesPinnedKind.Folder -> {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(iconSize),
            )
        }
        NotesPinnedKind.Note -> {
            NotesNoteGlyph(icon = item.icon, size = iconSize)
        }
    }
}

@Composable
private fun NotesPinnedHorizontalScrollbar(
    state: ScrollState,
    modifier: Modifier = Modifier,
) {
    val maxValue = state.maxValue
    if (maxValue <= 0) {
        return
    }
    val density = LocalDensity.current
    val scrollFraction = (state.value.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)

    BoxWithConstraints(modifier = modifier.height(3.dp)) {
        val trackWidthPx = with(density) { maxWidth.toPx() }
        val contentWidthPx = trackWidthPx + maxValue
        val thumbWidthPx =
            (trackWidthPx * trackWidthPx / contentWidthPx)
                .coerceIn(trackWidthPx * 0.12f, trackWidthPx)
        val thumbOffsetPx = scrollFraction * (trackWidthPx - thumbWidthPx)
        Box(
            modifier =
            Modifier
                .fillMaxHeight()
                .width(with(density) { thumbWidthPx.toDp() })
                .offset(x = with(density) { thumbOffsetPx.toDp() })
                .background(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(2.dp),
                ),
        )
    }
}

@Composable
private fun NotesPinnedAutoSizeLabel(
    text: String,
    muted: Boolean,
    minFont: TextUnit,
    maxFont: TextUnit,
    modifier: Modifier = Modifier,
) {
    val baseStyle = MaterialTheme.typography.labelSmall
    val textMeasurer = rememberTextMeasurer()
    var fontSize by remember(text, maxFont) { mutableStateOf(maxFont) }
    val color =
        if (muted) {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = constraints.maxWidth
        val maxHeightPx = constraints.maxHeight
        LaunchedEffect(text, maxWidthPx, maxHeightPx, baseStyle, minFont, maxFont) {
            if (maxWidthPx <= 0) {
                return@LaunchedEffect
            }
            var candidate = maxFont
            while (candidate > minFont) {
                val layout =
                    textMeasurer.measure(
                        text = text,
                        style = baseStyle.copy(fontSize = candidate, lineHeight = candidate * 1.15f),
                        overflow = TextOverflow.Clip,
                        softWrap = true,
                        maxLines = PinnedLabelMaxLines,
                        constraints =
                        Constraints(
                            maxWidth = maxWidthPx,
                            maxHeight = if (maxHeightPx > 0) maxHeightPx else Constraints.Infinity,
                        ),
                    )
                if (!layout.hasVisualOverflow) {
                    break
                }
                candidate =
                    (candidate.value - PinnedLabelFontStepSp)
                        .coerceAtLeast(minFont.value)
                        .sp
            }
            fontSize = candidate
        }
        Text(
            text = text,
            style = baseStyle.copy(fontSize = fontSize, lineHeight = fontSize * 1.15f),
            color = color,
            textAlign = TextAlign.Center,
            maxLines = PinnedLabelMaxLines,
            overflow = TextOverflow.Ellipsis,
            softWrap = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

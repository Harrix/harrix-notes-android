package dev.harrix.notes.ui.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.rememberTextMeasurer
import dev.harrix.notes.NotesPinnedItem
import dev.harrix.notes.NotesPinnedKind
import dev.harrix.notes.R

private val PinnedItemWidth = 72.dp
private val PinnedIconSize = 28.dp
private val PinnedLabelMinFont = 9.sp
private val PinnedLabelMaxFont = 12.sp
private const val PinnedLabelMaxLines = 2
private const val PinnedLabelFontStepSp = 0.5f

@Composable
fun NotesPinnedBar(
    items: List<NotesPinnedItem>,
    onOpen: (NotesPinnedItem) -> Unit,
    onUnpin: (NotesPinnedItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        return
    }
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider()
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            items.forEach { item ->
                NotesPinnedBarItem(
                    item = item,
                    onOpen = { onOpen(item) },
                    onUnpin = { onUnpin(item) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotesPinnedBarItem(
    item: NotesPinnedItem,
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

    Box(
        modifier = Modifier.width(PinnedItemWidth),
    ) {
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
            NotesPinnedGlyph(item = item)
            Spacer(modifier = Modifier.height(4.dp))
            NotesPinnedAutoSizeLabel(
                text = label,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 28.dp, max = 36.dp),
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
            )
        }
    }
}

@Composable
private fun NotesPinnedGlyph(item: NotesPinnedItem) {
    when (item.kind) {
        NotesPinnedKind.Home -> {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(PinnedIconSize),
            )
        }
        NotesPinnedKind.Folder -> {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(PinnedIconSize),
            )
        }
        NotesPinnedKind.Note -> {
            NotesNoteGlyph(icon = item.icon, size = PinnedIconSize)
        }
    }
}

@Composable
private fun NotesPinnedAutoSizeLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val baseStyle = MaterialTheme.typography.labelSmall
    val textMeasurer = rememberTextMeasurer()
    var fontSize by remember(text) { mutableStateOf(PinnedLabelMaxFont) }

    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = constraints.maxWidth
        val maxHeightPx = constraints.maxHeight
        LaunchedEffect(text, maxWidthPx, maxHeightPx, baseStyle) {
            if (maxWidthPx <= 0) {
                return@LaunchedEffect
            }
            var candidate = PinnedLabelMaxFont
            while (candidate > PinnedLabelMinFont) {
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
                        .coerceAtLeast(PinnedLabelMinFont.value)
                        .sp
            }
            fontSize = candidate
        }
        Text(
            text = text,
            style = baseStyle.copy(fontSize = fontSize, lineHeight = fontSize * 1.15f),
            textAlign = TextAlign.Center,
            maxLines = PinnedLabelMaxLines,
            overflow = TextOverflow.Ellipsis,
            softWrap = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

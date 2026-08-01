package dev.harrix.notes.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.harrix.notes.NotesEntry
import dev.harrix.notes.NotesListDensity
import dev.harrix.notes.NotesPathSegment
import dev.harrix.notes.R
import dev.harrix.notes.ui.adaptiveDrawerWidth

private val DrawerLogoSize = 40.dp

data class NotesTreeRow(
    val entry: NotesEntry,
    val depth: Int,
    /** Path from notes root through the parent folder (excludes the entry itself). */
    val parentPath: List<NotesPathSegment>,
)

@Composable
fun NotesTreeDrawerContent(
    rows: List<NotesTreeRow>,
    expandedFolderIds: Set<String>,
    selectedNoteDocumentId: String?,
    isLoadingRoot: Boolean,
    density: NotesListDensity,
    onToggleFolder: (NotesEntry.Folder) -> Unit,
    onOpenFolder: (NotesEntry.Folder, List<NotesPathSegment>) -> Unit,
    onOpenNote: (NotesEntry.Note, List<NotesPathSegment>) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(modifier = modifier.adaptiveDrawerWidth()) {
        Column(modifier = Modifier.fillMaxHeight()) {
            NotesBrandTitle(
                logoSize = DrawerLogoSize,
                textStyle = MaterialTheme.typography.titleLarge,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 28.dp,
                        vertical = 24.dp,
                    ),
            )
            HorizontalDivider(
                modifier = Modifier.padding(bottom = 8.dp),
            )
            when {
                isLoadingRoot && rows.isEmpty() -> {
                    Column(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }

                rows.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.markdown_notes_folder_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                }

                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = rows,
                            key = { row -> "${row.entry.documentId}-${row.depth}" },
                        ) { row ->
                            when (val entry = row.entry) {
                                is NotesEntry.Folder -> {
                                    NotesTreeFolderRow(
                                        folder = entry,
                                        depth = row.depth,
                                        density = density,
                                        expanded = entry.documentId in expandedFolderIds,
                                        onToggle = { onToggleFolder(entry) },
                                        onOpen = { onOpenFolder(entry, row.parentPath) },
                                    )
                                }

                                is NotesEntry.Note -> {
                                    NotesTreeNoteRow(
                                        note = entry,
                                        depth = row.depth,
                                        density = density,
                                        selected = entry.documentId == selectedNoteDocumentId,
                                        onOpen = { onOpenNote(entry, row.parentPath) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesTreeFolderRow(
    folder: NotesEntry.Folder,
    depth: Int,
    density: NotesListDensity,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    val iconSize = density.iconSizeDp.dp
    val expandButtonSize = density.mergedButtonHeightDp.dp
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(density.listRowHeightDp.dp)
            .clickable(onClick = onOpen)
            .padding(start = (8 + depth * 12).dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector =
            if (expanded) {
                Icons.Filled.ExpandMore
            } else {
                Icons.AutoMirrored.Filled.KeyboardArrowRight
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
            Modifier
                .size(expandButtonSize)
                .clickable(
                    role = Role.Button,
                    onClick = onToggle,
                ).padding((expandButtonSize - iconSize) / 2),
        )
        Icon(
            imageVector = Icons.Filled.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(iconSize),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = folder.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun NotesTreeNoteRow(
    note: NotesEntry.Note,
    depth: Int,
    density: NotesListDensity,
    selected: Boolean,
    onOpen: () -> Unit,
) {
    val iconSize = density.iconSizeDp.dp
    // Align note content with folder label (start padding + expand control).
    val contentStart = 8 + density.mergedButtonHeightDp + depth * 12
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(density.listRowHeightDp.dp)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    Color.Transparent
                },
            ).clickable(onClick = onOpen)
            .padding(start = contentStart.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NotesNoteGlyph(icon = note.displayIcon, size = iconSize)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = note.displayLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Flattens loaded children into visible rows according to [expandedFolderIds]. */
fun buildVisibleNotesTreeRows(
    root: NotesPathSegment,
    childrenByFolderId: Map<String, List<NotesEntry>>,
    expandedFolderIds: Set<String>,
): List<NotesTreeRow> {
    val result = ArrayList<NotesTreeRow>()

    fun walk(
        dir: NotesPathSegment,
        pathToDir: List<NotesPathSegment>,
        depth: Int,
    ) {
        val children = childrenByFolderId[dir.documentId].orEmpty()
        for (entry in children) {
            result +=
                NotesTreeRow(
                    entry = entry,
                    depth = depth,
                    parentPath = pathToDir,
                )
            if (entry is NotesEntry.Folder && entry.documentId in expandedFolderIds) {
                val folderSegment =
                    NotesPathSegment(
                        documentId = entry.documentId,
                        name = entry.name,
                        uri = entry.uri,
                    )
                walk(
                    dir = folderSegment,
                    pathToDir = pathToDir + folderSegment,
                    depth = depth + 1,
                )
            }
        }
    }

    walk(dir = root, pathToDir = listOf(root), depth = 0)
    return result
}

package dev.harrix.notes.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.harrix.notes.NotesDateFormats
import dev.harrix.notes.NotesDocumentInfo
import dev.harrix.notes.OpenNoteTab
import dev.harrix.notes.R

@Composable
fun NotesNoteInfoDialog(
    tab: OpenNoteTab,
    documentInfo: NotesDocumentInfo?,
    onDismiss: () -> Unit,
) {
    val unknown = stringResource(R.string.markdown_notes_note_info_unknown)
    val fileName =
        documentInfo?.displayName?.takeIf { it.isNotBlank() }
            ?: tab.fileName.takeIf { it.isNotBlank() }
            ?: unknown
    val sizeText =
        documentInfo?.sizeBytes?.let { NotesDateFormats.formatByteSize(it) } ?: unknown
    val modifiedText =
        documentInfo?.lastModifiedEpochMs?.let { NotesDateFormats.formatListDateTime(it) }
            ?: unknown
    val mimeText = documentInfo?.mimeType?.takeIf { it.isNotBlank() } ?: unknown
    val locationText =
        when {
            tab.isExternal -> stringResource(R.string.markdown_notes_note_info_external)
            tab.folderPath.isEmpty() -> unknown
            else -> tab.folderPath.joinToString(" / ") { it.name }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.markdown_notes_note_info)) },
        text = {
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NotesInfoRow(
                    label = stringResource(R.string.markdown_notes_note_info_title),
                    value = tab.title.ifBlank { unknown },
                )
                NotesInfoRow(
                    label = stringResource(R.string.markdown_notes_note_info_file_name),
                    value = fileName,
                )
                NotesInfoRow(
                    label = stringResource(R.string.markdown_notes_note_info_size),
                    value = sizeText,
                )
                NotesInfoRow(
                    label = stringResource(R.string.markdown_notes_note_info_modified),
                    value = modifiedText,
                )
                NotesInfoRow(
                    label = stringResource(R.string.markdown_notes_note_info_location),
                    value = locationText,
                )
                NotesInfoRow(
                    label = stringResource(R.string.markdown_notes_note_info_mime),
                    value = mimeText,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.markdown_notes_note_info_ok))
            }
        },
    )
}

@Composable
private fun NotesInfoRow(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

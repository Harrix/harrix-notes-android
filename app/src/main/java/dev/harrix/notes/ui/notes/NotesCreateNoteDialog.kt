package dev.harrix.notes.ui.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.harrix.notes.NewNoteContent
import dev.harrix.notes.NotesTreeRepository
import dev.harrix.notes.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesCreateNoteDialog(
    untitledFileStem: String,
    beginningTemplates: List<NewNoteContent.BeginningTemplate>,
    defaultBeginningTemplateId: String,
    onDismiss: () -> Unit,
    onConfirm: (
        fileStem: String,
        noteTitle: String,
        beginningTemplateId: String,
        isCanvas: Boolean,
    ) -> Unit,
) {
    var noteTitle by remember { mutableStateOf("") }
    var fileStem by remember { mutableStateOf("") }
    var syncFileNameFromTitle by remember { mutableStateOf(true) }
    var isCanvas by remember { mutableStateOf(false) }
    val templates =
        beginningTemplates.ifEmpty { NewNoteContent.defaultBeginningTemplates }
    var selectedTemplateId by remember {
        mutableStateOf(
            templates
                .firstOrNull { it.id == defaultBeginningTemplateId }
                ?.id
                ?: templates.first().id,
        )
    }
    var templateMenuExpanded by remember { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }
    val untitledFileName = "$untitledFileStem.md"
    val selectedTemplate =
        templates.firstOrNull { it.id == selectedTemplateId } ?: templates.first()

    LaunchedEffect(Unit) {
        titleFocus.requestFocus()
    }

    val canCreate =
        noteTitle.isNotBlank() &&
            NotesTreeRepository.normalizeMarkdownFileStem(fileStem).isNotEmpty()

    fun applyTitle(value: String) {
        noteTitle = value
        if (syncFileNameFromTitle) {
            fileStem = NotesTreeRepository.fileStemFromNoteTitle(value)
        }
    }

    fun confirm() {
        if (!canCreate) {
            return
        }
        onConfirm(
            NotesTreeRepository.normalizeMarkdownFileStem(fileStem),
            noteTitle.trim(),
            selectedTemplate.id,
            isCanvas,
        )
    }

    fun confirmUntitled() {
        onConfirm(untitledFileStem, untitledFileStem, selectedTemplate.id, isCanvas)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.markdown_notes_create_note_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = ::confirmUntitled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = untitledFileName)
                }
                ExposedDropdownMenuBox(
                    expanded = templateMenuExpanded,
                    onExpandedChange = { templateMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedTemplate.label,
                        onValueChange = {},
                        readOnly = true,
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        label = {
                            Text(stringResource(R.string.markdown_notes_create_note_template_label))
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = templateMenuExpanded)
                        },
                    )
                    ExposedDropdownMenu(
                        expanded = templateMenuExpanded,
                        onDismissRequest = { templateMenuExpanded = false },
                    ) {
                        templates.forEach { template ->
                            DropdownMenuItem(
                                text = { Text(template.label) },
                                onClick = {
                                    selectedTemplateId = template.id
                                    templateMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = noteTitle,
                    onValueChange = ::applyTitle,
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocus),
                    label = { Text(stringResource(R.string.markdown_notes_create_note_name_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.markdown_notes_create_note_extension),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = fileStem,
                        onValueChange = { value ->
                            if (syncFileNameFromTitle) {
                                syncFileNameFromTitle = false
                            }
                            fileStem = value
                        },
                        modifier = Modifier.weight(1f),
                        label = {
                            Text(stringResource(R.string.markdown_notes_create_note_file_label))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions =
                        KeyboardActions(
                            onDone = { confirm() },
                        ),
                    )
                }
                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            role = Role.Checkbox,
                            onClick = {
                                val enabled = !syncFileNameFromTitle
                                syncFileNameFromTitle = enabled
                                if (enabled) {
                                    fileStem = NotesTreeRepository.fileStemFromNoteTitle(noteTitle)
                                }
                            },
                        )
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = syncFileNameFromTitle,
                        onCheckedChange = null,
                    )
                    Text(
                        text = stringResource(R.string.markdown_notes_create_note_sync_file_name),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            role = Role.Checkbox,
                            onClick = { isCanvas = !isCanvas },
                        )
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = isCanvas,
                        onCheckedChange = null,
                    )
                    Text(
                        text = stringResource(R.string.markdown_notes_create_note_canvas),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { confirm() },
                enabled = canCreate,
            ) {
                Text(stringResource(R.string.markdown_notes_create_note_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.markdown_notes_create_note_cancel))
            }
        },
    )
}

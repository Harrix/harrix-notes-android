package dev.harrix.notes.ui.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.harrix.notes.NewNoteContent
import dev.harrix.notes.NotesContentFont
import dev.harrix.notes.NotesCreateKind
import dev.harrix.notes.NotesTemplateSource
import dev.harrix.notes.NotesTreeRepository
import dev.harrix.notes.R

@Composable
fun NotesCreateKindMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onSelect: (NotesCreateKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        NotesCreateKind.entries.forEach { kind ->
            NotesDropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(createKindLabelRes(kind)),
                        maxLines = 1,
                    )
                },
                onClick = {
                    onDismissRequest()
                    onSelect(kind)
                },
            )
        }
    }
}

fun createKindLabelRes(kind: NotesCreateKind): Int = when (kind) {
    NotesCreateKind.RegularNote -> R.string.markdown_notes_create_kind_regular
    NotesCreateKind.QuickNote -> R.string.markdown_notes_create_kind_quick
    NotesCreateKind.CanvasNote -> R.string.markdown_notes_create_kind_canvas
    NotesCreateKind.QuickCanvas -> R.string.markdown_notes_create_kind_quick_canvas
    NotesCreateKind.FromTemplate -> R.string.markdown_notes_create_kind_template
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesCreateNoteDialog(
    kind: NotesCreateKind,
    beginningTemplates: List<NewNoteContent.BeginningTemplate>,
    defaultBeginningTemplateId: String,
    editorFontSizeSp: Int,
    editorFont: NotesContentFont,
    highlightMaxChars: Int,
    onDismiss: () -> Unit,
    onConfirm: (
        fileStem: String,
        noteTitle: String,
        beginningTemplateId: String,
        isCanvas: Boolean,
    ) -> Unit,
) {
    val isCanvas = kind == NotesCreateKind.CanvasNote
    val showTemplatePicker = kind == NotesCreateKind.FromTemplate
    var noteTitle by remember { mutableStateOf("") }
    var fileStem by remember { mutableStateOf("") }
    var syncFileNameFromTitle by remember { mutableStateOf(true) }
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
    var showTemplatePreview by remember { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }
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
        val templateId =
            if (showTemplatePicker) {
                selectedTemplate.id
            } else {
                defaultBeginningTemplateId
            }
        onConfirm(
            NotesTreeRepository.normalizeMarkdownFileStem(fileStem),
            noteTitle.trim(),
            templateId,
            isCanvas,
        )
    }

    val titleRes =
        when (kind) {
            NotesCreateKind.CanvasNote -> R.string.markdown_notes_create_canvas_title
            NotesCreateKind.FromTemplate -> R.string.markdown_notes_create_template_title
            else -> R.string.markdown_notes_create_note_title
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (showTemplatePicker) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = templateMenuExpanded,
                            onExpandedChange = { templateMenuExpanded = it },
                            modifier = Modifier.weight(1f),
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
                                    Text(
                                        stringResource(R.string.markdown_notes_create_note_template_label),
                                    )
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = templateMenuExpanded,
                                    )
                                },
                            )
                            ExposedDropdownMenu(
                                expanded = templateMenuExpanded,
                                onDismissRequest = { templateMenuExpanded = false },
                            ) {
                                templates.forEach { template ->
                                    NotesDropdownMenuItem(
                                        text = {
                                            Text(
                                                text = templateDisplayLabel(template),
                                                maxLines = 1,
                                            )
                                        },
                                        onClick = {
                                            selectedTemplateId = template.id
                                            templateMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                        Text(
                            text = stringResource(R.string.markdown_notes_create_note_template_preview),
                            style =
                            MaterialTheme.typography.labelLarge.copy(
                                textDecoration = TextDecoration.Underline,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier =
                            Modifier
                                .clickable { showTemplatePreview = true }
                                .padding(vertical = 8.dp),
                        )
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
                    Text(
                        text = stringResource(R.string.markdown_notes_create_note_extension),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

    if (showTemplatePreview) {
        NotesTemplatePreviewDialog(
            template = selectedTemplate,
            editorFontSizeSp = editorFontSizeSp,
            editorFont = editorFont,
            highlightMaxChars = highlightMaxChars,
            onDismiss = { showTemplatePreview = false },
        )
    }
}

@Composable
fun NotesTemplatePreviewDialog(
    template: NewNoteContent.BeginningTemplate,
    editorFontSizeSp: Int,
    editorFont: NotesContentFont,
    highlightMaxChars: Int,
    onDismiss: () -> Unit,
) {
    val controller = remember { NotesMarkdownEditorController() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(template.label) },
        text = {
            Box(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .height(240.dp),
            ) {
                NotesMarkdownEditorPane(
                    isLoading = false,
                    docKey = "template-preview-${template.id}",
                    text = template.content,
                    errorMessage = null,
                    hasContent = template.content.isNotEmpty(),
                    fontSizeSp = editorFontSizeSp,
                    highlightMaxChars = highlightMaxChars,
                    controller = controller,
                    onTextChange = {},
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    font = editorFont,
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
fun templateDisplayLabel(template: NewNoteContent.BeginningTemplate): String {
    val badge =
        when (template.source) {
            NotesTemplateSource.System -> stringResource(R.string.settings_new_note_template_badge_system)
            NotesTemplateSource.User -> stringResource(R.string.settings_new_note_template_badge_user)
        }
    return "${template.label} ($badge)"
}

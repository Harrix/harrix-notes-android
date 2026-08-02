package dev.harrix.notes.ui.settings

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.harrix.notes.AppPreferences
import dev.harrix.notes.NotesBrowseLayout
import dev.harrix.notes.NotesListDensity
import dev.harrix.notes.NotesOpenMode
import dev.harrix.notes.NotesPinnedItem
import dev.harrix.notes.NotesPinnedKind
import dev.harrix.notes.NotesTitleSource
import dev.harrix.notes.NotesTreeRepository
import dev.harrix.notes.NotesViewerPreferences
import dev.harrix.notes.R
import dev.harrix.notes.ui.adaptiveContentWidth
import dev.harrix.notes.ui.notes.NotesFolderPathControls
import dev.harrix.notes.ui.notes.NotesNoteGlyph
import dev.harrix.notes.ui.theme.ThemeMode
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    uiFontSizeSp: Int,
    onUiFontSizeChange: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appPreferences = remember { AppPreferences(context.applicationContext) }
    val notesPreferences = remember { NotesViewerPreferences(context.applicationContext) }
    var settingsEpoch by rememberSaveable { mutableIntStateOf(0) }
    var resetMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    BackHandler(onBack = onClose)

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .adaptiveContentWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            key(settingsEpoch) {
                AppearanceSettingsSection(
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    uiFontSizeSp = uiFontSizeSp,
                    onUiFontSizeChange = onUiFontSizeChange,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                NotesSettingsSection(showSectionTitle = true)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = stringResource(R.string.settings_reset_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingsFullWidthOutlinedButton(
                onClick = {
                    appPreferences.resetAppearanceToDefaults()
                    notesPreferences.resetSettingsToDefaults()
                    onThemeModeChange(ThemeMode.System)
                    onUiFontSizeChange(AppPreferences.DEFAULT_UI_FONT_SIZE_SP)
                    settingsEpoch += 1
                    resetMessage = context.getString(R.string.settings_reset_done)
                },
                label = stringResource(R.string.settings_reset),
            )
            resetMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsFullWidthOutlinedButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CollapsibleSettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = true,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    role = Role.Button,
                    onClick = { expanded = !expanded },
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector =
                if (expanded) {
                    Icons.Filled.ExpandLess
                } else {
                    Icons.Filled.ExpandMore
                },
                contentDescription =
                stringResource(
                    if (expanded) {
                        R.string.settings_section_collapse
                    } else {
                        R.string.settings_section_expand
                    },
                ),
            )
        }
        if (expanded) {
            content()
        }
    }
}

@Composable
private fun NotesSettingsSection(
    showSectionTitle: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val preferences = remember { NotesViewerPreferences(context.applicationContext) }
    val repository = remember { NotesTreeRepository(context.applicationContext) }
    var treeUri by remember { mutableStateOf(preferences.loadNotesTreeUri()) }
    var browseLayout by remember { mutableStateOf(preferences.loadBrowseLayout()) }
    var listDensity by remember { mutableStateOf(preferences.loadListDensity()) }
    var titleSource by remember { mutableStateOf(preferences.loadTitleSource()) }
    var noteOpenMode by remember { mutableStateOf(preferences.loadNoteOpenMode()) }
    var previewFontSizeText by remember {
        mutableStateOf(preferences.loadPreviewFontSizeSp().toString())
    }
    var editorFontSizeText by remember {
        mutableStateOf(preferences.loadEditorFontSizeSp().toString())
    }
    var maxOpenTabsText by remember {
        mutableStateOf(preferences.loadMaxOpenTabs().toString())
    }
    var pinnedBarEnabled by remember { mutableStateOf(preferences.loadPinnedBarEnabled()) }
    var maxPinnedText by remember {
        mutableStateOf(preferences.loadMaxPinnedItems().toString())
    }
    var pinnedItems by remember(treeUri) {
        mutableStateOf(loadPinnedItemsForSettings(preferences, repository, treeUri))
    }

    val layoutOptions =
        listOf(
            NotesBrowseLayout.List to R.string.settings_markdown_notes_browse_layout_list,
            NotesBrowseLayout.Icons to R.string.settings_markdown_notes_browse_layout_icons,
        )
    val densityOptions =
        listOf(
            NotesListDensity.Compact to R.string.settings_markdown_notes_list_density_compact,
            NotesListDensity.Comfortable to R.string.settings_markdown_notes_list_density_comfortable,
            NotesListDensity.Spacious to R.string.settings_markdown_notes_list_density_spacious,
        )
    val titleSourceOptions =
        listOf(
            NotesTitleSource.Content to R.string.settings_markdown_notes_title_source_content,
            NotesTitleSource.FileName to R.string.settings_markdown_notes_title_source_file_name,
        )
    val openModeOptions =
        listOf(
            NotesOpenMode.Preview to R.string.settings_markdown_notes_open_mode_preview,
            NotesOpenMode.Edit to R.string.settings_markdown_notes_open_mode_edit,
        )

    fun persistPinned(items: List<NotesPinnedItem>) {
        val uri = treeUri ?: return
        val limited = items.take(preferences.loadMaxPinnedItems())
        pinnedItems = limited
        preferences.savePinnedItems(uri, limited)
    }

    val body: @Composable () -> Unit = {
        NotesFolderPathControls(
            treeUri = treeUri,
            onTreeUriChange = {
                treeUri = it
                pinnedItems = loadPinnedItemsForSettings(preferences, repository, it)
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_markdown_notes_open_mode),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            openModeOptions.forEachIndexed { index, (mode, labelRes) ->
                SegmentedButton(
                    selected = noteOpenMode == mode,
                    onClick = {
                        noteOpenMode = mode
                        preferences.saveNoteOpenMode(mode)
                    },
                    shape =
                    SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = openModeOptions.size,
                    ),
                ) {
                    Text(
                        text = stringResource(labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        FontSizeField(
            label = stringResource(R.string.settings_markdown_notes_preview_font_size),
            valueText = previewFontSizeText,
            onValueTextChange = { previewFontSizeText = it },
            onCommit = { preferences.savePreviewFontSizeSp(it) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        FontSizeField(
            label = stringResource(R.string.settings_markdown_notes_editor_font_size),
            valueText = editorFontSizeText,
            onValueTextChange = { editorFontSizeText = it },
            onCommit = { preferences.saveEditorFontSizeSp(it) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_markdown_notes_browse_layout),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            layoutOptions.forEachIndexed { index, (layout, labelRes) ->
                SegmentedButton(
                    selected = browseLayout == layout,
                    onClick = {
                        browseLayout = layout
                        preferences.saveBrowseLayout(layout)
                    },
                    shape =
                    SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = layoutOptions.size,
                    ),
                ) {
                    Text(
                        text = stringResource(labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_markdown_notes_title_source),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            titleSourceOptions.forEachIndexed { index, (source, labelRes) ->
                SegmentedButton(
                    selected = titleSource == source,
                    onClick = {
                        titleSource = source
                        preferences.saveTitleSource(source)
                    },
                    shape =
                    SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = titleSourceOptions.size,
                    ),
                ) {
                    Text(
                        text = stringResource(labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_markdown_notes_max_open_tabs),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = maxOpenTabsText,
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(2)
                maxOpenTabsText = digits
                val parsed = digits.toIntOrNull() ?: return@OutlinedTextField
                val clamped =
                    parsed.coerceIn(
                        NotesViewerPreferences.MIN_OPEN_TABS,
                        NotesViewerPreferences.MAX_OPEN_TABS,
                    )
                preferences.saveMaxOpenTabs(clamped)
                if (parsed != clamped) {
                    maxOpenTabsText = clamped.toString()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            supportingText = {
                Text(stringResource(R.string.settings_markdown_notes_max_open_tabs_hint))
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_markdown_notes_list_density),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            densityOptions.forEachIndexed { index, (density, labelRes) ->
                SegmentedButton(
                    selected = listDensity == density,
                    onClick = {
                        listDensity = density
                        preferences.saveListDensity(density)
                    },
                    shape =
                    SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = densityOptions.size,
                    ),
                ) {
                    Text(
                        text = stringResource(labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_markdown_notes_pinned_bar),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_markdown_notes_pinned_bar_enabled),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = pinnedBarEnabled,
                onCheckedChange = { enabled ->
                    pinnedBarEnabled = enabled
                    preferences.savePinnedBarEnabled(enabled)
                },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_markdown_notes_max_pinned),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = maxPinnedText,
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(2)
                maxPinnedText = digits
                val parsed = digits.toIntOrNull() ?: return@OutlinedTextField
                val clamped =
                    parsed.coerceIn(
                        NotesViewerPreferences.MIN_PINNED_ITEMS,
                        NotesViewerPreferences.MAX_PINNED_ITEMS,
                    )
                preferences.saveMaxPinnedItems(clamped)
                if (parsed != clamped) {
                    maxPinnedText = clamped.toString()
                }
                persistPinned(pinnedItems)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            supportingText = {
                Text(stringResource(R.string.settings_markdown_notes_max_pinned_hint))
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_markdown_notes_pinned_items),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (treeUri.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.settings_markdown_notes_pinned_items_need_folder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (pinnedItems.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_markdown_notes_pinned_items_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            pinnedItems.forEachIndexed { index, item ->
                SettingsPinnedItemRow(
                    item = item,
                    onRemove = {
                        persistPinned(pinnedItems.filterNot { it.id == item.id })
                    },
                    onReorderBySteps = { steps ->
                        if (steps == 0) {
                            return@SettingsPinnedItemRow
                        }
                        val toIndex = (index + steps).coerceIn(0, pinnedItems.lastIndex)
                        if (toIndex == index) {
                            return@SettingsPinnedItemRow
                        }
                        val mutable = pinnedItems.toMutableList()
                        val moved = mutable.removeAt(index)
                        mutable.add(toIndex, moved)
                        persistPinned(mutable)
                    },
                )
            }
        }
    }

    if (showSectionTitle) {
        CollapsibleSettingsSection(
            title = stringResource(R.string.settings_notes_title),
            modifier = modifier,
            content = body,
        )
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = { body() },
        )
    }
}

private fun loadPinnedItemsForSettings(
    preferences: NotesViewerPreferences,
    repository: NotesTreeRepository,
    treeUri: String?,
): List<NotesPinnedItem> {
    if (treeUri.isNullOrBlank()) {
        return emptyList()
    }
    val root =
        runCatching { repository.rootSegment(Uri.parse(treeUri)) }.getOrNull()
            ?: return emptyList()
    return preferences.loadPinnedItems(treeUri, root)
}

private val SettingsPinnedReorderStepHeight = 48.dp

@Composable
private fun SettingsPinnedItemRow(
    item: NotesPinnedItem,
    onRemove: () -> Unit,
    onReorderBySteps: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val reorderStepPx = with(density) { SettingsPinnedReorderStepHeight.toPx() }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val label =
        when {
            item.kind == NotesPinnedKind.Home || item.id == NotesPinnedItem.HOME_ID -> {
                stringResource(R.string.nav_drawer_home)
            }

            else -> item.title.ifBlank { item.documentId }
        }

    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, dragOffsetY.roundToInt()) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.DragHandle,
            contentDescription = stringResource(R.string.settings_markdown_notes_pinned_reorder),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
            Modifier
                .size(40.dp)
                .padding(8.dp)
                .pointerInput(item.id) {
                    detectDragGestures(
                        onDragEnd = {
                            if (abs(dragOffsetY) >= reorderStepPx / 2f) {
                                onReorderBySteps((dragOffsetY / reorderStepPx).roundToInt())
                            }
                            dragOffsetY = 0f
                        },
                        onDragCancel = { dragOffsetY = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetY += dragAmount.y
                        },
                    )
                },
        )
        when (item.kind) {
            NotesPinnedKind.Home -> {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }

            NotesPinnedKind.Folder -> {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }

            NotesPinnedKind.Note -> {
                NotesNoteGlyph(icon = item.icon, size = 20.dp)
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
            Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        )
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.settings_markdown_notes_pinned_remove),
            )
        }
    }
}

@Composable
private fun AppearanceSettingsSection(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    uiFontSizeSp: Int,
    onUiFontSizeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options =
        listOf(
            ThemeMode.System to R.string.settings_theme_system,
            ThemeMode.Light to R.string.settings_theme_light,
            ThemeMode.Dark to R.string.settings_theme_dark,
        )
    var uiFontSizeText by remember(uiFontSizeSp) { mutableStateOf(uiFontSizeSp.toString()) }

    CollapsibleSettingsSection(
        title = stringResource(R.string.settings_appearance_title),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.settings_theme_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (mode, labelRes) ->
                SegmentedButton(
                    selected = themeMode == mode,
                    onClick = { onThemeModeChange(mode) },
                    shape =
                    SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size,
                    ),
                ) {
                    Text(
                        text = stringResource(labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        FontSizeField(
            label = stringResource(R.string.settings_ui_font_size),
            valueText = uiFontSizeText,
            onValueTextChange = { uiFontSizeText = it },
            onCommit = onUiFontSizeChange,
        )
    }
}

@Composable
private fun FontSizeField(
    label: String,
    valueText: String,
    onValueTextChange: (String) -> Unit,
    onCommit: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = valueText,
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(2)
                onValueTextChange(digits)
                val parsed = digits.toIntOrNull() ?: return@OutlinedTextField
                val clamped =
                    parsed.coerceIn(
                        AppPreferences.MIN_FONT_SIZE_SP,
                        AppPreferences.MAX_FONT_SIZE_SP,
                    )
                onCommit(clamped)
                if (parsed != clamped) {
                    onValueTextChange(clamped.toString())
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            supportingText = {
                Text(stringResource(R.string.settings_font_size_hint))
            },
        )
    }
}

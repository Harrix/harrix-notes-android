package dev.harrix.notes.ui.settings

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.harrix.notes.AppPreferences
import dev.harrix.notes.NewNoteContent
import dev.harrix.notes.NotesBrowseLayout
import dev.harrix.notes.NotesContentFont
import dev.harrix.notes.NotesIconStyle
import dev.harrix.notes.NotesListDensity
import dev.harrix.notes.NotesOpenMode
import dev.harrix.notes.NotesPinnedItem
import dev.harrix.notes.NotesPinnedKind
import dev.harrix.notes.NotesSortBy
import dev.harrix.notes.NotesTitleSource
import dev.harrix.notes.NotesTreeRepository
import dev.harrix.notes.NotesViewerPreferences
import dev.harrix.notes.R
import dev.harrix.notes.pinnedDisplayLabels
import dev.harrix.notes.ui.adaptiveContentWidth
import dev.harrix.notes.ui.notes.LocalNotesIconStyle
import dev.harrix.notes.ui.notes.NotesDropdownMenuItem
import dev.harrix.notes.ui.notes.NotesFolderGlyph
import dev.harrix.notes.ui.notes.NotesFolderPathControls
import dev.harrix.notes.ui.notes.NotesNoteGlyph
import dev.harrix.notes.ui.theme.AppLanguage
import dev.harrix.notes.ui.theme.ThemeMode
import dev.harrix.notes.ui.theme.notesScaffoldContainerColor
import dev.harrix.notes.ui.theme.notesScaffoldContentWindowInsets
import dev.harrix.notes.ui.theme.notesTopAppBarColors
import dev.harrix.notes.ui.theme.notesTopAppBarWindowInsets
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Settings navigation mirrors Markor's nested preference screens:
 * General / Edit mode / View mode, with Theme & Language on the hub root.
 */
private enum class NotesSettingsPage {
    Hub,
    General,
    EditMode,
    ViewMode,
    NewNote,
    Other,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    uiFontSizeSp: Int,
    onUiFontSizeChange: (Int) -> Unit,
    appLanguage: AppLanguage,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appPreferences = remember { AppPreferences(context.applicationContext) }
    val notesPreferences = remember { NotesViewerPreferences(context.applicationContext) }
    var settingsEpoch by rememberSaveable { mutableIntStateOf(0) }
    var resetMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var page by rememberSaveable { mutableStateOf(NotesSettingsPage.Hub) }

    val pageTitle =
        when (page) {
            NotesSettingsPage.Hub -> stringResource(R.string.settings_title)
            NotesSettingsPage.General -> stringResource(R.string.settings_general_title)
            NotesSettingsPage.EditMode -> stringResource(R.string.settings_edit_mode_title)
            NotesSettingsPage.ViewMode -> stringResource(R.string.settings_view_mode_title)
            NotesSettingsPage.NewNote -> stringResource(R.string.settings_new_note_title)
            NotesSettingsPage.Other -> stringResource(R.string.settings_other_title)
        }

    BackHandler {
        if (page == NotesSettingsPage.Hub) {
            onClose()
        } else {
            page = NotesSettingsPage.Hub
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = notesScaffoldContainerColor(),
        contentWindowInsets = notesScaffoldContentWindowInsets(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = pageTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = notesTopAppBarColors(),
                windowInsets = notesTopAppBarWindowInsets(),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (page == NotesSettingsPage.Hub) {
                                onClose()
                            } else {
                                page = NotesSettingsPage.Hub
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (page) {
            NotesSettingsPage.Hub -> {
                Column(
                    modifier =
                    Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .adaptiveContentWidth(),
                ) {
                    SettingsHubRow(
                        title = stringResource(R.string.settings_general_title),
                        summary = stringResource(R.string.settings_general_summary),
                        icon = Icons.Filled.Folder,
                        onClick = { page = NotesSettingsPage.General },
                    )
                    HorizontalDivider()
                    SettingsHubRow(
                        title = stringResource(R.string.settings_edit_mode_title),
                        summary = stringResource(R.string.settings_edit_mode_summary),
                        icon = Icons.Filled.Edit,
                        onClick = { page = NotesSettingsPage.EditMode },
                    )
                    HorizontalDivider()
                    SettingsHubRow(
                        title = stringResource(R.string.settings_view_mode_title),
                        summary = stringResource(R.string.settings_view_mode_summary),
                        icon = Icons.Filled.Visibility,
                        onClick = { page = NotesSettingsPage.ViewMode },
                    )
                    HorizontalDivider()
                    SettingsHubRow(
                        title = stringResource(R.string.settings_new_note_title),
                        summary = stringResource(R.string.settings_new_note_summary),
                        icon = Icons.Filled.Add,
                        onClick = { page = NotesSettingsPage.NewNote },
                    )
                    SettingsCategoryHeader(text = stringResource(R.string.settings_category_essential))
                    key(settingsEpoch) {
                        EssentialSettingsSection(
                            themeMode = themeMode,
                            onThemeModeChange = onThemeModeChange,
                            uiFontSizeSp = uiFontSizeSp,
                            onUiFontSizeChange = onUiFontSizeChange,
                            appLanguage = appLanguage,
                            onAppLanguageChange = onAppLanguageChange,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    SettingsCategoryHeader(text = stringResource(R.string.settings_category_main))
                    SettingsHubRow(
                        title = stringResource(R.string.settings_other_title),
                        summary = stringResource(R.string.settings_other_summary),
                        icon = Icons.Filled.MoreHoriz,
                        onClick = { page = NotesSettingsPage.Other },
                    )
                }
            }

            NotesSettingsPage.General -> {
                SettingsDetailPane(innerPadding = innerPadding) {
                    key(settingsEpoch) {
                        GeneralSettingsSection()
                    }
                }
            }

            NotesSettingsPage.EditMode -> {
                SettingsDetailPane(innerPadding = innerPadding) {
                    key(settingsEpoch) {
                        EditModeSettingsSection()
                    }
                }
            }

            NotesSettingsPage.ViewMode -> {
                SettingsDetailPane(innerPadding = innerPadding) {
                    key(settingsEpoch) {
                        ViewModeSettingsSection()
                    }
                }
            }

            NotesSettingsPage.NewNote -> {
                SettingsDetailPane(innerPadding = innerPadding) {
                    key(settingsEpoch) {
                        NewNoteSettingsSection()
                    }
                }
            }

            NotesSettingsPage.Other -> {
                SettingsDetailPane(innerPadding = innerPadding) {
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
                            onAppLanguageChange(AppLanguage.System)
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
    }
}

@Composable
private fun SettingsDetailPane(
    innerPadding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
        Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .adaptiveContentWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun SettingsCategoryHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsHubRow(
    title: String,
    summary: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(text = title)
        },
        supportingContent = {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors =
        ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClick = onClick,
            ),
    )
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
private fun GeneralSettingsSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val preferences = remember { NotesViewerPreferences(context.applicationContext) }
    val repository = remember { NotesTreeRepository(context.applicationContext) }
    var treeUri by remember { mutableStateOf(preferences.loadNotesTreeUri()) }
    var browseLayout by remember { mutableStateOf(preferences.loadBrowseLayout()) }
    var iconStyle by remember { mutableStateOf(preferences.loadIconStyle()) }
    var listDensity by remember { mutableStateOf(preferences.loadListDensity()) }
    var treeDensity by remember { mutableStateOf(preferences.loadTreeDensity()) }
    var pinnedBarDensity by remember { mutableStateOf(preferences.loadPinnedBarDensity()) }
    var titleSource by remember { mutableStateOf(preferences.loadTitleSource()) }
    var noteOpenMode by remember { mutableStateOf(preferences.loadNoteOpenMode()) }
    var singleNoteMode by remember { mutableStateOf(preferences.loadSingleNoteMode()) }
    var dualPaneEnabled by remember { mutableStateOf(preferences.loadDualPaneEnabled()) }
    var showNoteDates by remember { mutableStateOf(preferences.loadShowNoteDates()) }
    var showNotePath by remember { mutableStateOf(preferences.loadShowNotePath()) }
    var sortBy by remember { mutableStateOf(preferences.loadSortBy()) }
    var foldersFirst by remember { mutableStateOf(preferences.loadFoldersFirst()) }
    var sortReverseOrder by remember { mutableStateOf(preferences.loadSortReverseOrder()) }
    var showGmdFiles by remember { mutableStateOf(preferences.loadShowGmdFiles()) }
    var maxOpenTabs by remember { mutableIntStateOf(preferences.loadMaxOpenTabs()) }
    var pinnedBarEnabled by remember { mutableStateOf(preferences.loadPinnedBarEnabled()) }
    var maxPinnedItems by remember { mutableIntStateOf(preferences.loadMaxPinnedItems()) }
    var pinnedItems by remember(treeUri) {
        mutableStateOf(loadPinnedItemsForSettings(preferences, repository, treeUri))
    }

    val layoutOptions =
        listOf(
            NotesBrowseLayout.List to R.string.settings_markdown_notes_browse_layout_list,
            NotesBrowseLayout.Icons to R.string.settings_markdown_notes_browse_layout_icons,
        )
    val iconStyleOptions =
        listOf(
            NotesIconStyle.Harrix to R.string.settings_markdown_notes_icon_style_harrix,
            NotesIconStyle.Material to R.string.settings_markdown_notes_icon_style_material,
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
    val sortByOptions =
        listOf(
            NotesSortBy.Name to R.string.markdown_notes_sort_by_name,
            NotesSortBy.Date to R.string.markdown_notes_sort_by_date,
            NotesSortBy.Size to R.string.markdown_notes_sort_by_size,
        )

    fun persistPinned(items: List<NotesPinnedItem>) {
        val uri = treeUri ?: return
        val limited = items.take(preferences.loadMaxPinnedItems())
        pinnedItems = limited
        preferences.savePinnedItems(uri, limited)
    }

    CompositionLocalProvider(LocalNotesIconStyle provides iconStyle) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsSectionHeader(text = stringResource(R.string.settings_category_location))
            NotesFolderPathControls(
                treeUri = treeUri,
                onTreeUriChange = {
                    treeUri = it
                    pinnedItems = loadPinnedItemsForSettings(preferences, repository, it)
                },
            )

            SettingsSectionHeader(text = stringResource(R.string.settings_category_opening))
            SettingsChoiceRow(
                label = stringResource(R.string.settings_markdown_notes_open_mode),
                options = openModeOptions,
                selected = noteOpenMode,
                onSelect = { mode ->
                    noteOpenMode = mode
                    preferences.saveNoteOpenMode(mode)
                },
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_markdown_notes_single_note_mode),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = singleNoteMode,
                        onCheckedChange = { enabled ->
                            singleNoteMode = enabled
                            preferences.saveSingleNoteMode(enabled)
                        },
                    )
                }
                Text(
                    text = stringResource(R.string.settings_markdown_notes_single_note_mode_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_markdown_notes_dual_pane),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = dualPaneEnabled,
                        onCheckedChange = { enabled ->
                            dualPaneEnabled = enabled
                            preferences.saveDualPaneEnabled(enabled)
                        },
                    )
                }
                Text(
                    text = stringResource(R.string.settings_markdown_notes_dual_pane_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SettingsSectionHeader(text = stringResource(R.string.settings_category_file_browser))
            SettingsChoiceRow(
                label = stringResource(R.string.settings_markdown_notes_browse_layout),
                options = layoutOptions,
                selected = browseLayout,
                onSelect = { layout ->
                    browseLayout = layout
                    preferences.saveBrowseLayout(layout)
                },
            )
            SettingsChoiceRow(
                label = stringResource(R.string.settings_markdown_notes_icon_style),
                options = iconStyleOptions,
                selected = iconStyle,
                onSelect = { style ->
                    iconStyle = style
                    preferences.saveIconStyle(style)
                },
            )
            SettingsChoiceRow(
                label = stringResource(R.string.settings_markdown_notes_title_source),
                options = titleSourceOptions,
                selected = titleSource,
                onSelect = { source ->
                    titleSource = source
                    preferences.saveTitleSource(source)
                },
            )
            SettingsChoiceRow(
                label = stringResource(R.string.settings_markdown_notes_sort_by),
                options = sortByOptions,
                selected = sortBy,
                onSelect = { value ->
                    sortBy = value
                    preferences.saveSortBy(value)
                },
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.markdown_notes_sort_folders_first),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = foldersFirst,
                        onCheckedChange = { enabled ->
                            foldersFirst = enabled
                            preferences.saveFoldersFirst(enabled)
                        },
                    )
                }
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.markdown_notes_sort_reverse),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = sortReverseOrder,
                        onCheckedChange = { enabled ->
                            sortReverseOrder = enabled
                            preferences.saveSortReverseOrder(enabled)
                        },
                    )
                }
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.markdown_notes_sort_show_gmd),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = showGmdFiles,
                        onCheckedChange = { enabled ->
                            showGmdFiles = enabled
                            preferences.saveShowGmdFiles(enabled)
                        },
                    )
                }
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_markdown_notes_show_note_dates),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = showNoteDates,
                        onCheckedChange = { enabled ->
                            showNoteDates = enabled
                            preferences.saveShowNoteDates(enabled)
                        },
                    )
                }
                Text(
                    text = stringResource(R.string.settings_markdown_notes_show_note_dates_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_markdown_notes_show_note_path),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = showNotePath,
                        onCheckedChange = { enabled ->
                            showNotePath = enabled
                            preferences.saveShowNotePath(enabled)
                        },
                    )
                }
                Text(
                    text = stringResource(R.string.settings_markdown_notes_show_note_path_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!singleNoteMode) {
                IntSliderSetting(
                    label = stringResource(R.string.settings_markdown_notes_max_open_tabs),
                    value = maxOpenTabs,
                    valueRange = NotesViewerPreferences.MIN_OPEN_TABS..NotesViewerPreferences.MAX_OPEN_TABS,
                    hint = stringResource(R.string.settings_markdown_notes_max_open_tabs_hint),
                    onValueChange = { value ->
                        maxOpenTabs = value
                        preferences.saveMaxOpenTabs(value)
                    },
                )
            }
            NotesDensitySettingRow(
                labelRes = R.string.settings_markdown_notes_list_density,
                selected = listDensity,
                options = densityOptions,
                onSelect = { density ->
                    listDensity = density
                    preferences.saveListDensity(density)
                },
            )
            NotesDensitySettingRow(
                labelRes = R.string.settings_markdown_notes_tree_density,
                selected = treeDensity,
                options = densityOptions,
                onSelect = { density ->
                    treeDensity = density
                    preferences.saveTreeDensity(density)
                },
            )

            SettingsSectionHeader(text = stringResource(R.string.settings_category_pinned_bar))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_markdown_notes_pinned_bar_enabled),
                    style = MaterialTheme.typography.bodyLarge,
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
            IntSliderSetting(
                label = stringResource(R.string.settings_markdown_notes_max_pinned),
                value = maxPinnedItems,
                valueRange = NotesViewerPreferences.MIN_PINNED_ITEMS..NotesViewerPreferences.MAX_PINNED_ITEMS,
                hint = stringResource(R.string.settings_markdown_notes_max_pinned_hint),
                onValueChange = { value ->
                    maxPinnedItems = value
                    preferences.saveMaxPinnedItems(value)
                    persistPinned(pinnedItems)
                },
            )
            NotesDensitySettingRow(
                labelRes = R.string.settings_markdown_notes_pinned_bar_density,
                selected = pinnedBarDensity,
                options = densityOptions,
                onSelect = { density ->
                    pinnedBarDensity = density
                    preferences.savePinnedBarDensity(density)
                },
            )
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
                val homeLabel = stringResource(R.string.nav_drawer_home)
                val displayLabels =
                    remember(pinnedItems, homeLabel) {
                        pinnedDisplayLabels(pinnedItems, homeLabel)
                    }
                pinnedItems.forEachIndexed { index, item ->
                    SettingsPinnedItemRow(
                        item = item,
                        label = displayLabels[item.id].orEmpty().ifBlank { item.title },
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
    }
}

@Composable
private fun EditModeSettingsSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val preferences = remember { NotesViewerPreferences(context.applicationContext) }
    var editorFont by remember { mutableStateOf(preferences.loadEditorFont()) }
    var editorFontSizeSp by remember { mutableIntStateOf(preferences.loadEditorFontSizeSp()) }
    var highlightMaxMb by remember { mutableIntStateOf(preferences.loadHighlightMaxMb()) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ContentFontDropdown(
            label = stringResource(R.string.settings_markdown_notes_editor_font),
            selected = editorFont,
            onSelected = { font ->
                editorFont = font
                preferences.saveEditorFont(font)
            },
        )
        IntSliderSetting(
            label = stringResource(R.string.settings_markdown_notes_editor_font_size),
            value = editorFontSizeSp,
            valueRange = AppPreferences.MIN_FONT_SIZE_SP..AppPreferences.MAX_FONT_SIZE_SP,
            hint = stringResource(R.string.settings_font_size_hint),
            valueLabel = { "$it sp" },
            onValueChange = { value ->
                editorFontSizeSp = value
                preferences.saveEditorFontSizeSp(value)
            },
        )
        SettingsSectionHeader(text = stringResource(R.string.settings_category_syntax_highlighting))
        IntSliderSetting(
            label = stringResource(R.string.settings_markdown_notes_highlight_max_mb),
            value = highlightMaxMb,
            valueRange =
            NotesViewerPreferences.MIN_HIGHLIGHT_MAX_MB..NotesViewerPreferences.MAX_HIGHLIGHT_MAX_MB,
            hint = stringResource(R.string.settings_markdown_notes_highlight_max_mb_hint),
            valueLabel = { "$it MB" },
            onValueChange = { value ->
                highlightMaxMb = value
                preferences.saveHighlightMaxMb(value)
            },
        )
    }
}

@Composable
private fun ViewModeSettingsSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val preferences = remember { NotesViewerPreferences(context.applicationContext) }
    var previewFont by remember { mutableStateOf(preferences.loadPreviewFont()) }
    var previewFontSizeSp by remember { mutableIntStateOf(preferences.loadPreviewFontSizeSp()) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ContentFontDropdown(
            label = stringResource(R.string.settings_markdown_notes_preview_font),
            selected = previewFont,
            onSelected = { font ->
                previewFont = font
                preferences.savePreviewFont(font)
            },
        )
        IntSliderSetting(
            label = stringResource(R.string.settings_markdown_notes_preview_font_size),
            value = previewFontSizeSp,
            valueRange = AppPreferences.MIN_FONT_SIZE_SP..AppPreferences.MAX_FONT_SIZE_SP,
            hint = stringResource(R.string.settings_font_size_hint),
            valueLabel = { "$it sp" },
            onValueChange = { value ->
                previewFontSizeSp = value
                preferences.savePreviewFontSizeSp(value)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentFontDropdown(
    label: String,
    selected: NotesContentFont,
    onSelected: (NotesContentFont) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = contentFontLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier =
            Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            NotesContentFont.entries.forEach { font ->
                NotesDropdownMenuItem(
                    text = { Text(contentFontLabel(font)) },
                    onClick = {
                        expanded = false
                        if (font != selected) {
                            onSelected(font)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun contentFontLabel(font: NotesContentFont): String =
    stringResource(
        when (font) {
            NotesContentFont.System -> R.string.settings_markdown_notes_font_system
            NotesContentFont.JetBrainsMono -> R.string.settings_markdown_notes_font_jetbrains_mono
            NotesContentFont.FiraSans -> R.string.settings_markdown_notes_font_fira_sans
        },
    )

/** @hsk-sync:new-note */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewNoteSettingsSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val preferences = remember { NotesViewerPreferences(context.applicationContext) }
    var personalEnabled by remember { mutableStateOf(preferences.loadPersonalDataEnabled()) }
    var author by remember { mutableStateOf(preferences.loadPersonalDataAuthor()) }
    var authorEmail by remember { mutableStateOf(preferences.loadPersonalDataAuthorEmail()) }
    var templates by remember { mutableStateOf(preferences.loadBeginningTemplates()) }
    var defaultTemplateId by remember { mutableStateOf(preferences.loadDefaultBeginningTemplateId()) }
    var selectedTemplateId by remember {
        mutableStateOf(defaultTemplateId.ifBlank { templates.firstOrNull()?.id.orEmpty() })
    }
    var templateLabel by remember {
        mutableStateOf(templates.firstOrNull { it.id == selectedTemplateId }?.label.orEmpty())
    }
    var templateContent by remember {
        mutableStateOf(templates.firstOrNull { it.id == selectedTemplateId }?.content.orEmpty())
    }
    var defaultMenuExpanded by remember { mutableStateOf(false) }
    var templateMenuExpanded by remember { mutableStateOf(false) }

    fun selectTemplate(id: String) {
        selectedTemplateId = id
        val template = templates.firstOrNull { it.id == id }
        templateLabel = template?.label.orEmpty()
        templateContent = template?.content.orEmpty()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsSectionHeader(text = stringResource(R.string.settings_new_note_personal_data))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.settings_new_note_personal_data_enabled),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Switch(
                checked = personalEnabled,
                onCheckedChange = { enabled ->
                    personalEnabled = enabled
                    preferences.savePersonalDataEnabled(enabled)
                },
            )
        }
        if (personalEnabled) {
            OutlinedTextField(
                value = author,
                onValueChange = { value ->
                    author = value
                    preferences.savePersonalDataAuthor(value)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_new_note_personal_data_author)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = authorEmail,
                onValueChange = { value ->
                    authorEmail = value
                    preferences.savePersonalDataAuthorEmail(value)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_new_note_personal_data_author_email)) },
                singleLine = true,
            )
        }

        SettingsSectionHeader(text = stringResource(R.string.settings_new_note_templates))
        ExposedDropdownMenuBox(
            expanded = defaultMenuExpanded,
            onExpandedChange = { defaultMenuExpanded = it },
        ) {
            val defaultLabel =
                templates.firstOrNull { it.id == defaultTemplateId }?.label
                    ?: defaultTemplateId
            OutlinedTextField(
                value = defaultLabel,
                onValueChange = {},
                readOnly = true,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                label = { Text(stringResource(R.string.settings_new_note_default_template)) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = defaultMenuExpanded)
                },
            )
            ExposedDropdownMenu(
                expanded = defaultMenuExpanded,
                onDismissRequest = { defaultMenuExpanded = false },
            ) {
                templates.forEach { template ->
                    DropdownMenuItem(
                        text = { Text(template.label) },
                        onClick = {
                            defaultTemplateId = template.id
                            preferences.saveDefaultBeginningTemplateId(template.id)
                            defaultMenuExpanded = false
                        },
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = templateMenuExpanded,
            onExpandedChange = { templateMenuExpanded = it },
        ) {
            OutlinedTextField(
                value = templates.firstOrNull { it.id == selectedTemplateId }?.label.orEmpty(),
                onValueChange = {},
                readOnly = true,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                label = { Text(stringResource(R.string.settings_new_note_templates)) },
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
                            selectTemplate(template.id)
                            templateMenuExpanded = false
                        },
                    )
                }
            }
        }

        OutlinedTextField(
            value = templateLabel,
            onValueChange = { templateLabel = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.settings_new_note_template_label)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = templateContent,
            onValueChange = { templateContent = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.settings_new_note_template_content)) },
            minLines = 6,
        )
        SettingsFullWidthOutlinedButton(
            onClick = {
                val id = selectedTemplateId.ifBlank { templateLabel.ifBlank { "template" } }
                val updated =
                    NewNoteContent.BeginningTemplate(
                        id = id,
                        label = templateLabel.ifBlank { id },
                        content = templateContent,
                    )
                templates =
                    templates.map { existing ->
                        if (existing.id == id) updated else existing
                    }.let { list ->
                        if (list.any { it.id == id }) list else list + updated
                    }
                preferences.saveBeginningTemplates(templates)
                selectTemplate(id)
            },
            label = stringResource(R.string.settings_new_note_template_save),
        )
        SettingsFullWidthOutlinedButton(
            onClick = {
                templates = NewNoteContent.defaultBeginningTemplates
                preferences.saveBeginningTemplates(templates)
                defaultTemplateId = templates.first().id
                preferences.saveDefaultBeginningTemplateId(defaultTemplateId)
                selectTemplate(defaultTemplateId)
            },
            label = stringResource(R.string.settings_new_note_templates_reset),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EssentialSettingsSection(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    uiFontSizeSp: Int,
    onUiFontSizeChange: (Int) -> Unit,
    appLanguage: AppLanguage,
    onAppLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options =
        listOf(
            ThemeMode.System to R.string.settings_theme_system,
            ThemeMode.Light to R.string.settings_theme_light,
            ThemeMode.Dark to R.string.settings_theme_dark,
        )
    var languageMenuExpanded by remember { mutableStateOf(false) }
    val systemLanguageLabel = stringResource(R.string.settings_language_system)
    val languageLabel =
        if (appLanguage == AppLanguage.System) {
            systemLanguageLabel
        } else {
            appLanguage.nativeLabel
        }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_language_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ExposedDropdownMenuBox(
            expanded = languageMenuExpanded,
            onExpandedChange = { languageMenuExpanded = it },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = languageLabel,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageMenuExpanded) },
                modifier =
                Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = languageMenuExpanded,
                onDismissRequest = { languageMenuExpanded = false },
            ) {
                AppLanguage.entries.forEach { language ->
                    val optionLabel =
                        if (language == AppLanguage.System) {
                            systemLanguageLabel
                        } else {
                            language.nativeLabel
                        }
                    NotesDropdownMenuItem(
                        text = {
                            Text(
                                text = optionLabel,
                                maxLines = 2,
                            )
                        },
                        onClick = {
                            languageMenuExpanded = false
                            if (language != appLanguage) {
                                onAppLanguageChange(language)
                            }
                        },
                    )
                }
            }
        }
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
        IntSliderSetting(
            label = stringResource(R.string.settings_ui_font_size),
            value = uiFontSizeSp,
            valueRange = AppPreferences.MIN_FONT_SIZE_SP..AppPreferences.MAX_FONT_SIZE_SP,
            hint = stringResource(R.string.settings_font_size_hint),
            valueLabel = { "$it sp" },
            onValueChange = onUiFontSizeChange,
        )
    }
}

@Composable
private fun <T> SettingsChoiceRow(
    label: String,
    options: List<Pair<T, Int>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (value, labelRes) ->
                SegmentedButton(
                    selected = selected == value,
                    onClick = { onSelect(value) },
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
    }
}

@Composable
private fun IntSliderSetting(
    label: String,
    value: Int,
    valueRange: ClosedRange<Int>,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    valueLabel: (Int) -> String = { it.toString() },
) {
    val min = valueRange.start
    val max = valueRange.endInclusive
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueLabel(value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(min, max)) },
            valueRange = min.toFloat()..max.toFloat(),
            steps = (max - min - 1).coerceAtLeast(0),
        )
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesDensitySettingRow(
    labelRes: Int,
    selected: NotesListDensity,
    options: List<Pair<NotesListDensity, Int>>,
    onSelect: (NotesListDensity) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsChoiceRow(
        label = stringResource(labelRes),
        options = options,
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
    )
}

private val SettingsPinnedReorderStepHeight = 48.dp

@Composable
private fun SettingsPinnedItemRow(
    item: NotesPinnedItem,
    label: String,
    onRemove: () -> Unit,
    onReorderBySteps: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val reorderStepPx = with(density) { SettingsPinnedReorderStepHeight.toPx() }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

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
                NotesFolderGlyph(size = 20.dp)
            }

            NotesPinnedKind.Note -> {
                NotesNoteGlyph(icon = item.icon, size = 20.dp)
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
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

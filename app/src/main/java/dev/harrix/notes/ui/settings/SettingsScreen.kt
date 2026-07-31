package dev.harrix.notes.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.harrix.notes.NotesBrowseLayout
import dev.harrix.notes.NotesListDensity
import dev.harrix.notes.NotesTitleSource
import dev.harrix.notes.NotesViewerPreferences
import dev.harrix.notes.R
import dev.harrix.notes.ui.notes.NotesFolderPathControls
import dev.harrix.notes.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = MaterialTheme.colorScheme.background

    BackHandler(onBack = onClose)

    Scaffold(
        modifier = modifier,
        containerColor = background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
                colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = background,
                    scrolledContainerColor = background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppearanceSettingsSection(
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            NotesSettingsSection(showSectionTitle = true)
        }
    }
}

@Composable
private fun CollapsibleSettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = true,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
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
    var treeUri by remember { mutableStateOf(preferences.loadNotesTreeUri()) }
    var browseLayout by remember { mutableStateOf(preferences.loadBrowseLayout()) }
    var listDensity by remember { mutableStateOf(preferences.loadListDensity()) }
    var titleSource by remember { mutableStateOf(preferences.loadTitleSource()) }
    var maxOpenTabsText by remember {
        mutableStateOf(preferences.loadMaxOpenTabs().toString())
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

    val body: @Composable () -> Unit = {
        NotesFolderPathControls(
            treeUri = treeUri,
            onTreeUriChange = { treeUri = it },
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
                    Text(stringResource(labelRes))
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
                    Text(stringResource(labelRes))
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
                    Text(stringResource(labelRes))
                }
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

@Composable
private fun AppearanceSettingsSection(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options =
        listOf(
            ThemeMode.System to R.string.settings_theme_system,
            ThemeMode.Light to R.string.settings_theme_light,
            ThemeMode.Dark to R.string.settings_theme_dark,
        )

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
                    Text(stringResource(labelRes))
                }
            }
        }
    }
}

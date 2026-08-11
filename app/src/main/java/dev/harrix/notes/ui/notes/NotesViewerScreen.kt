package dev.harrix.notes.ui.notes

import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.harrix.notes.NoteMetaUpdates
import dev.harrix.notes.NoteTitleExtractor
import dev.harrix.notes.NotesBrowseLayout
import dev.harrix.notes.NotesClipboardEntry
import dev.harrix.notes.NotesClipboardKind
import dev.harrix.notes.NotesClipboardMode
import dev.harrix.notes.NotesDateFormats
import dev.harrix.notes.NotesDocumentInfo
import dev.harrix.notes.NotesEntry
import dev.harrix.notes.NotesExternalNoteConflict
import dev.harrix.notes.NotesExternalNoteProbe
import dev.harrix.notes.NotesListDensity
import dev.harrix.notes.NotesListingOptions
import dev.harrix.notes.NotesLoadedDocumentBaseline
import dev.harrix.notes.NotesOpenIntent
import dev.harrix.notes.NotesOpenMode
import dev.harrix.notes.NotesPathSegment
import dev.harrix.notes.NotesPinnedItem
import dev.harrix.notes.NotesPinnedKind
import dev.harrix.notes.NotesSortBy
import dev.harrix.notes.NotesTitleSource
import dev.harrix.notes.NotesTreeRepository
import dev.harrix.notes.NotesViewerPreferences
import dev.harrix.notes.OpenNoteTab
import dev.harrix.notes.R
import dev.harrix.notes.blocksClipboardRelocation
import dev.harrix.notes.mutationDocument
import dev.harrix.notes.noteAssetFolderPath
import dev.harrix.notes.notesFolderDisplayName
import dev.harrix.notes.probeOpenNote
import dev.harrix.notes.takeNotesFolderPermission
import dev.harrix.notes.ui.adaptiveContentWidth
import dev.harrix.notes.ui.isCompactHeight
import dev.harrix.notes.ui.isDualPaneLayoutEligible
import dev.harrix.notes.ui.notesIconsGridColumnCount
import dev.harrix.notes.ui.theme.notesScaffoldContainerColor
import dev.harrix.notes.ui.theme.notesScaffoldContentWindowInsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

private val NotesIconsLabelMinFontSize = 9.sp
private val NotesIconsLabelMaxFontSize = 13.sp
private const val NotesIconsLabelMaxLines = 3
private const val NotesIconsLabelCompactMaxLines = 2
private const val NotesIconsLabelFontStepSp = 0.5f
private val TopBarLogoSize = 28.dp

/** Denser than Material default 48.dp so top-bar actions sit closer. */
private val TopBarActionButtonSize = 40.dp

@Composable
fun NotesViewerScreen(
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
    settingsRevision: Int = 0,
    pendingOpenUri: Uri? = null,
    onPendingOpenUriConsumed: () -> Unit = {},
    viewModel: NotesViewerViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = viewModel.preferences
    val repository = viewModel.repository
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var notesTreeUri by viewModel.notesTreeUri
    var menuExpanded by remember { mutableStateOf(false) }
    var browseContextMenuExpanded by remember { mutableStateOf(false) }
    var browseContextMenuOffset by remember { mutableStateOf(DpOffset.Zero) }
    var showCreateNoteDialog by remember { mutableStateOf(false) }
    var showNoteInfoDialog by remember { mutableStateOf(false) }
    var noteInfoDocument by remember { mutableStateOf<NotesDocumentInfo?>(null) }
    var pendingDeleteEntry by remember { mutableStateOf<NotesEntry?>(null) }
    var createNoteUntitledStem by remember { mutableStateOf("Untitled_01") }
    var folderPath by viewModel.folderPath
    var entries by viewModel.entries
    var isLoading by viewModel.isLoading
    var statusMessage by viewModel.statusMessage
    var openTabs by viewModel.openTabs
    var selectedTabDocumentId by viewModel.selectedTabDocumentId
    var sessionRestoredForTree by viewModel.sessionRestoredForTree
    var noteContent by viewModel.noteContent
    var noteLoading by viewModel.noteLoading
    var isEditing by viewModel.isEditing
    var canvasMarkdownMode by viewModel.canvasMarkdownMode
    var autoEditDocumentId by viewModel.autoEditDocumentId
    var draftText by viewModel.draftText
    var lastSavedText by viewModel.lastSavedText
    var isSaving by viewModel.isSaving
    var autosaveJob by viewModel.autosaveJob
    var folderListRequestId by viewModel.folderListRequestId
    var listDensity by viewModel.listDensity
    var treeDensity by viewModel.treeDensity
    var pinnedBarDensity by viewModel.pinnedBarDensity
    var browseLayout by viewModel.browseLayout
    var iconStyle by viewModel.iconStyle
    var titleSource by viewModel.titleSource
    var noteOpenMode by viewModel.noteOpenMode
    var previewFontSizeSp by viewModel.previewFontSizeSp
    var previewFont by viewModel.previewFont
    var editorFontSizeSp by viewModel.editorFontSizeSp
    var editorFont by viewModel.editorFont
    var highlightMaxMb by viewModel.highlightMaxMb
    var maxOpenTabs by viewModel.maxOpenTabs
    var singleNoteMode by viewModel.singleNoteMode
    var dualPaneEnabled by viewModel.dualPaneEnabled
    var showNoteDates by viewModel.showNoteDates
    var showNotePath by viewModel.showNotePath
    var previewDraftText by remember { mutableStateOf("") }
    var sortBy by viewModel.sortBy
    var foldersFirst by viewModel.foldersFirst
    var sortReverseOrder by viewModel.sortReverseOrder
    var showGmdFiles by viewModel.showGmdFiles
    var pinnedBarEnabled by viewModel.pinnedBarEnabled
    var maxPinnedItems by viewModel.maxPinnedItems
    var pinnedItems by viewModel.pinnedItems
    var notesClipboard by viewModel.notesClipboard
    var pinnedRestoredForTree by viewModel.pinnedRestoredForTree
    var treeRoot by viewModel.treeRoot
    var treeChildrenByFolderId by viewModel.treeChildrenByFolderId
    var treeExpandedFolderIds by viewModel.treeExpandedFolderIds
    var treeLoadingRoot by viewModel.treeLoadingRoot
    var externalNoteConflict by viewModel.externalNoteConflict
    val editorController = remember { NotesMarkdownEditorController() }
    val lifecycleOwner = LocalLifecycleOwner.current

    fun reloadPath() {
        notesTreeUri = preferences.loadNotesTreeUri()
        listDensity = preferences.loadListDensity()
        treeDensity = preferences.loadTreeDensity()
        pinnedBarDensity = preferences.loadPinnedBarDensity()
        browseLayout = preferences.loadBrowseLayout()
        iconStyle = preferences.loadIconStyle()
        titleSource = preferences.loadTitleSource()
        noteOpenMode = preferences.loadNoteOpenMode()
        previewFontSizeSp = preferences.loadPreviewFontSizeSp()
        previewFont = preferences.loadPreviewFont()
        editorFontSizeSp = preferences.loadEditorFontSizeSp()
        editorFont = preferences.loadEditorFont()
        highlightMaxMb = preferences.loadHighlightMaxMb()
        maxOpenTabs = preferences.loadMaxOpenTabs()
        singleNoteMode = preferences.loadSingleNoteMode()
        dualPaneEnabled = preferences.loadDualPaneEnabled()
        showNoteDates = preferences.loadShowNoteDates()
        showNotePath = preferences.loadShowNotePath()
        sortBy = preferences.loadSortBy()
        foldersFirst = preferences.loadFoldersFirst()
        sortReverseOrder = preferences.loadSortReverseOrder()
        showGmdFiles = preferences.loadShowGmdFiles()
        pinnedBarEnabled = preferences.loadPinnedBarEnabled()
        maxPinnedItems = preferences.loadMaxPinnedItems()
    }

    fun clearTreeState() {
        treeRoot = null
        treeChildrenByFolderId = emptyMap()
        treeExpandedFolderIds = emptySet()
        treeLoadingRoot = false
        notesClipboard = null
    }

    fun persistPinnedItems(items: List<NotesPinnedItem>) {
        val tree = notesTreeUri ?: return
        val limited = items.take(maxPinnedItems.coerceAtLeast(NotesViewerPreferences.MIN_PINNED_ITEMS))
        pinnedItems = limited
        preferences.savePinnedItems(tree, limited)
    }

    fun ensureMaxPinnedItems() {
        if (pinnedItems.size <= maxPinnedItems) {
            return
        }
        persistPinnedItems(pinnedItems.take(maxPinnedItems))
    }

    fun isPinned(documentId: String): Boolean = pinnedItems.any {
        it.id == documentId ||
            (it.kind != NotesPinnedKind.Home && it.documentId == documentId)
    }

    fun pinFolder(
        folder: NotesEntry.Folder,
        pathForFolder: List<NotesPathSegment>,
    ) {
        if (isPinned(folder.documentId)) {
            return
        }
        if (pinnedItems.size >= maxPinnedItems) {
            return
        }
        persistPinnedItems(
            pinnedItems +
                NotesPinnedItem(
                    id = folder.documentId,
                    kind = NotesPinnedKind.Folder,
                    documentId = folder.documentId,
                    uri = folder.uri,
                    title = folder.name,
                    folderPath = pathForFolder,
                ),
        )
    }

    fun pinNote(
        note: NotesEntry.Note,
        pathForNote: List<NotesPathSegment>,
    ) {
        if (isPinned(note.documentId)) {
            return
        }
        if (pinnedItems.size >= maxPinnedItems) {
            return
        }
        persistPinnedItems(
            pinnedItems +
                NotesPinnedItem(
                    id = note.documentId,
                    kind = NotesPinnedKind.Note,
                    documentId = note.documentId,
                    uri = note.uri,
                    title = note.displayLabel,
                    icon = note.displayIcon,
                    fileName = note.name,
                    folderPath = pathForNote,
                ),
        )
    }

    fun unpinItem(itemId: String) {
        persistPinnedItems(pinnedItems.filterNot { it.id == itemId })
    }

    fun unpinByDocumentId(documentId: String) {
        persistPinnedItems(
            pinnedItems.filterNot {
                it.id == documentId ||
                    (it.kind != NotesPinnedKind.Home && it.documentId == documentId)
            },
        )
    }

    fun putTreeChildren(
        dirDocumentId: String,
        children: List<NotesEntry>,
    ) {
        treeChildrenByFolderId = treeChildrenByFolderId + (dirDocumentId to children)
    }

    fun applyNoteMetaUpdates(
        dirDocumentId: String,
        updates: NoteMetaUpdates,
    ) {
        if (updates.isEmpty) {
            return
        }
        putTreeChildren(
            dirDocumentId,
            repository.withUpdatedNoteMeta(
                treeChildrenByFolderId[dirDocumentId].orEmpty(),
                titles = updates.titles,
                icons = updates.icons,
            ),
        )
    }

    fun loadTreeFolder(
        treeUri: Uri,
        dir: NotesPathSegment,
        onLoaded: ((List<NotesEntry>) -> Unit)? = null,
    ) {
        val cached = repository.peekListing(treeUri, dir.documentId)
        if (cached != null) {
            val withTitles = repository.applyTitleSource(cached, titleSource)
            putTreeChildren(dir.documentId, withTitles)
            onLoaded?.invoke(withTitles)
            scope.launch {
                applyNoteMetaUpdates(
                    dir.documentId,
                    repository.resolveMissingNoteMeta(
                        withTitles.filterIsInstance<NotesEntry.Note>(),
                        applyTitles = titleSource == NotesTitleSource.Content,
                    ),
                )
            }
            return
        }
        scope.launch {
            val listed =
                runCatching {
                    repository.listChildren(treeUri, dir.documentId, dir.name)
                }.getOrNull()
            if (listed == null) {
                onLoaded?.invoke(emptyList())
                return@launch
            }
            val withTitles = repository.applyTitleSource(listed, titleSource)
            putTreeChildren(dir.documentId, withTitles)
            onLoaded?.invoke(withTitles)
            applyNoteMetaUpdates(
                dir.documentId,
                repository.resolveMissingNoteMeta(
                    withTitles.filterIsInstance<NotesEntry.Note>(),
                    applyTitles = titleSource == NotesTitleSource.Content,
                ),
            )
        }
    }

    fun ensureTreeRootLoaded() {
        val tree = notesTreeUri ?: run {
            clearTreeState()
            return
        }
        val treeUri = Uri.parse(tree)
        val root = repository.rootSegment(treeUri)
        treeRoot = root
        if (treeChildrenByFolderId.containsKey(root.documentId)) {
            return
        }
        treeLoadingRoot = true
        loadTreeFolder(treeUri, root) {
            treeLoadingRoot = false
        }
    }

    fun expandPathToNote(tab: OpenNoteTab) {
        if (tab.isExternal) {
            viewModel.treeExpandedForTabId = tab.documentId
            return
        }
        val tree = notesTreeUri ?: return
        val treeUri = Uri.parse(tree)
        val root = treeRoot ?: repository.rootSegment(treeUri).also { treeRoot = it }
        treeExpandedFolderIds = tab.folderPath.map { it.documentId }.toSet()
        viewModel.treeExpandedForTabId = tab.documentId
        val segmentsToLoad =
            if (tab.folderPath.isEmpty()) {
                listOf(root)
            } else {
                tab.folderPath
            }
        segmentsToLoad.forEach { segment ->
            loadTreeFolder(treeUri, segment)
        }
        // Also ensure root is loaded when path starts deeper.
        if (tab.folderPath.isNotEmpty()) {
            loadTreeFolder(treeUri, root)
        }
    }

    fun ensurePathFoldersLoaded(tab: OpenNoteTab) {
        if (tab.isExternal) {
            return
        }
        val tree = notesTreeUri ?: return
        val treeUri = Uri.parse(tree)
        val root = treeRoot ?: repository.rootSegment(treeUri).also { treeRoot = it }
        val segmentsToLoad =
            if (tab.folderPath.isEmpty()) {
                listOf(root)
            } else {
                tab.folderPath
            }
        segmentsToLoad.forEach { segment ->
            loadTreeFolder(treeUri, segment)
        }
        if (tab.folderPath.isNotEmpty()) {
            loadTreeFolder(treeUri, root)
        }
    }

    fun toggleTreeFolder(folder: NotesEntry.Folder) {
        val tree = notesTreeUri ?: return
        val treeUri = Uri.parse(tree)
        if (folder.documentId in treeExpandedFolderIds) {
            treeExpandedFolderIds = treeExpandedFolderIds - folder.documentId
            return
        }
        treeExpandedFolderIds = treeExpandedFolderIds + folder.documentId
        loadTreeFolder(
            treeUri,
            NotesPathSegment(
                documentId = folder.documentId,
                name = folder.name,
                uri = folder.uri,
            ),
        )
    }

    fun resetEditorState() {
        isEditing = false
        canvasMarkdownMode = false
        draftText = ""
        lastSavedText = null
        autosaveJob?.cancel()
        autosaveJob = null
        viewModel.clearLoadedNote()
    }

    suspend fun saveNoteText(
        uri: Uri,
        text: String,
    ): Boolean {
        isSaving = true
        val result =
            withContext(Dispatchers.IO) {
                runCatching { repository.writeText(uri, text) }
            }
        isSaving = false
        result
            .onSuccess {
                lastSavedText = text
                noteContent = text
                statusMessage = null
                val tab = openTabs.firstOrNull { it.documentId == selectedTabDocumentId }
                if (tab != null) {
                    val (contentTitle, contentIcon) =
                        repository.rememberMetaFromContent(tab.documentId, text)
                    val fileStem =
                        tab.fileName
                            .takeIf { it.isNotBlank() }
                            ?.let { NotesTreeRepository.noteDisplayLabel(it) }
                            ?: entries
                                .filterIsInstance<NotesEntry.Note>()
                                .firstOrNull { note -> note.documentId == tab.documentId }
                                ?.let { note -> NotesTreeRepository.noteDisplayLabel(note.name) }
                    val label =
                        when (titleSource) {
                            NotesTitleSource.FileName -> fileStem ?: tab.title
                            NotesTitleSource.Content -> contentTitle ?: fileStem ?: tab.title
                        }
                    openTabs =
                        openTabs.map { openTab ->
                            if (openTab.documentId == tab.documentId) {
                                openTab.copy(title = label)
                            } else {
                                openTab
                            }
                        }
                    val titles =
                        if (titleSource == NotesTitleSource.Content || fileStem != null) {
                            mapOf(tab.documentId to label)
                        } else {
                            emptyMap()
                        }
                    val icons = mapOf(tab.documentId to contentIcon)
                    entries =
                        repository.withUpdatedNoteMeta(
                            entries,
                            titles = titles,
                            icons = icons,
                        )
                    notesTreeUri?.let { tree ->
                        folderPath.lastOrNull()?.let { dir ->
                            repository.patchListingNoteMeta(
                                Uri.parse(tree),
                                dir.documentId,
                                titles = titles,
                                icons = icons,
                            )
                            applyNoteMetaUpdates(
                                dir.documentId,
                                NoteMetaUpdates(titles = titles, icons = icons),
                            )
                        }
                    }
                }
            }.onFailure { error ->
                statusMessage =
                    error.message ?: context.getString(R.string.markdown_notes_save_failed)
            }
        if (result.isSuccess) {
            val info =
                withContext(Dispatchers.IO) {
                    repository.queryDocumentInfo(uri)
                }
            val uriString = uri.toString()
            val savedDocumentId =
                openTabs.firstOrNull { it.uri == uri }?.documentId
                    ?: selectedTabDocumentId
            // Only refresh the baseline for the note currently shown in the editor.
            if (savedDocumentId != null &&
                viewModel.loadedNoteDocumentId == savedDocumentId &&
                viewModel.loadedNoteUri == uriString
            ) {
                viewModel.loadedNoteBaseline =
                    NotesLoadedDocumentBaseline(
                        documentId = savedDocumentId,
                        uri = uriString,
                        lastModifiedEpochMs = info?.lastModifiedEpochMs,
                        sizeBytes = info?.sizeBytes,
                    )
            }
            viewModel.suppressExternalProbeUntilElapsedMs =
                android.os.SystemClock.elapsedRealtime() + ExternalProbeQuietPeriodMs
        }
        return result.isSuccess
    }

    fun persistCurrentDraft(after: (() -> Unit)? = null) {
        val tab = openTabs.firstOrNull { it.documentId == selectedTabDocumentId }
        if (tab == null || !isEditing) {
            after?.invoke()
            return
        }
        scope.launch {
            // The editor owns the text, so pull the newest edits before saving.
            editorController.flush()
            if (draftText != lastSavedText) {
                saveNoteText(tab.uri, draftText)
            }
            after?.invoke()
        }
    }

    fun scheduleAutosave() {
        val tab = openTabs.firstOrNull { it.documentId == selectedTabDocumentId } ?: return
        if (externalNoteConflict != null || noteLoading || isSaving) {
            return
        }
        if (!isEditing || draftText == lastSavedText) {
            return
        }
        autosaveJob?.cancel()
        autosaveJob =
            scope.launch {
                delay(AutosaveDelayMs)
                val canSave =
                    externalNoteConflict == null &&
                        !noteLoading &&
                        !isSaving &&
                        isEditing
                if (canSave &&
                    draftText != lastSavedText &&
                    selectedTabDocumentId == tab.documentId
                ) {
                    saveNoteText(tab.uri, draftText)
                }
            }
    }

    fun prefetchChildFolders(
        treeUri: Uri,
        listed: List<NotesEntry>,
    ) {
        listed.filterIsInstance<NotesEntry.Folder>().forEach { folder ->
            scope.launch {
                repository.prefetchDirectory(treeUri, folder.documentId, folder.name)
            }
        }
    }

    fun enrichNoteMeta(
        treeUri: Uri,
        dirDocumentId: String,
        listed: List<NotesEntry>,
        requestId: Int,
    ) {
        val notes = listed.filterIsInstance<NotesEntry.Note>()
        if (notes.isEmpty()) {
            return
        }
        scope.launch {
            val updates =
                repository.resolveMissingNoteMeta(
                    notes,
                    applyTitles = titleSource == NotesTitleSource.Content,
                )
            if (updates.isEmpty || requestId != folderListRequestId) {
                return@launch
            }
            if (folderPath.lastOrNull()?.documentId != dirDocumentId) {
                repository.patchListingNoteMeta(
                    treeUri,
                    dirDocumentId,
                    titles = updates.titles,
                    icons = updates.icons,
                )
                applyNoteMetaUpdates(dirDocumentId, updates)
                return@launch
            }
            entries =
                repository.withUpdatedNoteMeta(
                    entries,
                    titles = updates.titles,
                    icons = updates.icons,
                )
            putTreeChildren(
                dirDocumentId,
                repository.withUpdatedNoteMeta(
                    treeChildrenByFolderId[dirDocumentId] ?: entries,
                    titles = updates.titles,
                    icons = updates.icons,
                ),
            )
            repository.patchListingNoteMeta(
                treeUri,
                dirDocumentId,
                titles = updates.titles,
                icons = updates.icons,
            )
            if (updates.titles.isNotEmpty()) {
                openTabs =
                    openTabs.map { tab ->
                        val label = updates.titles[tab.documentId]
                        if (label != null) {
                            tab.copy(title = label)
                        } else {
                            tab
                        }
                    }
            }
            if (updates.titles.isNotEmpty() || updates.icons.isNotEmpty()) {
                val updatedPins =
                    pinnedItems.map { item ->
                        if (item.kind != NotesPinnedKind.Note) {
                            return@map item
                        }
                        val title = updates.titles[item.documentId]
                        val icon = updates.icons[item.documentId]
                        if (title == null && icon == null) {
                            item
                        } else {
                            item.copy(
                                title = title ?: item.title,
                                icon = icon ?: item.icon,
                            )
                        }
                    }
                if (updatedPins != pinnedItems) {
                    persistPinnedItems(updatedPins)
                }
            }
        }
    }

    fun applyTitleSourceToVisibleLists() {
        val dir = folderPath.lastOrNull()
        if (dir != null && entries.isNotEmpty()) {
            val updated = repository.applyTitleSource(entries, titleSource)
            entries = updated
            putTreeChildren(dir.documentId, updated)
            notesTreeUri?.let { tree ->
                enrichNoteMeta(Uri.parse(tree), dir.documentId, updated, folderListRequestId)
            }
        }
        treeChildrenByFolderId =
            treeChildrenByFolderId.mapValues { (_, children) ->
                repository.applyTitleSource(children, titleSource)
            }
        openTabs =
            openTabs.map { tab ->
                tab.copy(title = repository.displayTitleFor(tab, titleSource))
            }
    }

    fun openFolderList(
        path: List<NotesPathSegment>,
        clearSelection: Boolean = true,
    ) {
        val tree = notesTreeUri ?: return
        val treeUri = Uri.parse(tree)
        val current = path.lastOrNull() ?: return
        persistCurrentDraft {
            statusMessage = null
            if (clearSelection) {
                selectedTabDocumentId = null
                noteContent = null
                resetEditorState()
                viewModel.treeExpandedForTabId = null
            }
            val pathChanged =
                folderPath.map { it.documentId } != path.map { it.documentId }
            if (pathChanged) {
                viewModel.resetFolderScroll()
            }

            folderListRequestId += 1
            val requestId = folderListRequestId

            val cached = repository.peekListing(treeUri, current.documentId)
            if (cached != null) {
                val withTitles = repository.applyTitleSource(cached, titleSource)
                folderPath = path
                entries = withTitles
                putTreeChildren(current.documentId, withTitles)
                isLoading = false
                prefetchChildFolders(treeUri, withTitles)
                enrichNoteMeta(treeUri, current.documentId, withTitles, requestId)
                return@persistCurrentDraft
            }

            isLoading = true
            scope.launch {
                val shallow =
                    runCatching {
                        repository.listChildrenShallow(treeUri, current.documentId, current.name)
                    }.getOrNull()
                if (requestId == folderListRequestId && shallow != null) {
                    folderPath = path
                    entries = repository.applyTitleSource(shallow, titleSource)
                    putTreeChildren(current.documentId, entries)
                    isLoading = false
                    enrichNoteMeta(treeUri, current.documentId, entries, requestId)
                }

                val result =
                    runCatching {
                        repository.listChildren(treeUri, current.documentId, current.name)
                    }
                if (requestId != folderListRequestId) {
                    return@launch
                }
                result
                    .onSuccess { listed ->
                        val withTitles = repository.applyTitleSource(listed, titleSource)
                        folderPath = path
                        entries = withTitles
                        putTreeChildren(current.documentId, withTitles)
                        prefetchChildFolders(treeUri, withTitles)
                        enrichNoteMeta(treeUri, current.documentId, withTitles, requestId)
                    }.onFailure { error ->
                        if (shallow == null) {
                            statusMessage =
                                error.message
                                    ?: context.getString(R.string.markdown_notes_load_failed)
                            entries = emptyList()
                        }
                    }
                isLoading = false
            }
        }
    }

    fun ensureRootPath(): List<NotesPathSegment>? {
        val tree = notesTreeUri ?: return null
        val treeUri = Uri.parse(tree)
        return listOf(repository.rootSegment(treeUri))
    }

    fun ensureMaxOpenTabs(
        preferredSelectedId: String? = selectedTabDocumentId,
    ) {
        val limit =
            if (singleNoteMode) {
                1
            } else {
                maxOpenTabs.coerceAtLeast(NotesViewerPreferences.MIN_OPEN_TABS)
            }
        if (openTabs.size > limit) {
            openTabs =
                if (singleNoteMode) {
                    val preferred =
                        preferredSelectedId?.let { id ->
                            openTabs.firstOrNull { it.documentId == id }
                        }
                    listOfNotNull(preferred ?: openTabs.lastOrNull())
                } else {
                    // Drop oldest tabs first (left side of the tab bar).
                    openTabs.takeLast(limit)
                }
        }
        val kept = openTabs
        if (preferredSelectedId != null && kept.any { it.documentId == preferredSelectedId }) {
            selectedTabDocumentId = preferredSelectedId
        } else if (selectedTabDocumentId != null &&
            kept.none { it.documentId == selectedTabDocumentId }
        ) {
            selectedTabDocumentId = kept.lastOrNull()?.documentId
        } else if (preferredSelectedId != null && kept.none { it.documentId == preferredSelectedId }) {
            selectedTabDocumentId = kept.lastOrNull()?.documentId
        }
    }

    fun appendOpenTab(tab: OpenNoteTab) {
        val previousIds = openTabs.map { it.documentId }.toSet()
        openTabs =
            if (singleNoteMode) {
                listOf(tab)
            } else {
                openTabs + tab
            }
        ensureMaxOpenTabs(preferredSelectedId = tab.documentId)
        val closedPrevious =
            previousIds.any { id ->
                openTabs.none { it.documentId == id }
            }
        if (closedPrevious) {
            Toast
                .makeText(
                    context,
                    R.string.markdown_notes_max_open_tabs_warning,
                    Toast.LENGTH_LONG,
                ).show()
        }
    }

    fun openNote(
        note: NotesEntry.Note,
        pathForNote: List<NotesPathSegment>,
    ) {
        fun applyOpen() {
            val existing = openTabs.firstOrNull { it.documentId == note.documentId }
            if (existing == null) {
                appendOpenTab(
                    OpenNoteTab(
                        documentId = note.documentId,
                        uri = note.uri,
                        title = note.displayLabel,
                        fileName = note.name,
                        folderPath = pathForNote,
                        isExternal = false,
                    ),
                )
            } else {
                if (existing.folderPath.map { it.documentId } != pathForNote.map { it.documentId }) {
                    // Refresh path (e.g. collapsed Folder/Folder.md previously opened with parent path).
                    openTabs =
                        openTabs.map { tab ->
                            if (tab.documentId == note.documentId) {
                                tab.copy(folderPath = pathForNote, isExternal = false)
                            } else {
                                tab
                            }
                        }
                }
                if (singleNoteMode && openTabs.size > 1) {
                    openTabs = listOf(openTabs.first { it.documentId == note.documentId })
                }
            }
            if (selectedTabDocumentId != note.documentId) {
                viewModel.externalProbeGeneration += 1
                externalNoteConflict = null
                noteLoading = true
                noteContent = null
                resetEditorState()
            }
            selectedTabDocumentId = note.documentId
        }

        val closesOthers =
            singleNoteMode && openTabs.any { it.documentId != note.documentId }
        if (closesOthers || selectedTabDocumentId != note.documentId) {
            persistCurrentDraft { applyOpen() }
        } else {
            applyOpen()
        }
    }

    fun openTabFromIntent(uri: Uri) {
        scope.launch {
            val tab =
                withContext(Dispatchers.IO) {
                    NotesOpenIntent.resolveTab(
                        context = context,
                        treeUriString = notesTreeUri,
                        fileUri = uri,
                    )
                }

            fun applyOpen() {
                val existing =
                    openTabs.firstOrNull { openTab ->
                        openTab.documentId == tab.documentId || openTab.uri == tab.uri
                    }
                if (existing == null) {
                    appendOpenTab(tab)
                    if (selectedTabDocumentId != tab.documentId) {
                        noteLoading = true
                        noteContent = null
                        resetEditorState()
                    }
                    selectedTabDocumentId = tab.documentId
                } else {
                    val merged =
                        existing.copy(
                            uri = tab.uri,
                            title = tab.title.ifBlank { existing.title },
                            fileName = tab.fileName.ifBlank { existing.fileName },
                            folderPath =
                            if (tab.folderPath.isNotEmpty()) {
                                tab.folderPath
                            } else {
                                existing.folderPath
                            },
                            isExternal = tab.isExternal,
                        )
                    openTabs =
                        openTabs.map { openTab ->
                            if (openTab.documentId == existing.documentId) {
                                merged
                            } else {
                                openTab
                            }
                        }
                    if (singleNoteMode && openTabs.size > 1) {
                        openTabs = listOf(merged)
                    }
                    if (selectedTabDocumentId != merged.documentId) {
                        noteLoading = true
                        noteContent = null
                        resetEditorState()
                    }
                    selectedTabDocumentId = merged.documentId
                }
                if (!tab.isExternal && tab.folderPath.isNotEmpty()) {
                    openFolderList(tab.folderPath, clearSelection = false)
                }
            }

            val closesOthers =
                singleNoteMode &&
                    openTabs.any { openTab ->
                        openTab.documentId != tab.documentId && openTab.uri != tab.uri
                    }
            if (closesOthers ||
                selectedTabDocumentId == null ||
                openTabs.none { it.documentId == tab.documentId || it.uri == tab.uri }
            ) {
                persistCurrentDraft { applyOpen() }
            } else {
                applyOpen()
            }
        }
    }

    fun createNewNote(
        fileStem: String,
        noteTitle: String,
        beginningTemplateId: String,
        isCanvas: Boolean = false,
    ) {
        val tree = notesTreeUri ?: return
        val treeUri = Uri.parse(tree)
        val currentTab = openTabs.firstOrNull { it.documentId == selectedTabDocumentId }
        val path =
            when {
                currentTab != null ->
                    currentTab.folderPath.ifEmpty { ensureRootPath() ?: return }

                folderPath.isNotEmpty() -> folderPath

                else -> ensureRootPath() ?: return
            }
        val dir = path.lastOrNull() ?: return
        // @hsk-sync:new-note
        val beginningTemplate = preferences.resolveBeginningTemplate(beginningTemplateId)
        val personalData = preferences.loadPersonalData()
        scope.launch {
            val note =
                withContext(Dispatchers.IO) {
                    runCatching {
                        repository.createMarkdownNote(
                            treeUri = treeUri,
                            parentDocumentId = dir.documentId,
                            fileStem = fileStem,
                            noteTitle = noteTitle,
                            beginningTemplate = beginningTemplate,
                            personalData = personalData,
                            isCanvas = isCanvas,
                            canvasPaper = preferences.loadCanvasPaperMode(),
                        )
                    }
                }.getOrElse { error ->
                    statusMessage =
                        error.message
                            ?: context.getString(R.string.markdown_notes_create_failed)
                    return@launch
                }
            val listed =
                withContext(Dispatchers.IO) {
                    runCatching {
                        repository.listChildren(treeUri, dir.documentId, dir.name)
                    }.getOrNull()
                }
            if (listed != null && folderPath.lastOrNull()?.documentId == dir.documentId) {
                val withTitles = repository.applyTitleSource(listed, titleSource)
                entries = withTitles
                putTreeChildren(dir.documentId, withTitles)
                enrichNoteMeta(treeUri, dir.documentId, withTitles, folderListRequestId)
            } else if (listed != null) {
                putTreeChildren(
                    dir.documentId,
                    repository.applyTitleSource(listed, titleSource),
                )
            }
            canvasMarkdownMode = false
            if (isCanvas) {
                autoEditDocumentId = null
            } else {
                autoEditDocumentId = note.documentId
            }
            openNote(note, noteAssetFolderPath(path, note))
        }
    }

    fun requestCreateNewNote() {
        if (notesTreeUri == null) {
            return
        }
        val tree = notesTreeUri ?: return
        val treeUri = Uri.parse(tree)
        val currentTab = openTabs.firstOrNull { it.documentId == selectedTabDocumentId }
        val path =
            when {
                currentTab != null ->
                    currentTab.folderPath.ifEmpty { ensureRootPath() ?: return }

                folderPath.isNotEmpty() -> folderPath

                else -> ensureRootPath() ?: return
            }
        val dir = path.lastOrNull() ?: return
        scope.launch {
            val stem =
                withContext(Dispatchers.IO) {
                    val existing = repository.childNamesLowercase(treeUri, dir.documentId)
                    NotesTreeRepository.nextUntitledNumberedStem(existing)
                }
            createNoteUntitledStem = stem
            showCreateNoteDialog = true
        }
    }

    fun openPinnedItem(item: NotesPinnedItem) {
        val root = ensureRootPath()?.firstOrNull() ?: return
        when (item.kind) {
            NotesPinnedKind.Home -> {
                openFolderList(listOf(root))
            }

            NotesPinnedKind.Folder -> {
                val path = item.folderPath.ifEmpty { listOf(root) }
                openFolderList(path)
            }

            NotesPinnedKind.Note -> {
                openNote(
                    NotesEntry.Note(
                        documentId = item.documentId,
                        name = item.fileName.ifBlank { item.title },
                        uri = item.uri,
                        displayLabel = item.title,
                        displayIcon = item.icon,
                    ),
                    item.folderPath,
                )
            }
        }
    }

    fun openMergedNote(
        folder: NotesEntry.Folder,
        pathForFolder: List<NotesPathSegment>,
    ) {
        val uri = folder.mergedNoteUri ?: return
        val documentId = folder.mergedNoteDocumentId ?: return
        val title = "_${folder.name}.g"
        val fileName = "_${folder.name}.g.md"
        // Images/files live beside the merged note inside [folder], not in the parent listing.
        val noteParentPath =
            pathForFolder +
                NotesPathSegment(
                    documentId = folder.documentId,
                    name = folder.name,
                    uri = folder.uri,
                )

        fun applyOpen() {
            val existing = openTabs.firstOrNull { it.documentId == documentId }
            if (existing == null) {
                appendOpenTab(
                    OpenNoteTab(
                        documentId = documentId,
                        uri = uri,
                        title = title,
                        fileName = fileName,
                        folderPath = noteParentPath,
                    ),
                )
            } else if (singleNoteMode && openTabs.size > 1) {
                openTabs = listOf(existing)
            }
            if (selectedTabDocumentId != documentId) {
                noteLoading = true
                noteContent = null
                resetEditorState()
            }
            selectedTabDocumentId = documentId
        }

        val closesOthers =
            singleNoteMode && openTabs.any { it.documentId != documentId }
        if (closesOthers || selectedTabDocumentId != documentId) {
            persistCurrentDraft { applyOpen() }
        } else {
            applyOpen()
        }
    }

    fun closeTab(documentId: String) {
        val closingSelected = selectedTabDocumentId == documentId
        if (closingSelected) {
            persistCurrentDraft {
                openTabs = openTabs.filterNot { it.documentId == documentId }
                val nextId = openTabs.lastOrNull()?.documentId
                selectedTabDocumentId = nextId
                if (nextId == null) {
                    noteContent = null
                    noteLoading = false
                    resetEditorState()
                } else {
                    noteLoading = true
                    noteContent = null
                    resetEditorState()
                }
            }
        } else {
            openTabs = openTabs.filterNot { it.documentId == documentId }
        }
    }

    fun listingParentDocumentId(): String? = folderPath.lastOrNull()?.documentId

    fun clipboardFromEntry(
        entry: NotesEntry,
        mode: NotesClipboardMode,
    ) {
        if (entry.blocksClipboardRelocation()) {
            return
        }
        val tree = notesTreeUri ?: return
        val parentId = listingParentDocumentId() ?: return
        notesClipboard = entry.mutationDocument(parentId).toClipboardEntry(tree, mode)
    }

    fun reloadListingAfterMutation(parentDocumentId: String) {
        val tree = notesTreeUri ?: return
        val treeUri = Uri.parse(tree)
        val dir = folderPath.lastOrNull() ?: return
        repository.invalidateDirectory(treeUri, parentDocumentId)
        if (dir.documentId != parentDocumentId) {
            repository.invalidateDirectory(treeUri, dir.documentId)
        }
        scope.launch {
            val listed =
                withContext(Dispatchers.IO) {
                    runCatching {
                        repository.listChildren(treeUri, dir.documentId, dir.name)
                    }.getOrNull()
                } ?: return@launch
            val fingerprint =
                withContext(Dispatchers.IO) {
                    runCatching {
                        repository.directoryFingerprint(treeUri, dir.documentId)
                    }.getOrNull()
                }
            if (fingerprint != null) {
                viewModel.rememberDirectoryFingerprint(dir.documentId, fingerprint)
            }
            if (folderPath.lastOrNull()?.documentId != dir.documentId) {
                putTreeChildren(
                    dir.documentId,
                    repository.applyTitleSource(listed, titleSource),
                )
                return@launch
            }
            val withTitles = repository.applyTitleSource(listed, titleSource)
            entries = withTitles
            putTreeChildren(dir.documentId, withTitles)
            enrichNoteMeta(treeUri, dir.documentId, withTitles, folderListRequestId)
        }
    }

    fun closeTabWithoutSaving(documentId: String) {
        autosaveJob?.cancel()
        autosaveJob = null
        val closingSelected = selectedTabDocumentId == documentId
        openTabs = openTabs.filterNot { it.documentId == documentId }
        if (!closingSelected) {
            return
        }
        val nextId = openTabs.lastOrNull()?.documentId
        selectedTabDocumentId = nextId
        if (nextId == null) {
            noteContent = null
            noteLoading = false
            resetEditorState()
        } else {
            noteLoading = true
            noteContent = null
            resetEditorState()
        }
    }

    fun reloadSelectedNoteFromDisk() {
        autosaveJob?.cancel()
        autosaveJob = null
        externalNoteConflict = null
        isEditing = false
        viewModel.clearLoadedNote()
        // Keep draft until disk load finishes so the editor cannot autosave empty text.
        noteLoading = true
        noteContent = null
    }

    fun keepLocalAgainstExternal(conflict: NotesExternalNoteConflict.Modified) {
        if (selectedTabDocumentId != conflict.tab.documentId) {
            externalNoteConflict = null
            return
        }
        viewModel.loadedNoteBaseline =
            NotesLoadedDocumentBaseline(
                documentId = conflict.tab.documentId,
                uri = conflict.tab.uri.toString(),
                lastModifiedEpochMs = conflict.diskLastModifiedEpochMs,
                sizeBytes = conflict.diskSizeBytes,
            )
        externalNoteConflict = null
    }

    suspend fun refreshDirectoryFromExternal(
        treeUri: Uri,
        dirDocumentId: String,
    ) {
        repository.invalidateDirectory(treeUri, dirDocumentId)
        val currentDir = folderPath.lastOrNull()
        if (currentDir?.documentId == dirDocumentId) {
            val listed =
                withContext(Dispatchers.IO) {
                    runCatching {
                        repository.listChildren(treeUri, currentDir.documentId, currentDir.name)
                    }.getOrNull()
                } ?: return
            if (folderPath.lastOrNull()?.documentId != currentDir.documentId) {
                return
            }
            val withTitles = repository.applyTitleSource(listed, titleSource)
            entries = withTitles
            putTreeChildren(currentDir.documentId, withTitles)
            enrichNoteMeta(treeUri, currentDir.documentId, withTitles, folderListRequestId)
            return
        }
        val name =
            treeChildrenByFolderId.values
                .asSequence()
                .flatten()
                .filterIsInstance<NotesEntry.Folder>()
                .firstOrNull { it.documentId == dirDocumentId }
                ?.name
                ?: treeRoot?.takeIf { it.documentId == dirDocumentId }?.name
                ?: folderPath.firstOrNull { it.documentId == dirDocumentId }?.name
                ?: return
        val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, dirDocumentId)
        loadTreeFolder(
            treeUri,
            NotesPathSegment(documentId = dirDocumentId, name = name, uri = uri),
        )
    }

    suspend fun syncExternalChanges() {
        if (externalNoteConflict != null || isSaving || noteLoading) {
            return
        }
        if (android.os.SystemClock.elapsedRealtime() < viewModel.suppressExternalProbeUntilElapsedMs) {
            return
        }
        val tree = notesTreeUri ?: return
        val treeUri = Uri.parse(tree)
        val dirsToCheck =
            buildSet {
                folderPath.lastOrNull()?.documentId?.let { add(it) }
                treeRoot?.documentId?.let { add(it) }
                addAll(treeExpandedFolderIds)
            }
        for (dirId in dirsToCheck) {
            if (isSaving || noteLoading || externalNoteConflict != null) {
                return
            }
            val fingerprint =
                withContext(Dispatchers.IO) {
                    runCatching { repository.directoryFingerprint(treeUri, dirId) }.getOrNull()
                } ?: continue
            val previous = viewModel.directoryFingerprints.value[dirId]
            viewModel.rememberDirectoryFingerprint(dirId, fingerprint)
            if (previous != null && previous != fingerprint) {
                refreshDirectoryFromExternal(treeUri, dirId)
            }
        }

        val selectedId = selectedTabDocumentId ?: return
        val tab = openTabs.firstOrNull { it.documentId == selectedId } ?: return
        if (noteLoading || isSaving) {
            return
        }
        if (viewModel.loadedNoteDocumentId != tab.documentId ||
            viewModel.loadedNoteUri != tab.uri.toString()
        ) {
            return
        }
        val knownTexts = listOfNotNull(lastSavedText, draftText).distinct()
        val probeGeneration = viewModel.externalProbeGeneration
        val probe =
            withContext(Dispatchers.IO) {
                repository.probeOpenNote(
                    uri = tab.uri,
                    documentId = tab.documentId,
                    baseline = viewModel.loadedNoteBaseline,
                    knownTexts = knownTexts,
                )
            }
        // Tab may have changed while the probe ran — never alert for a non-selected note.
        val probeStillValid =
            probeGeneration == viewModel.externalProbeGeneration &&
                selectedTabDocumentId == tab.documentId &&
                !noteLoading &&
                !isSaving &&
                externalNoteConflict == null
        if (!probeStillValid) {
            return
        }
        when (probe) {
            is NotesExternalNoteProbe.Unchanged -> {
                viewModel.loadedNoteBaseline = probe.baseline
                val displayName = probe.displayName
                if (!displayName.isNullOrBlank() && displayName != tab.fileName) {
                    openTabs =
                        openTabs.map { openTab ->
                            if (openTab.documentId != tab.documentId) {
                                openTab
                            } else {
                                val nextTitle =
                                    if (titleSource == NotesTitleSource.FileName) {
                                        NotesTreeRepository.noteDisplayLabel(displayName)
                                    } else {
                                        openTab.title
                                    }
                                openTab.copy(fileName = displayName, title = nextTitle)
                            }
                        }
                }
            }

            is NotesExternalNoteProbe.Modified -> {
                // Content already matches what we have — refresh baseline only.
                if (probe.diskText == draftText || probe.diskText == lastSavedText) {
                    viewModel.loadedNoteBaseline =
                        NotesLoadedDocumentBaseline(
                            documentId = tab.documentId,
                            uri = tab.uri.toString(),
                            lastModifiedEpochMs = probe.diskLastModifiedEpochMs,
                            sizeBytes = probe.diskSizeBytes,
                        )
                    return
                }
                autosaveJob?.cancel()
                autosaveJob = null
                externalNoteConflict =
                    NotesExternalNoteConflict.Modified(
                        tab = tab,
                        diskLastModifiedEpochMs = probe.diskLastModifiedEpochMs,
                        diskSizeBytes = probe.diskSizeBytes,
                        diskText = probe.diskText,
                    )
            }

            NotesExternalNoteProbe.Missing -> {
                autosaveJob?.cancel()
                autosaveJob = null
                externalNoteConflict = NotesExternalNoteConflict.Deleted(tab = tab)
            }
        }
    }

    fun saveDeletedOpenNote(conflict: NotesExternalNoteConflict.Deleted) {
        val tree = notesTreeUri
        if (tree == null) {
            statusMessage = context.getString(R.string.markdown_notes_save_failed)
            return
        }
        scope.launch {
            editorController.flush()
            val tab = conflict.tab
            val text = draftText
            val treeUri = Uri.parse(tree)
            val stem =
                tab.fileName
                    .substringBeforeLast('.')
                    .ifBlank { tab.title }
                    .ifBlank { "Untitled" }
            val parentIds =
                (
                    tab.folderPath.map { it.documentId }.asReversed() +
                        repository.rootSegment(treeUri).documentId
                    ).distinct()
            val result =
                withContext(Dispatchers.IO) {
                    runCatching {
                        val note =
                            repository.recreateMarkdownNoteWithContent(
                                treeUri = treeUri,
                                parentDocumentIds = parentIds,
                                fileStem = stem,
                                content = text,
                            )
                        val survivingPath =
                            tab.folderPath.filter { segment ->
                                repository.documentExists(segment.uri)
                            }
                        note to noteAssetFolderPath(survivingPath, note)
                    }
                }
            result
                .onSuccess { (note, pathForNote) ->
                    openTabs =
                        openTabs.map { openTab ->
                            if (openTab.documentId == tab.documentId) {
                                OpenNoteTab(
                                    documentId = note.documentId,
                                    uri = note.uri,
                                    title = note.displayLabel,
                                    fileName = note.name,
                                    folderPath = pathForNote,
                                    isExternal = false,
                                )
                            } else {
                                openTab
                            }
                        }
                    selectedTabDocumentId = note.documentId
                    noteContent = text
                    draftText = text
                    lastSavedText = text
                    previewDraftText = text
                    viewModel.markNoteLoaded(
                        note.documentId,
                        note.uri.toString(),
                        NotesLoadedDocumentBaseline(
                            documentId = note.documentId,
                            uri = note.uri.toString(),
                            lastModifiedEpochMs = note.lastModifiedEpochMs,
                            sizeBytes = note.sizeBytes,
                        ),
                    )
                    externalNoteConflict = null
                    statusMessage = null
                    pathForNote.lastOrNull()?.documentId?.let { parentId ->
                        reloadListingAfterMutation(parentId)
                    }
                }.onFailure { error ->
                    statusMessage =
                        error.message ?: context.getString(R.string.markdown_notes_save_failed)
                }
        }
    }

    fun closeTabsAffectedByDelete(
        entry: NotesEntry,
    ) {
        val removedIds =
            when (entry) {
                is NotesEntry.Note -> {
                    val containingId = entry.containingFolder?.documentId
                    openTabs
                        .filter { tab ->
                            tab.documentId == entry.documentId ||
                                (
                                    containingId != null &&
                                        (
                                            tab.documentId == containingId ||
                                                tab.folderPath.any { it.documentId == containingId }
                                            )
                                    )
                        }.map { it.documentId }
                        .toSet()
                }

                is NotesEntry.Folder ->
                    openTabs
                        .filter { tab ->
                            tab.documentId == entry.documentId ||
                                tab.folderPath.any { it.documentId == entry.documentId }
                        }.map { it.documentId }
                        .toSet()
            }
        if (removedIds.isEmpty()) {
            return
        }
        val closingSelected = selectedTabDocumentId in removedIds
        if (closingSelected) {
            persistCurrentDraft {
                openTabs = openTabs.filterNot { it.documentId in removedIds }
                val nextId = openTabs.lastOrNull()?.documentId
                selectedTabDocumentId = nextId
                if (nextId == null) {
                    noteContent = null
                    noteLoading = false
                    resetEditorState()
                } else {
                    noteLoading = true
                    noteContent = null
                    resetEditorState()
                }
            }
        } else {
            openTabs = openTabs.filterNot { it.documentId in removedIds }
        }
    }

    fun pasteClipboard() {
        val clip = notesClipboard ?: return
        val tree = notesTreeUri ?: return
        if (clip.treeUri != tree) {
            statusMessage = context.getString(R.string.markdown_notes_paste_failed)
            return
        }
        if (clip.kind == NotesClipboardKind.Note && NotesTreeRepository.isGMd(clip.displayName)) {
            notesClipboard = null
            return
        }
        val dest = folderPath.lastOrNull() ?: return
        val treeUri = Uri.parse(tree)
        scope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    runCatching {
                        val existing = repository.childNamesLowercase(treeUri, dest.documentId)
                        when (clip.mode) {
                            NotesClipboardMode.Copy -> {
                                val name =
                                    NotesTreeRepository.uniqueCopyDisplayName(
                                        clip.displayName,
                                        existing,
                                    )
                                val created =
                                    repository.copyEntryInto(
                                        treeUri = treeUri,
                                        sourceUri = clip.uri,
                                        sourceDocumentId = clip.documentId,
                                        isDirectory = clip.kind == NotesClipboardKind.Folder,
                                        destParentDocumentId = dest.documentId,
                                        desiredDisplayName = name,
                                    )
                                val stem = clip.folderPerNoteStem
                                if (stem != null && clip.kind == NotesClipboardKind.Folder) {
                                    val folderId = DocumentsContract.getDocumentId(created)
                                    val actualName =
                                        repository.queryDocumentInfo(created)?.displayName
                                            ?: name
                                    repository.syncFolderPerNoteInnerNames(
                                        treeUri = treeUri,
                                        folderDocumentId = folderId,
                                        oldStem = stem,
                                        newFolderName = actualName,
                                    )
                                }
                                created
                            }

                            NotesClipboardMode.Cut -> {
                                if (clip.sourceParentDocumentId == dest.documentId) {
                                    return@runCatching clip.uri
                                }
                                val nameLower = clip.displayName.lowercase(java.util.Locale.ROOT)
                                val name =
                                    if (nameLower in existing) {
                                        NotesTreeRepository.uniqueCopyDisplayName(
                                            clip.displayName,
                                            existing,
                                        )
                                    } else {
                                        clip.displayName
                                    }
                                val moved =
                                    repository.moveEntryInto(
                                        treeUri = treeUri,
                                        sourceUri = clip.uri,
                                        sourceDocumentId = clip.documentId,
                                        isDirectory = clip.kind == NotesClipboardKind.Folder,
                                        sourceParentDocumentId = clip.sourceParentDocumentId,
                                        destParentDocumentId = dest.documentId,
                                        desiredDisplayName = name,
                                    )
                                val stem = clip.folderPerNoteStem
                                if (stem != null && clip.kind == NotesClipboardKind.Folder) {
                                    val folderId = DocumentsContract.getDocumentId(moved)
                                    val actualName =
                                        repository.queryDocumentInfo(moved)?.displayName
                                            ?: name
                                    repository.syncFolderPerNoteInnerNames(
                                        treeUri = treeUri,
                                        folderDocumentId = folderId,
                                        oldStem = stem,
                                        newFolderName = actualName,
                                    )
                                }
                                moved
                            }
                        }
                    }
                }
            result
                .onSuccess {
                    if (clip.mode == NotesClipboardMode.Cut) {
                        unpinByDocumentId(clip.pinDocumentId)
                        if (clip.pinDocumentId != clip.documentId) {
                            unpinByDocumentId(clip.documentId)
                        }
                        notesClipboard = null
                        if (clip.sourceParentDocumentId != dest.documentId) {
                            repository.invalidateDirectory(treeUri, clip.sourceParentDocumentId)
                        }
                    }
                    reloadListingAfterMutation(dest.documentId)
                }.onFailure { error ->
                    statusMessage =
                        error.message
                            ?: context.getString(R.string.markdown_notes_paste_failed)
                }
        }
    }

    fun duplicateEntry(entry: NotesEntry) {
        if (entry.blocksClipboardRelocation()) {
            return
        }
        val tree = notesTreeUri ?: return
        val parentId = listingParentDocumentId() ?: return
        val treeUri = Uri.parse(tree)
        val target = entry.mutationDocument(parentId)
        scope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    runCatching {
                        val existing = repository.childNamesLowercase(treeUri, parentId)
                        val name =
                            NotesTreeRepository.uniqueCopyDisplayName(target.displayName, existing)
                        val created =
                            repository.copyEntryInto(
                                treeUri = treeUri,
                                sourceUri = target.uri,
                                sourceDocumentId = target.documentId,
                                isDirectory = target.isDirectory,
                                destParentDocumentId = parentId,
                                desiredDisplayName = name,
                            )
                        val stem = target.folderPerNoteStem
                        if (stem != null && target.isDirectory) {
                            val folderId = DocumentsContract.getDocumentId(created)
                            val actualName =
                                repository.queryDocumentInfo(created)?.displayName ?: name
                            repository.syncFolderPerNoteInnerNames(
                                treeUri = treeUri,
                                folderDocumentId = folderId,
                                oldStem = stem,
                                newFolderName = actualName,
                            )
                        }
                        created
                    }
                }
            result
                .onSuccess {
                    reloadListingAfterMutation(parentId)
                }.onFailure { error ->
                    statusMessage =
                        error.message
                            ?: context.getString(R.string.markdown_notes_copy_failed)
                }
        }
    }

    fun confirmDeleteEntry(entry: NotesEntry) {
        val tree = notesTreeUri ?: return
        val parentId = listingParentDocumentId() ?: return
        val treeUri = Uri.parse(tree)
        val target = entry.mutationDocument(parentId)
        pendingDeleteEntry = null
        scope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    runCatching {
                        repository.deleteEntry(
                            treeUri = treeUri,
                            documentUri = target.uri,
                            documentId = target.documentId,
                            isDirectory = target.isDirectory,
                            parentDocumentId = parentId,
                        )
                    }
                }
            result
                .onSuccess { deleted ->
                    if (!deleted) {
                        statusMessage = context.getString(R.string.markdown_notes_delete_failed)
                        return@onSuccess
                    }
                    closeTabsAffectedByDelete(entry)
                    unpinByDocumentId(target.pinDocumentId)
                    if (target.pinDocumentId != target.documentId) {
                        unpinByDocumentId(target.documentId)
                    }
                    val clip = notesClipboard
                    if (clip != null &&
                        (
                            clip.documentId == target.documentId ||
                                clip.pinDocumentId == target.pinDocumentId ||
                                clip.documentId == entry.documentId
                            )
                    ) {
                        notesClipboard = null
                    }
                    reloadListingAfterMutation(parentId)
                }.onFailure { error ->
                    statusMessage =
                        error.message
                            ?: context.getString(R.string.markdown_notes_delete_failed)
                }
        }
    }

    fun reorderTabs(
        fromIndex: Int,
        toIndex: Int,
    ) {
        openTabs = openTabs.moved(fromIndex, toIndex)
    }

    fun navigateBack(useDualPane: Boolean = false) {
        val canvasNote =
            NoteTitleExtractor.isCanvas(draftText.ifBlank { noteContent.orEmpty() })
        when {
            canvasNote && canvasMarkdownMode && isEditing && !useDualPane -> {
                persistCurrentDraft {
                    isEditing = false
                    draftText = noteContent.orEmpty()
                    lastSavedText = noteContent
                }
            }

            canvasNote && canvasMarkdownMode -> {
                canvasMarkdownMode = false
                isEditing = false
            }

            isEditing && !useDualPane -> {
                persistCurrentDraft {
                    isEditing = false
                    draftText = noteContent.orEmpty()
                    lastSavedText = noteContent
                }
            }

            selectedTabDocumentId != null -> {
                selectedTabDocumentId = null
                noteContent = null
                noteLoading = false
                resetEditorState()
            }

            folderPath.size > 1 -> {
                openFolderList(folderPath.dropLast(1))
            }

            else -> {
                onClose()
            }
        }
    }

    fun selectTab(documentId: String) {
        if (documentId == selectedTabDocumentId) {
            return
        }
        viewModel.externalProbeGeneration += 1
        externalNoteConflict = null
        persistCurrentDraft {
            selectedTabDocumentId = documentId
            noteLoading = true
            noteContent = null
            resetEditorState()
        }
    }

    val folderPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree(),
        ) { uri: Uri? ->
            if (uri == null) {
                return@rememberLauncherForActivityResult
            }
            takeNotesFolderPermission(context, uri)
            preferences.saveNotesTreeUri(uri.toString())
            repository.clearCache()
            reloadPath()
        }

    LaunchedEffect(settingsRevision) {
        if (viewModel.appliedSettingsRevision == settingsRevision) {
            return@LaunchedEffect
        }
        val previousTitleSource = titleSource
        val previousMaxOpenTabs = maxOpenTabs
        val previousSingleNoteMode = singleNoteMode
        val previousMaxPinned = maxPinnedItems
        val hadSession = viewModel.appliedSettingsRevision >= 0
        reloadPath()
        viewModel.appliedSettingsRevision = settingsRevision
        if (previousTitleSource != titleSource) {
            applyTitleSourceToVisibleLists()
        }
        if (previousMaxOpenTabs != maxOpenTabs ||
            previousSingleNoteMode != singleNoteMode
        ) {
            ensureMaxOpenTabs()
        }
        val tree = notesTreeUri
        val root = ensureRootPath()?.firstOrNull()
        if (tree != null && root != null) {
            if (hadSession || pinnedRestoredForTree != tree) {
                pinnedItems = preferences.loadPinnedItems(tree, root)
                pinnedRestoredForTree = tree
            }
            if (previousMaxPinned != maxPinnedItems) {
                ensureMaxPinnedItems()
            }
        } else if (hadSession) {
            pinnedItems = emptyList()
            pinnedRestoredForTree = null
        }
    }

    LaunchedEffect(notesTreeUri) {
        repository.prepareForTree(notesTreeUri)
        val treeUriValue = notesTreeUri
        val root = ensureRootPath()
        if (root != null && treeUriValue != null) {
            val alreadyRestored = sessionRestoredForTree == treeUriValue
            if (!alreadyRestored) {
                clearTreeState()
                viewModel.treeExpandedForTabId = null
                viewModel.clearLoadedNote()
                viewModel.resetFolderScroll()
                val session = preferences.loadOpenTabsSession(treeUriValue)
                openTabs =
                    session.tabs.map { tab ->
                        tab.copy(title = repository.displayTitleFor(tab, titleSource))
                    }
                ensureMaxOpenTabs(preferredSelectedId = session.selectedDocumentId)
                if (selectedTabDocumentId != null) {
                    noteLoading = true
                    noteContent = null
                    resetEditorState()
                } else {
                    noteContent = null
                    noteLoading = false
                    resetEditorState()
                }
                sessionRestoredForTree = treeUriValue
                val restoredTab = openTabs.firstOrNull { it.documentId == selectedTabDocumentId }
                if (restoredTab != null) {
                    val path = restoredTab.folderPath.ifEmpty { root }
                    openFolderList(path, clearSelection = false)
                } else {
                    openFolderList(root)
                }
            } else if (folderPath.isEmpty()) {
                openFolderList(root, clearSelection = false)
            } else if (entries.isEmpty()) {
                openFolderList(folderPath, clearSelection = false)
            }
            if (pinnedRestoredForTree != treeUriValue) {
                val loaded = preferences.loadPinnedItems(treeUriValue, root.first())
                // Persist default home on first use so empty vs missing can be distinguished later.
                if (preferences.loadPinnedItemsStore().itemsFor(treeUriValue) == null) {
                    preferences.savePinnedItems(treeUriValue, loaded)
                }
                pinnedItems = loaded
                pinnedRestoredForTree = treeUriValue
                scope.launch {
                    val missingIds =
                        withContext(Dispatchers.IO) {
                            loaded
                                .filter { item ->
                                    item.kind != NotesPinnedKind.Home &&
                                        !repository.documentExists(item.uri)
                                }.map { it.id }
                                .toSet()
                        }
                    if (missingIds.isNotEmpty() && pinnedRestoredForTree == treeUriValue) {
                        persistPinnedItems(pinnedItems.filterNot { it.id in missingIds })
                    }
                }
            }
            ensureTreeRootLoaded()
        } else {
            clearTreeState()
            sessionRestoredForTree = null
            pinnedRestoredForTree = null
            viewModel.treeExpandedForTabId = null
            viewModel.clearLoadedNote()
            viewModel.resetFolderScroll()
            folderPath = emptyList()
            entries = emptyList()
            pinnedItems = emptyList()
            val keptExternal = openTabs.filter { it.isExternal }
            openTabs = keptExternal
            selectedTabDocumentId =
                keptExternal
                    .firstOrNull { it.documentId == selectedTabDocumentId }
                    ?.documentId
                    ?: keptExternal.lastOrNull()?.documentId
            if (selectedTabDocumentId == null) {
                noteContent = null
                noteLoading = false
                resetEditorState()
            } else {
                noteLoading = true
                noteContent = null
                resetEditorState()
            }
            preferences.clearOpenTabsSession()
        }
    }

    LaunchedEffect(pendingOpenUri, notesTreeUri, sessionRestoredForTree) {
        val uri = pendingOpenUri ?: return@LaunchedEffect
        val tree = notesTreeUri
        if (tree != null && sessionRestoredForTree != tree) {
            // Wait until the notes-folder session is restored so we do not race it.
            return@LaunchedEffect
        }
        openTabFromIntent(uri)
        onPendingOpenUriConsumed()
    }

    LaunchedEffect(openTabs, selectedTabDocumentId, notesTreeUri, sessionRestoredForTree) {
        val tree = notesTreeUri
        if (tree == null) {
            return@LaunchedEffect
        }
        if (sessionRestoredForTree != tree) {
            return@LaunchedEffect
        }
        preferences.saveOpenTabsSession(
            treeUri = tree,
            tabs = openTabs,
            selectedDocumentId = selectedTabDocumentId,
        )
    }

    val selectedTab = openTabs.firstOrNull { it.documentId == selectedTabDocumentId }

    LaunchedEffect(selectedTabDocumentId, selectedTab?.folderPath) {
        val tab = selectedTab
        if (tab == null) {
            if (viewModel.treeExpandedForTabId != null) {
                treeExpandedFolderIds = emptySet()
                viewModel.treeExpandedForTabId = null
            }
            ensureTreeRootLoaded()
        } else if (viewModel.treeExpandedForTabId == tab.documentId) {
            ensureTreeRootLoaded()
            ensurePathFoldersLoaded(tab)
        } else {
            expandPathToNote(tab)
        }
    }

    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            ensureTreeRootLoaded()
        }
    }

    val useDualPane = dualPaneEnabled && isDualPaneLayoutEligible()
    val useDualPaneState = rememberUpdatedState(useDualPane)

    LaunchedEffect(selectedTabDocumentId, selectedTab?.uri) {
        val tab = selectedTab
        if (tab == null) {
            noteContent = null
            noteLoading = false
            previewDraftText = ""
            resetEditorState()
            return@LaunchedEffect
        }
        val tabUri = tab.uri.toString()
        val alreadyLoaded =
            viewModel.loadedNoteDocumentId == tab.documentId &&
                viewModel.loadedNoteUri == tabUri
        if (alreadyLoaded && noteContent != null && !noteLoading) {
            return@LaunchedEffect
        }
        noteLoading = true
        statusMessage = null
        noteContent = null
        viewModel.clearLoadedNote()
        val result =
            withContext(Dispatchers.IO) {
                runCatching { repository.readText(tab.uri) }
            }
        val loaded = result.getOrNull()
        if (loaded != null) {
            noteContent = loaded
            draftText = loaded
            lastSavedText = loaded
            canvasMarkdownMode = false
            val canvasNote = NoteTitleExtractor.isCanvas(loaded)
            val shouldEdit =
                when {
                    canvasNote -> false

                    autoEditDocumentId == tab.documentId -> {
                        autoEditDocumentId = null
                        true
                    }

                    useDualPaneState.value -> true

                    else -> noteOpenMode == NotesOpenMode.Edit
                }
            if (autoEditDocumentId == tab.documentId && canvasNote) {
                autoEditDocumentId = null
            }
            isEditing = shouldEdit
            previewDraftText = loaded
            statusMessage = null
            val info =
                withContext(Dispatchers.IO) {
                    repository.queryDocumentInfo(tab.uri)
                }
            viewModel.markNoteLoaded(
                tab.documentId,
                tabUri,
                NotesLoadedDocumentBaseline(
                    documentId = tab.documentId,
                    uri = tabUri,
                    lastModifiedEpochMs = info?.lastModifiedEpochMs,
                    sizeBytes = info?.sizeBytes,
                ),
            )
        } else {
            val error = result.exceptionOrNull()
            noteContent = null
            draftText = ""
            lastSavedText = null
            previewDraftText = ""
            canvasMarkdownMode = false
            if (autoEditDocumentId == tab.documentId) {
                autoEditDocumentId = null
            }
            isEditing = false
            statusMessage =
                error?.message ?: context.getString(R.string.markdown_notes_load_failed)
            viewModel.clearLoadedNote()
        }
        noteLoading = false
    }

    val syncExternalChangesLatest =
        rememberUpdatedState(
            newValue =
            suspend {
                syncExternalChanges()
            },
        )
    LaunchedEffect(lifecycleOwner, notesTreeUri) {
        if (notesTreeUri == null) {
            return@LaunchedEffect
        }
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                syncExternalChangesLatest.value.invoke()
                delay(ExternalSyncIntervalMs)
            }
        }
    }

    val isCanvasNote =
        remember(draftText, noteContent) {
            NoteTitleExtractor.isCanvas(draftText.ifBlank { noteContent.orEmpty() })
        }
    val canvasSurfaceTab =
        selectedTab?.takeIf {
            isCanvasNote && !canvasMarkdownMode && noteContent != null
        }

    val hasOpenNoteContent = selectedTabDocumentId != null && noteContent != null
    val canvasAllowsMarkdown = !isCanvasNote || canvasMarkdownMode
    val dualPaneShowsMarkdown = useDualPane && hasOpenNoteContent && canvasAllowsMarkdown
    LaunchedEffect(dualPaneShowsMarkdown, draftText) {
        if (dualPaneShowsMarkdown) {
            isEditing = true
            previewDraftText = draftText
        }
    }

    LaunchedEffect(draftText, useDualPane, selectedTabDocumentId) {
        if (!useDualPane) {
            return@LaunchedEffect
        }
        if (draftText == previewDraftText) {
            return@LaunchedEffect
        }
        delay(AutosaveDelayMs)
        previewDraftText = draftText
    }

    BackHandler {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            navigateBack(useDualPane = useDualPane)
        }
    }

    val treeRows =
        remember(
            treeRoot,
            treeChildrenByFolderId,
            treeExpandedFolderIds,
            sortBy,
            foldersFirst,
            sortReverseOrder,
            showGmdFiles,
        ) {
            val root = treeRoot ?: return@remember emptyList()
            buildVisibleNotesTreeRows(
                root = root,
                childrenByFolderId = treeChildrenByFolderId,
                expandedFolderIds = treeExpandedFolderIds,
                sortBy = sortBy,
                foldersFirst = foldersFirst,
                reverseOrder = sortReverseOrder,
                showGmdFiles = showGmdFiles,
            )
        }

    val visibleEntries =
        remember(entries, sortBy, foldersFirst, sortReverseOrder, showGmdFiles) {
            NotesListingOptions.apply(
                entries = entries,
                sortBy = sortBy,
                foldersFirst = foldersFirst,
                reverseOrder = sortReverseOrder,
                showGmdFiles = showGmdFiles,
            )
        }
    CompositionLocalProvider(LocalNotesIconStyle provides iconStyle) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            modifier = modifier,
            // Allow dismiss by swipe/scrim when open. Block edge-swipe open on welcome and while a
            // note is open: both preview and editor are WebViews, whose touch events Compose does
            // not consume, so the drawer would steal vertical/diagonal scroll swipes.
            gesturesEnabled =
            drawerState.isOpen ||
                (!notesTreeUri.isNullOrBlank() && selectedTab == null),
            drawerContent = {
                NotesTreeDrawerContent(
                    rows = treeRows,
                    expandedFolderIds = treeExpandedFolderIds,
                    selectedNoteDocumentId = selectedTabDocumentId,
                    isLoadingRoot = treeLoadingRoot,
                    density = treeDensity,
                    onToggleFolder = { toggleTreeFolder(it) },
                    onOpenFolder = { folder, parentPath ->
                        scope.launch { drawerState.close() }
                        openFolderList(
                            parentPath +
                                NotesPathSegment(
                                    documentId = folder.documentId,
                                    name = folder.name,
                                    uri = folder.uri,
                                ),
                        )
                    },
                    onOpenNote = { note, parentPath ->
                        scope.launch { drawerState.close() }
                        openNote(note, noteAssetFolderPath(parentPath, note))
                    },
                    onOpenSettings = {
                        scope.launch {
                            drawerState.close()
                            onOpenSettings()
                        }
                    },
                )
            },
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = notesScaffoldContainerColor(),
                contentWindowInsets = notesScaffoldContentWindowInsets(),
            ) { innerPadding ->
                Column(
                    modifier =
                    Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                ) {
                    NotesTopChrome(
                        onOpenDrawer = {
                            scope.launch { drawerState.open() }
                        },
                        breadcrumbSegments =
                        if (notesTreeUri.isNullOrBlank()) {
                            null
                        } else if (selectedTab != null) {
                            selectedTab.folderPath +
                                NotesPathSegment(
                                    documentId = selectedTab.documentId,
                                    name = selectedTab.title,
                                    uri = selectedTab.uri,
                                )
                        } else {
                            folderPath
                        },
                        lastIsNote = selectedTab != null,
                        onSegmentClick = { index ->
                            val path =
                                if (selectedTab != null) {
                                    selectedTab.folderPath
                                } else {
                                    folderPath
                                }
                            val targetIndex = index.coerceAtMost(path.lastIndex)
                            if (targetIndex >= 0) {
                                openFolderList(path.take(targetIndex + 1))
                            }
                        },
                        menuExpanded = menuExpanded,
                        onMenuExpandedChange = { menuExpanded = it },
                        showSortViewMenu =
                        selectedTab == null && !notesTreeUri.isNullOrBlank(),
                        browseLayout = browseLayout,
                        sortBy = sortBy,
                        foldersFirst = foldersFirst,
                        sortReverseOrder = sortReverseOrder,
                        showGmdFiles = showGmdFiles,
                        showNoteDates = showNoteDates,
                        onBrowseLayoutChange = { value ->
                            browseLayout = value
                            preferences.saveBrowseLayout(value)
                        },
                        onSortByChange = { value ->
                            sortBy = value
                            preferences.saveSortBy(value)
                        },
                        onFoldersFirstChange = { value ->
                            foldersFirst = value
                            preferences.saveFoldersFirst(value)
                        },
                        onSortReverseOrderChange = { value ->
                            sortReverseOrder = value
                            preferences.saveSortReverseOrder(value)
                        },
                        onShowGmdFilesChange = { value ->
                            showGmdFiles = value
                            preferences.saveShowGmdFiles(value)
                        },
                        onShowNoteDatesChange = { value ->
                            showNoteDates = value
                            preferences.saveShowNoteDates(value)
                        },
                        noteOpen = selectedTab != null,
                        notePinned = selectedTab?.let { isPinned(it.documentId) } == true,
                        canPinNote =
                        selectedTab != null &&
                            !selectedTab.isExternal &&
                            !notesTreeUri.isNullOrBlank(),
                        onPinNote = {
                            val tab = selectedTab ?: return@NotesTopChrome
                            if (tab.isExternal) {
                                return@NotesTopChrome
                            }
                            pinNote(
                                NotesEntry.Note(
                                    documentId = tab.documentId,
                                    name = tab.fileName.ifBlank { "${tab.title}.md" },
                                    uri = tab.uri,
                                    displayLabel = tab.title,
                                ),
                                tab.folderPath,
                            )
                        },
                        onUnpinNote = {
                            val tab = selectedTab ?: return@NotesTopChrome
                            unpinByDocumentId(tab.documentId)
                        },
                        canPaste =
                        selectedTab == null &&
                            notesClipboard != null &&
                            !notesTreeUri.isNullOrBlank() &&
                            folderPath.isNotEmpty(),
                        onPaste = { pasteClipboard() },
                        onOpenNoteInfo = {
                            val tab = selectedTab
                            if (tab != null) {
                                scope.launch {
                                    noteInfoDocument =
                                        withContext(Dispatchers.IO) {
                                            repository.queryDocumentInfo(tab.uri)
                                        }
                                    showNoteInfoDialog = true
                                }
                            }
                        },
                        onOpenSettings = onOpenSettings,
                        onOpenAbout = onOpenAbout,
                    )
                    if (notesTreeUri.isNullOrBlank()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            NotesPathWelcomeContent(
                                onChooseFolder = { folderPicker.launch(null) },
                                modifier =
                                Modifier
                                    .adaptiveContentWidth()
                                    .padding(24.dp),
                            )
                        }
                    } else {
                        Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            NotesNavigationRow(
                                onBack = { navigateBack(useDualPane = useDualPane) },
                                openTabs = openTabs,
                                selectedTabDocumentId = selectedTabDocumentId,
                                onSelectTab = { selectTab(it) },
                                onCloseTab = { closeTab(it) },
                                onReorderTabs = { from, to -> reorderTabs(from, to) },
                                onCreateNote = { requestCreateNewNote() },
                                showEditActions = selectedTab != null && !noteLoading && noteContent != null,
                                showEditPreviewToggle = !useDualPane && canvasSurfaceTab == null,
                                isEditing = isEditing,
                                isSaving = isSaving,
                                isCanvasNote = isCanvasNote,
                                canvasMarkdownMode = canvasMarkdownMode,
                                onToggleCanvasMarkdown = {
                                    if (canvasMarkdownMode) {
                                        persistCurrentDraft {
                                            canvasMarkdownMode = false
                                            isEditing = false
                                            draftText = noteContent.orEmpty()
                                            lastSavedText = noteContent
                                        }
                                    } else {
                                        canvasMarkdownMode = true
                                        isEditing = useDualPane || noteOpenMode == NotesOpenMode.Edit
                                        draftText = noteContent.orEmpty()
                                        lastSavedText = noteContent
                                        previewDraftText = noteContent.orEmpty()
                                    }
                                },
                                onPreview = {
                                    persistCurrentDraft {
                                        isEditing = false
                                        draftText = noteContent.orEmpty()
                                        lastSavedText = noteContent
                                    }
                                },
                                onEdit = {
                                    isEditing = true
                                    draftText = noteContent.orEmpty()
                                    lastSavedText = noteContent
                                },
                                showCloseNote = selectedTab != null,
                                onCloseNote = {
                                    selectedTab?.let { closeTab(it.documentId) }
                                },
                            )
                            HorizontalDivider()
                            Box(
                                modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                            ) {
                                Box(
                                    modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .clipToBounds()
                                        .background(MaterialTheme.colorScheme.surface),
                                ) {
                                    when {
                                        canvasSurfaceTab != null -> {
                                            NotesCanvasPane(
                                                isLoading = noteLoading,
                                                treeUri = notesTreeUri?.let { Uri.parse(it) },
                                                folderPath = canvasSurfaceTab.folderPath,
                                                noteDocumentId = canvasSurfaceTab.documentId,
                                                noteUri = canvasSurfaceTab.uri,
                                                noteMarkdown = draftText.ifBlank { noteContent.orEmpty() },
                                                contentResolver = context.contentResolver,
                                                preferences = preferences,
                                                onStatusMessage = { message ->
                                                    statusMessage = message
                                                },
                                                onNoteMarkdownChange = { markdown ->
                                                    draftText = markdown
                                                    noteContent = markdown
                                                    lastSavedText = markdown
                                                    previewDraftText = markdown
                                                },
                                            )
                                        }

                                        selectedTab != null && useDualPane -> {
                                            Row(modifier = Modifier.fillMaxSize()) {
                                                NotesMarkdownEditorPane(
                                                    isLoading = noteLoading,
                                                    docKey = selectedTab.documentId,
                                                    text = draftText,
                                                    errorMessage = statusMessage,
                                                    hasContent = noteContent != null,
                                                    fontSizeSp = editorFontSizeSp,
                                                    font = editorFont,
                                                    highlightMaxChars =
                                                    NotesViewerPreferences.highlightMaxChars(
                                                        highlightMaxMb,
                                                    ),
                                                    controller = editorController,
                                                    onTextChange = { value ->
                                                        draftText = value
                                                        scheduleAutosave()
                                                    },
                                                    modifier = Modifier.weight(1f).fillMaxSize(),
                                                )
                                                VerticalDivider()
                                                NotesHtmlPreviewPane(
                                                    isLoading = noteLoading,
                                                    content = previewDraftText,
                                                    errorMessage = statusMessage,
                                                    fontSizeSp = previewFontSizeSp,
                                                    font = previewFont,
                                                    treeUri = notesTreeUri?.let { Uri.parse(it) },
                                                    folderPath = selectedTab.folderPath,
                                                    noteDocumentId = selectedTab.documentId,
                                                    modifier = Modifier.weight(1f).fillMaxSize(),
                                                )
                                            }
                                        }

                                        selectedTab != null -> {
                                            if (isEditing) {
                                                NotesMarkdownEditorPane(
                                                    isLoading = noteLoading,
                                                    docKey = selectedTab.documentId,
                                                    text = draftText,
                                                    errorMessage = statusMessage,
                                                    hasContent = noteContent != null,
                                                    fontSizeSp = editorFontSizeSp,
                                                    font = editorFont,
                                                    highlightMaxChars =
                                                    NotesViewerPreferences.highlightMaxChars(highlightMaxMb),
                                                    controller = editorController,
                                                    onTextChange = { value ->
                                                        draftText = value
                                                        scheduleAutosave()
                                                    },
                                                )
                                            } else {
                                                // Preview mode: simple Markdown → HTML.
                                                NotesHtmlPreviewPane(
                                                    isLoading = noteLoading,
                                                    content = noteContent,
                                                    errorMessage = statusMessage,
                                                    fontSizeSp = previewFontSizeSp,
                                                    font = previewFont,
                                                    treeUri = notesTreeUri?.let { Uri.parse(it) },
                                                    folderPath = selectedTab.folderPath,
                                                    noteDocumentId = selectedTab.documentId,
                                                )
                                            }
                                        }

                                        isLoading -> {
                                            NotesLoadingIndicator(modifier = Modifier.align(Alignment.Center))
                                        }

                                        else -> {
                                            val density = LocalDensity.current
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                NotesFolderList(
                                                    entries = visibleEntries,
                                                    statusMessage = statusMessage,
                                                    density = listDensity,
                                                    layout = browseLayout,
                                                    showNoteDates = showNoteDates,
                                                    pinnedDocumentIds =
                                                    pinnedItems
                                                        .filter { it.kind != NotesPinnedKind.Home }
                                                        .map { it.documentId }
                                                        .toSet(),
                                                    listFirstVisibleIndex =
                                                    viewModel.folderListFirstVisibleIndex,
                                                    listFirstVisibleOffset =
                                                    viewModel.folderListFirstVisibleOffset,
                                                    gridFirstVisibleIndex =
                                                    viewModel.folderGridFirstVisibleIndex,
                                                    gridFirstVisibleOffset =
                                                    viewModel.folderGridFirstVisibleOffset,
                                                    onListScrollPositionChange = { index, offset ->
                                                        viewModel.folderListFirstVisibleIndex = index
                                                        viewModel.folderListFirstVisibleOffset = offset
                                                    },
                                                    onGridScrollPositionChange = { index, offset ->
                                                        viewModel.folderGridFirstVisibleIndex = index
                                                        viewModel.folderGridFirstVisibleOffset = offset
                                                    },
                                                    onOpenFolder = { folder ->
                                                        openFolderList(
                                                            folderPath +
                                                                NotesPathSegment(
                                                                    documentId = folder.documentId,
                                                                    name = folder.name,
                                                                    uri = folder.uri,
                                                                ),
                                                        )
                                                    },
                                                    onOpenNote = { note ->
                                                        openNote(
                                                            note,
                                                            noteAssetFolderPath(folderPath, note),
                                                        )
                                                    },
                                                    onShowMergedNote = { folder ->
                                                        openMergedNote(folder, folderPath)
                                                    },
                                                    onPinFolder = { folder ->
                                                        pinFolder(
                                                            folder,
                                                            folderPath +
                                                                NotesPathSegment(
                                                                    documentId = folder.documentId,
                                                                    name = folder.name,
                                                                    uri = folder.uri,
                                                                ),
                                                        )
                                                    },
                                                    onUnpinFolder = { folder ->
                                                        unpinByDocumentId(folder.documentId)
                                                    },
                                                    onPinNote = { note ->
                                                        pinNote(
                                                            note,
                                                            noteAssetFolderPath(folderPath, note),
                                                        )
                                                    },
                                                    onUnpinNote = { note ->
                                                        unpinByDocumentId(note.documentId)
                                                    },
                                                    onCopyEntry = { entry ->
                                                        clipboardFromEntry(
                                                            entry,
                                                            NotesClipboardMode.Copy,
                                                        )
                                                    },
                                                    onCutEntry = { entry ->
                                                        clipboardFromEntry(
                                                            entry,
                                                            NotesClipboardMode.Cut,
                                                        )
                                                    },
                                                    onDuplicateEntry = { entry ->
                                                        duplicateEntry(entry)
                                                    },
                                                    onDeleteEntry = { entry ->
                                                        pendingDeleteEntry = entry
                                                    },
                                                    onEmptySpaceLongPress = { offset ->
                                                        browseContextMenuOffset =
                                                            with(density) {
                                                                DpOffset(
                                                                    offset.x.toDp(),
                                                                    offset.y.toDp(),
                                                                )
                                                            }
                                                        browseContextMenuExpanded = true
                                                    },
                                                )
                                                DropdownMenu(
                                                    expanded = browseContextMenuExpanded,
                                                    onDismissRequest = {
                                                        browseContextMenuExpanded = false
                                                    },
                                                    offset = browseContextMenuOffset,
                                                ) {
                                                    NotesFolderSortViewMenuContent(
                                                        browseLayout = browseLayout,
                                                        sortBy = sortBy,
                                                        foldersFirst = foldersFirst,
                                                        sortReverseOrder = sortReverseOrder,
                                                        showGmdFiles = showGmdFiles,
                                                        showNoteDates = showNoteDates,
                                                        onBrowseLayoutChange = { value ->
                                                            browseLayout = value
                                                            preferences.saveBrowseLayout(value)
                                                        },
                                                        onSortByChange = { value ->
                                                            sortBy = value
                                                            preferences.saveSortBy(value)
                                                        },
                                                        onFoldersFirstChange = { value ->
                                                            foldersFirst = value
                                                            preferences.saveFoldersFirst(value)
                                                        },
                                                        onSortReverseOrderChange = { value ->
                                                            sortReverseOrder = value
                                                            preferences.saveSortReverseOrder(value)
                                                        },
                                                        onShowGmdFilesChange = { value ->
                                                            showGmdFiles = value
                                                            preferences.saveShowGmdFiles(value)
                                                        },
                                                        onShowNoteDatesChange = { value ->
                                                            showNoteDates = value
                                                            preferences.saveShowNoteDates(value)
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                // Hide FAB over note preview/editor so it does not cover content.
                                if (selectedTab == null) {
                                    FloatingActionButton(
                                        onClick = { requestCreateNewNote() },
                                        modifier =
                                        Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(16.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = stringResource(R.string.markdown_notes_new_note),
                                        )
                                    }
                                }
                            }
                            if (showNotePath) {
                                val pathTab = selectedTab
                                if (pathTab != null) {
                                    NotesNotePathBar(
                                        path = noteFullPathLabel(pathTab),
                                    )
                                }
                            }
                            if (pinnedBarEnabled) {
                                NotesPinnedBar(
                                    items = pinnedItems,
                                    maxSlots = maxPinnedItems,
                                    density = pinnedBarDensity,
                                    onOpen = { openPinnedItem(it) },
                                    onUnpin = { unpinItem(it.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateNoteDialog) {
        NotesCreateNoteDialog(
            untitledFileStem = createNoteUntitledStem,
            beginningTemplates = preferences.loadBeginningTemplates(),
            defaultBeginningTemplateId = preferences.loadDefaultBeginningTemplateId(),
            onDismiss = { showCreateNoteDialog = false },
            onConfirm = { fileStem, noteTitle, beginningTemplateId, isCanvas ->
                showCreateNoteDialog = false
                createNewNote(
                    fileStem = fileStem,
                    noteTitle = noteTitle,
                    beginningTemplateId = beginningTemplateId,
                    isCanvas = isCanvas,
                )
            },
        )
    }
    if (showNoteInfoDialog) {
        val infoTab = selectedTab
        if (infoTab != null) {
            NotesNoteInfoDialog(
                tab = infoTab,
                documentInfo = noteInfoDocument,
                onDismiss = {
                    showNoteInfoDialog = false
                    noteInfoDocument = null
                },
            )
        }
    }
    LaunchedEffect(showNoteInfoDialog, selectedTabDocumentId) {
        if (showNoteInfoDialog && selectedTabDocumentId == null) {
            showNoteInfoDialog = false
            noteInfoDocument = null
        }
    }
    val deleteTarget = pendingDeleteEntry
    if (deleteTarget != null) {
        val label =
            when (deleteTarget) {
                is NotesEntry.Folder -> deleteTarget.name
                is NotesEntry.Note -> deleteTarget.displayLabel.ifBlank { deleteTarget.name }
            }
        AlertDialog(
            onDismissRequest = { pendingDeleteEntry = null },
            title = { Text(stringResource(R.string.markdown_notes_delete_confirm_title)) },
            text = {
                Text(
                    text = stringResource(R.string.markdown_notes_delete_confirm_message, label),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { confirmDeleteEntry(deleteTarget) },
                ) {
                    Text(stringResource(R.string.markdown_notes_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteEntry = null }) {
                    Text(stringResource(R.string.markdown_notes_create_note_cancel))
                }
            },
        )
    }
    LaunchedEffect(externalNoteConflict, selectedTabDocumentId) {
        val conflict = externalNoteConflict ?: return@LaunchedEffect
        if (conflict.tab.documentId != selectedTabDocumentId) {
            externalNoteConflict = null
        }
    }
    val selectedConflict =
        externalNoteConflict?.takeIf { it.tab.documentId == selectedTabDocumentId }
    when (val conflict = selectedConflict) {
        is NotesExternalNoteConflict.Modified -> {
            val label = conflict.tab.title.ifBlank { conflict.tab.fileName }
            AlertDialog(
                onDismissRequest = { keepLocalAgainstExternal(conflict) },
                title = { Text(stringResource(R.string.markdown_notes_external_changed_title)) },
                text = {
                    Text(
                        text =
                        stringResource(
                            R.string.markdown_notes_external_changed_message,
                            label,
                        ),
                    )
                },
                confirmButton = {
                    TextButton(onClick = { reloadSelectedNoteFromDisk() }) {
                        Text(stringResource(R.string.markdown_notes_external_changed_reload))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { keepLocalAgainstExternal(conflict) }) {
                        Text(stringResource(R.string.markdown_notes_external_changed_keep))
                    }
                },
            )
        }

        is NotesExternalNoteConflict.Deleted -> {
            val label = conflict.tab.title.ifBlank { conflict.tab.fileName }
            AlertDialog(
                onDismissRequest = { },
                title = { Text(stringResource(R.string.markdown_notes_external_deleted_title)) },
                text = {
                    Text(
                        text =
                        stringResource(
                            R.string.markdown_notes_external_deleted_message,
                            label,
                        ),
                    )
                },
                confirmButton = {
                    TextButton(onClick = { saveDeletedOpenNote(conflict) }) {
                        Text(stringResource(R.string.markdown_notes_external_deleted_save))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            externalNoteConflict = null
                            closeTabWithoutSaving(conflict.tab.documentId)
                        },
                    ) {
                        Text(stringResource(R.string.markdown_notes_external_deleted_close))
                    }
                },
            )
        }

        null -> Unit
    }
}

private const val AutosaveDelayMs = 800L
private const val ExternalSyncIntervalMs = 3_000L
private const val ExternalProbeQuietPeriodMs = 2_500L
private val NotesTabMaxWidth = 128.dp
private val NotesOpenTabsMenuMaxHeight = 360.dp
private val NotesTabSwipeCloseThreshold = 40.dp
private val NotesMenuReorderStepHeight = 48.dp
private val NotesFabListBottomClearance = 88.dp
private val NotesPathBarMaxFontSize = 12.sp
private val NotesPathBarMinFontSize = 7.sp
private const val NotesPathBarFontStepSp = 0.5f

private fun noteFullPathLabel(tab: OpenNoteTab): String {
    val leaf =
        tab.fileName
            .ifBlank { tab.title }
            .ifBlank { tab.documentId }
    if (tab.isExternal) {
        return leaf
    }
    val parents = tab.folderPath.map { it.name }.filter { it.isNotBlank() }
    return (parents + leaf).joinToString(" / ")
}

@Composable
private fun NotesNotePathBar(
    path: String,
    modifier: Modifier = Modifier,
) {
    val baseStyle = MaterialTheme.typography.labelMedium
    val textMeasurer = rememberTextMeasurer()
    var fontSize by remember(path) { mutableStateOf(NotesPathBarMaxFontSize) }
    val color = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HorizontalDivider()
        BoxWithConstraints(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            val maxWidthPx = constraints.maxWidth
            LaunchedEffect(path, maxWidthPx, baseStyle) {
                if (maxWidthPx <= 0) {
                    return@LaunchedEffect
                }
                var candidate = NotesPathBarMaxFontSize
                while (candidate > NotesPathBarMinFontSize) {
                    val layout =
                        textMeasurer.measure(
                            text = path,
                            style = baseStyle.copy(fontSize = candidate, lineHeight = candidate),
                            overflow = TextOverflow.Clip,
                            softWrap = false,
                            maxLines = 1,
                            constraints = Constraints(maxWidth = maxWidthPx),
                        )
                    if (!layout.hasVisualOverflow) {
                        break
                    }
                    candidate =
                        (candidate.value - NotesPathBarFontStepSp)
                            .coerceAtLeast(NotesPathBarMinFontSize.value)
                            .sp
                }
                fontSize = candidate
            }
            Text(
                text = path,
                style = baseStyle.copy(fontSize = fontSize, lineHeight = fontSize),
                color = color,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun <T> List<T>.moved(
    fromIndex: Int,
    toIndex: Int,
): List<T> {
    if (fromIndex == toIndex || fromIndex !in indices || toIndex !in indices) {
        return this
    }
    val mutable = toMutableList()
    val item = mutable.removeAt(fromIndex)
    mutable.add(toIndex, item)
    return mutable
}

@Composable
private fun NotesTopChrome(
    onOpenDrawer: () -> Unit,
    breadcrumbSegments: List<NotesPathSegment>?,
    lastIsNote: Boolean,
    onSegmentClick: (Int) -> Unit,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    showSortViewMenu: Boolean,
    browseLayout: NotesBrowseLayout,
    sortBy: NotesSortBy,
    foldersFirst: Boolean,
    sortReverseOrder: Boolean,
    showGmdFiles: Boolean,
    showNoteDates: Boolean,
    onBrowseLayoutChange: (NotesBrowseLayout) -> Unit,
    onSortByChange: (NotesSortBy) -> Unit,
    onFoldersFirstChange: (Boolean) -> Unit,
    onSortReverseOrderChange: (Boolean) -> Unit,
    onShowGmdFilesChange: (Boolean) -> Unit,
    onShowNoteDatesChange: (Boolean) -> Unit,
    noteOpen: Boolean,
    notePinned: Boolean,
    canPinNote: Boolean,
    onPinNote: () -> Unit,
    onUnpinNote: () -> Unit,
    canPaste: Boolean,
    onPaste: () -> Unit,
    onOpenNoteInfo: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    var sortViewMenuExpanded by remember { mutableStateOf(false) }
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = stringResource(R.string.nav_open),
                )
            }
            val showBrandTitle =
                breadcrumbSegments == null ||
                    (!lastIsNote && breadcrumbSegments.size <= 1)
            if (showBrandTitle) {
                NotesBrandTitle(
                    logoSize = TopBarLogoSize,
                    textStyle = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
            } else {
                NotesBreadcrumbs(
                    segments = breadcrumbSegments.orEmpty(),
                    lastIsNote = lastIsNote,
                    onSegmentClick = onSegmentClick,
                    modifier = Modifier.weight(1f),
                )
            }
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                if (showSortViewMenu) {
                    Box {
                        IconButton(
                            onClick = { sortViewMenuExpanded = true },
                            modifier = Modifier.size(TopBarActionButtonSize),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = stringResource(R.string.markdown_notes_sort_view_menu),
                            )
                        }
                        DropdownMenu(
                            expanded = sortViewMenuExpanded,
                            onDismissRequest = { sortViewMenuExpanded = false },
                        ) {
                            NotesFolderSortViewMenuContent(
                                browseLayout = browseLayout,
                                sortBy = sortBy,
                                foldersFirst = foldersFirst,
                                sortReverseOrder = sortReverseOrder,
                                showGmdFiles = showGmdFiles,
                                showNoteDates = showNoteDates,
                                onBrowseLayoutChange = onBrowseLayoutChange,
                                onSortByChange = onSortByChange,
                                onFoldersFirstChange = onFoldersFirstChange,
                                onSortReverseOrderChange = onSortReverseOrderChange,
                                onShowGmdFilesChange = onShowGmdFilesChange,
                                onShowNoteDatesChange = onShowNoteDatesChange,
                            )
                        }
                    }
                }
                Box {
                    IconButton(
                        onClick = { onMenuExpandedChange(true) },
                        modifier = Modifier.size(TopBarActionButtonSize),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.markdown_notes_menu),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { onMenuExpandedChange(false) },
                    ) {
                        if (noteOpen) {
                            NotesDropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.markdown_notes_note_info),
                                        maxLines = 2,
                                    )
                                },
                                onClick = {
                                    onMenuExpandedChange(false)
                                    onOpenNoteInfo()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Description,
                                        contentDescription = null,
                                    )
                                },
                            )
                            if (canPinNote) {
                                NotesDropdownMenuItem(
                                    text = {
                                        Text(
                                            text =
                                            stringResource(
                                                if (notePinned) {
                                                    R.string.markdown_notes_unpin
                                                } else {
                                                    R.string.markdown_notes_pin
                                                },
                                            ),
                                            maxLines = 2,
                                        )
                                    },
                                    onClick = {
                                        onMenuExpandedChange(false)
                                        if (notePinned) {
                                            onUnpinNote()
                                        } else {
                                            onPinNote()
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector =
                                            if (notePinned) {
                                                Icons.Outlined.PushPin
                                            } else {
                                                Icons.Filled.PushPin
                                            },
                                            contentDescription = null,
                                        )
                                    },
                                )
                            }
                            HorizontalDivider()
                        } else if (canPaste) {
                            NotesDropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.markdown_notes_paste),
                                        maxLines = 2,
                                    )
                                },
                                onClick = {
                                    onMenuExpandedChange(false)
                                    onPaste()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.ContentPaste,
                                        contentDescription = null,
                                    )
                                },
                            )
                            HorizontalDivider()
                        }
                        NotesDropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.markdown_notes_settings),
                                    maxLines = 2,
                                )
                            },
                            onClick = {
                                onMenuExpandedChange(false)
                                onOpenSettings()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = null,
                                )
                            },
                        )
                        NotesDropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.markdown_notes_about),
                                    maxLines = 2,
                                )
                            },
                            onClick = {
                                onMenuExpandedChange(false)
                                onOpenAbout()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                }
            }
        }
        HorizontalDivider()
    }
}

/** Sort, filter, and browse-layout options for the folder browser. */
@Composable
private fun NotesFolderSortViewMenuContent(
    browseLayout: NotesBrowseLayout,
    sortBy: NotesSortBy,
    foldersFirst: Boolean,
    sortReverseOrder: Boolean,
    showGmdFiles: Boolean,
    showNoteDates: Boolean,
    onBrowseLayoutChange: (NotesBrowseLayout) -> Unit,
    onSortByChange: (NotesSortBy) -> Unit,
    onFoldersFirstChange: (Boolean) -> Unit,
    onSortReverseOrderChange: (Boolean) -> Unit,
    onShowGmdFilesChange: (Boolean) -> Unit,
    onShowNoteDatesChange: (Boolean) -> Unit,
) {
    NotesBrowseLayout.entries.forEach { option ->
        NotesDropdownMenuItem(
            text = {
                Text(
                    text = stringResource(browseLayoutLabelRes(option)),
                    maxLines = 2,
                )
            },
            onClick = { onBrowseLayoutChange(option) },
            leadingIcon = {
                Icon(
                    imageVector =
                    when (option) {
                        NotesBrowseLayout.List -> Icons.AutoMirrored.Filled.ViewList
                        NotesBrowseLayout.Icons -> Icons.Filled.GridView
                    },
                    contentDescription = null,
                )
            },
            trailingIcon = {
                if (browseLayout == option) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                    )
                }
            },
        )
    }
    HorizontalDivider()
    NotesSortBy.entries.forEach { option ->
        NotesDropdownMenuItem(
            text = {
                Text(
                    text = stringResource(sortByLabelRes(option)),
                    maxLines = 2,
                )
            },
            onClick = { onSortByChange(option) },
            leadingIcon = {
                if (sortBy == option) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                    )
                }
            },
        )
    }
    HorizontalDivider()
    NotesDropdownMenuItem(
        text = {
            Text(
                text = stringResource(R.string.markdown_notes_sort_folders_first),
                maxLines = 2,
            )
        },
        onClick = { onFoldersFirstChange(!foldersFirst) },
        trailingIcon = {
            NotesMenuCheckbox(checked = foldersFirst)
        },
    )
    NotesDropdownMenuItem(
        text = {
            Text(
                text = stringResource(R.string.markdown_notes_sort_reverse),
                maxLines = 2,
            )
        },
        onClick = { onSortReverseOrderChange(!sortReverseOrder) },
        trailingIcon = {
            NotesMenuCheckbox(checked = sortReverseOrder)
        },
    )
    NotesDropdownMenuItem(
        text = {
            Text(
                text = stringResource(R.string.markdown_notes_sort_show_gmd),
                maxLines = 2,
            )
        },
        onClick = { onShowGmdFilesChange(!showGmdFiles) },
        trailingIcon = {
            NotesMenuCheckbox(checked = showGmdFiles)
        },
    )
    NotesDropdownMenuItem(
        text = {
            Text(
                text = stringResource(R.string.settings_markdown_notes_show_note_dates),
                maxLines = 2,
            )
        },
        onClick = { onShowNoteDatesChange(!showNoteDates) },
        trailingIcon = {
            NotesMenuCheckbox(checked = showNoteDates)
        },
    )
}

private fun sortByLabelRes(sortBy: NotesSortBy): Int = when (sortBy) {
    NotesSortBy.Name -> R.string.markdown_notes_sort_by_name
    NotesSortBy.Date -> R.string.markdown_notes_sort_by_date
    NotesSortBy.Size -> R.string.markdown_notes_sort_by_size
}

private fun browseLayoutLabelRes(layout: NotesBrowseLayout): Int = when (layout) {
    NotesBrowseLayout.List -> R.string.settings_markdown_notes_browse_layout_list
    NotesBrowseLayout.Icons -> R.string.settings_markdown_notes_browse_layout_icons
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotesTabChip(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onLongPress: () -> Unit,
    isExternal: Boolean = false,
) {
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { NotesTabSwipeCloseThreshold.toPx() }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val colors = MaterialTheme.colorScheme
    val chipColor =
        when {
            selected && isExternal -> colors.tertiaryContainer
            selected -> colors.secondaryContainer
            isExternal -> colors.tertiaryContainer.copy(alpha = 0.55f)
            else -> colors.surfaceContainerHighest
        }
    val labelColor =
        when {
            selected && isExternal -> colors.onTertiaryContainer
            selected -> colors.onSecondaryContainer
            isExternal -> colors.onTertiaryContainer
            else -> colors.onSurfaceVariant
        }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = chipColor,
        tonalElevation = if (selected) 1.dp else 0.dp,
        modifier =
        Modifier
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .combinedClickable(
                onClick = onSelect,
                onLongClick = onLongPress,
            ).pointerInput(title) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (offsetY <= -dismissThresholdPx) {
                            onClose()
                        }
                        offsetY = 0f
                    },
                    onDragCancel = { offsetY = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        offsetY = (offsetY + dragAmount).coerceAtMost(0f)
                    },
                )
            },
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
            Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .widthIn(max = NotesTabMaxWidth),
        )
    }
}

@Composable
private fun NotesOpenTabsPopup(
    tabs: List<OpenNoteTab>,
    selectedTabDocumentId: String?,
    onDismiss: () -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onReorderTabs: (Int, Int) -> Unit,
) {
    val density = LocalDensity.current
    val menuOffsetY = with(density) { 48.dp.roundToPx() }
    val listState = rememberLazyListState()

    Popup(
        alignment = Alignment.TopCenter,
        offset = IntOffset(0, menuOffsetY),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier =
            Modifier
                .padding(horizontal = 12.dp)
                .widthIn(min = 260.dp, max = 400.dp)
                .fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = stringResource(R.string.markdown_notes_open_tabs),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                Box {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.heightIn(max = NotesOpenTabsMenuMaxHeight),
                    ) {
                        itemsIndexed(
                            items = tabs,
                            key = { _, tab -> tab.documentId },
                        ) { index, tab ->
                            NotesOpenTabMenuRow(
                                tab = tab,
                                selected = tab.documentId == selectedTabDocumentId,
                                onSelect = { onSelectTab(tab.documentId) },
                                onClose = { onCloseTab(tab.documentId) },
                                onReorderBySteps = { steps ->
                                    if (steps == 0) {
                                        return@NotesOpenTabMenuRow
                                    }
                                    val toIndex = (index + steps).coerceIn(0, tabs.lastIndex)
                                    onReorderTabs(index, toIndex)
                                },
                            )
                        }
                    }
                    NotesLazyListScrollbar(
                        state = listState,
                        modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesOpenTabMenuRow(
    tab: OpenNoteTab,
    selected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onReorderBySteps: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val reorderStepPx = with(density) { NotesMenuReorderStepHeight.toPx() }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, dragOffsetY.roundToInt()) }
            .background(
                when {
                    selected && tab.isExternal -> MaterialTheme.colorScheme.tertiaryContainer
                    selected -> MaterialTheme.colorScheme.secondaryContainer
                    tab.isExternal -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                    else -> MaterialTheme.colorScheme.surface
                },
            ).clickable(onClick = onSelect)
            .padding(start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.DragHandle,
            contentDescription = stringResource(R.string.markdown_notes_reorder_tab),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
            Modifier
                .size(40.dp)
                .padding(8.dp)
                .pointerInput(tab.documentId) {
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
        Text(
            text = tab.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier =
            Modifier
                .weight(1f)
                .padding(vertical = 6.dp),
        )
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.markdown_notes_close_tab),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun NotesHorizontalScrollbar(
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
private fun NotesLazyListScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
) {
    val layoutInfo = state.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    if (totalItems == 0) {
        return
    }
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) {
        return
    }
    val first = visibleItems.first()
    val last = visibleItems.last()
    val viewportSize = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
    val averageSize =
        visibleItems.sumOf { it.size }.toFloat() / visibleItems.size.coerceAtLeast(1)
    val estimatedContent = averageSize * totalItems
    if (estimatedContent <= viewportSize) {
        return
    }
    val scrolled =
        first.index * averageSize - first.offset + state.firstVisibleItemScrollOffset
    val thumbHeightFraction = (viewportSize / estimatedContent).coerceIn(0.12f, 1f)
    val thumbOffsetFraction =
        (scrolled / (estimatedContent - viewportSize).coerceAtLeast(1f)).coerceIn(0f, 1f)

    BoxWithConstraints(modifier = modifier.width(3.dp)) {
        val trackHeight = maxHeight
        val thumbHeight = trackHeight * thumbHeightFraction
        val thumbOffset = (trackHeight - thumbHeight) * thumbOffsetFraction
        Box(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(thumbHeight)
                .offset(y = thumbOffset)
                .background(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(2.dp),
                ),
        )
    }
}

@Composable
private fun NotesNavigationRow(
    onBack: () -> Unit,
    openTabs: List<OpenNoteTab>,
    selectedTabDocumentId: String?,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onReorderTabs: (Int, Int) -> Unit,
    onCreateNote: () -> Unit,
    showEditActions: Boolean,
    showEditPreviewToggle: Boolean = true,
    isEditing: Boolean,
    isSaving: Boolean,
    isCanvasNote: Boolean = false,
    canvasMarkdownMode: Boolean = false,
    onToggleCanvasMarkdown: () -> Unit = {},
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    showCloseNote: Boolean,
    onCloseNote: () -> Unit,
) {
    var tabsMenuExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(openTabs.isEmpty()) {
        if (openTabs.isEmpty()) {
            tabsMenuExpanded = false
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.markdown_notes_back),
                )
            }
            val tabsScrollState = rememberScrollState()
            LaunchedEffect(openTabs.size, selectedTabDocumentId, tabsScrollState.maxValue) {
                if (selectedTabDocumentId == openTabs.lastOrNull()?.documentId) {
                    tabsScrollState.animateScrollTo(tabsScrollState.maxValue)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(tabsScrollState)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    openTabs.forEach { tab ->
                        NotesTabChip(
                            title = tab.title,
                            selected = tab.documentId == selectedTabDocumentId,
                            onSelect = { onSelectTab(tab.documentId) },
                            onClose = { onCloseTab(tab.documentId) },
                            onLongPress = { tabsMenuExpanded = true },
                            isExternal = tab.isExternal,
                        )
                    }
                    if (openTabs.isEmpty()) {
                        NotesNewNoteTabChip(onClick = onCreateNote)
                    }
                }
                NotesHorizontalScrollbar(
                    state = tabsScrollState,
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                )
            }
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                if (showEditActions && isCanvasNote) {
                    IconButton(
                        onClick = onToggleCanvasMarkdown,
                        modifier = Modifier.size(TopBarActionButtonSize),
                    ) {
                        if (canvasMarkdownMode) {
                            Icon(
                                imageVector = Icons.Filled.Brush,
                                contentDescription = stringResource(R.string.markdown_notes_canvas),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = stringResource(R.string.markdown_notes_canvas_markdown),
                            )
                        }
                    }
                }
                if (showEditActions && showEditPreviewToggle) {
                    if (isEditing) {
                        IconButton(
                            onClick = onPreview,
                            enabled = !isSaving,
                            modifier = Modifier.size(TopBarActionButtonSize),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Visibility,
                                contentDescription = stringResource(R.string.markdown_notes_preview),
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(TopBarActionButtonSize),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.markdown_notes_edit),
                            )
                        }
                    }
                }
                if (showCloseNote) {
                    IconButton(
                        onClick = onCloseNote,
                        modifier = Modifier.size(TopBarActionButtonSize),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.markdown_notes_close_tab),
                        )
                    }
                }
            }
        }
        if (tabsMenuExpanded && openTabs.isNotEmpty()) {
            NotesOpenTabsPopup(
                tabs = openTabs,
                selectedTabDocumentId = selectedTabDocumentId,
                onDismiss = { tabsMenuExpanded = false },
                onSelectTab = { id ->
                    onSelectTab(id)
                    tabsMenuExpanded = false
                },
                onCloseTab = onCloseTab,
                onReorderTabs = onReorderTabs,
            )
        }
    }
}

@Composable
private fun NotesNewNoteTabChip(onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = stringResource(R.string.markdown_notes_new_note),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
            Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .widthIn(max = NotesTabMaxWidth),
        )
    }
}

@Composable
private fun NotesBreadcrumbs(
    segments: List<NotesPathSegment>,
    lastIsNote: Boolean,
    onSegmentClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (segments.isEmpty()) {
        return
    }
    Row(
        modifier =
        modifier
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        segments.forEachIndexed { index, segment ->
            if (index > 0) {
                Text(
                    text = " / ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val isLast = index == segments.lastIndex
            val clickable = !(isLast && lastIsNote)
            val color =
                if (clickable) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            if (index == 0) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = stringResource(R.string.nav_drawer_home),
                    tint = color,
                    modifier =
                    Modifier
                        .size(22.dp)
                        .then(
                            if (clickable) {
                                Modifier.clickable { onSegmentClick(index) }
                            } else {
                                Modifier
                            },
                        ),
                )
            } else {
                Text(
                    text = segment.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                    if (clickable) {
                        Modifier.clickable { onSegmentClick(index) }
                    } else {
                        Modifier
                    },
                )
            }
        }
    }
}

@Composable
private fun NotesFolderList(
    entries: List<NotesEntry>,
    statusMessage: String?,
    density: NotesListDensity,
    layout: NotesBrowseLayout,
    showNoteDates: Boolean,
    pinnedDocumentIds: Set<String>,
    listFirstVisibleIndex: Int,
    listFirstVisibleOffset: Int,
    gridFirstVisibleIndex: Int,
    gridFirstVisibleOffset: Int,
    onListScrollPositionChange: (Int, Int) -> Unit,
    onGridScrollPositionChange: (Int, Int) -> Unit,
    onOpenFolder: (NotesEntry.Folder) -> Unit,
    onOpenNote: (NotesEntry.Note) -> Unit,
    onShowMergedNote: (NotesEntry.Folder) -> Unit,
    onPinFolder: (NotesEntry.Folder) -> Unit,
    onUnpinFolder: (NotesEntry.Folder) -> Unit,
    onPinNote: (NotesEntry.Note) -> Unit,
    onUnpinNote: (NotesEntry.Note) -> Unit,
    onCopyEntry: (NotesEntry) -> Unit,
    onCutEntry: (NotesEntry) -> Unit,
    onDuplicateEntry: (NotesEntry) -> Unit,
    onDeleteEntry: (NotesEntry) -> Unit,
    onEmptySpaceLongPress: ((Offset) -> Unit)? = null,
) {
    val onEmptySpaceLongPressState = rememberUpdatedState(onEmptySpaceLongPress)
    when {
        statusMessage != null && entries.isEmpty() -> {
            Text(
                text = statusMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(24.dp),
            )
        }

        entries.isEmpty() -> {
            Box(
                modifier =
                Modifier
                    .fillMaxSize()
                    .then(
                        if (onEmptySpaceLongPress != null) {
                            Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = { offset ->
                                        onEmptySpaceLongPressState.value?.invoke(offset)
                                    },
                                )
                            }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                Text(
                    text = stringResource(R.string.markdown_notes_folder_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }

        layout == NotesBrowseLayout.Icons -> {
            val gridState =
                rememberLazyGridState(
                    initialFirstVisibleItemIndex = gridFirstVisibleIndex,
                    initialFirstVisibleItemScrollOffset = gridFirstVisibleOffset,
                )
            val onGridScrollPositionChangeState =
                rememberUpdatedState(onGridScrollPositionChange)
            LaunchedEffect(gridState) {
                snapshotFlow {
                    gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
                }.distinctUntilChanged()
                    .collect { (index, offset) ->
                        onGridScrollPositionChangeState.value(index, offset)
                    }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(notesIconsGridColumnCount()),
                state = gridState,
                modifier =
                Modifier
                    .fillMaxSize()
                    .then(
                        if (onEmptySpaceLongPress != null) {
                            Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = { offset ->
                                        val hitItem =
                                            gridState.layoutInfo.visibleItemsInfo.any { item ->
                                                val left = item.offset.x.toFloat()
                                                val top = item.offset.y.toFloat()
                                                val right = left + item.size.width
                                                val bottom = top + item.size.height
                                                offset.x in left..right && offset.y in top..bottom
                                            }
                                        if (!hitItem) {
                                            onEmptySpaceLongPressState.value?.invoke(offset)
                                        }
                                    },
                                )
                            }
                        } else {
                            Modifier
                        },
                    ),
                contentPadding =
                PaddingValues(
                    start = 8.dp,
                    top = 8.dp,
                    end = 8.dp,
                    bottom = NotesFabListBottomClearance,
                ),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(entries, key = { it.documentId }) { entry ->
                    when (entry) {
                        is NotesEntry.Folder -> {
                            NotesFolderIconCell(
                                folder = entry,
                                density = density,
                                pinned = entry.documentId in pinnedDocumentIds,
                                onOpen = { onOpenFolder(entry) },
                                onShowMergedNote = { onShowMergedNote(entry) },
                                onPin = { onPinFolder(entry) },
                                onUnpin = { onUnpinFolder(entry) },
                                onCopy = { onCopyEntry(entry) },
                                onCut = { onCutEntry(entry) },
                                onDuplicate = { onDuplicateEntry(entry) },
                                onDelete = { onDeleteEntry(entry) },
                            )
                        }

                        is NotesEntry.Note -> {
                            NotesNoteIconCell(
                                note = entry,
                                density = density,
                                pinned = entry.documentId in pinnedDocumentIds,
                                onOpen = { onOpenNote(entry) },
                                onPin = { onPinNote(entry) },
                                onUnpin = { onUnpinNote(entry) },
                                onCopy = { onCopyEntry(entry) },
                                onCut = { onCutEntry(entry) },
                                onDuplicate = { onDuplicateEntry(entry) },
                                onDelete = { onDeleteEntry(entry) },
                            )
                        }
                    }
                }
            }
        }

        else -> {
            val listState =
                rememberLazyListState(
                    initialFirstVisibleItemIndex = listFirstVisibleIndex,
                    initialFirstVisibleItemScrollOffset = listFirstVisibleOffset,
                )
            val onListScrollPositionChangeState =
                rememberUpdatedState(onListScrollPositionChange)
            LaunchedEffect(listState) {
                snapshotFlow {
                    listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
                }.distinctUntilChanged()
                    .collect { (index, offset) ->
                        onListScrollPositionChangeState.value(index, offset)
                    }
            }
            LazyColumn(
                state = listState,
                modifier =
                Modifier
                    .fillMaxSize()
                    .then(
                        if (onEmptySpaceLongPress != null) {
                            Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = { offset ->
                                        val hitItem =
                                            listState.layoutInfo.visibleItemsInfo.any { item ->
                                                val top = item.offset.toFloat()
                                                val bottom = top + item.size
                                                offset.y in top..bottom
                                            }
                                        if (!hitItem) {
                                            onEmptySpaceLongPressState.value?.invoke(offset)
                                        }
                                    },
                                )
                            }
                        } else {
                            Modifier
                        },
                    ),
                contentPadding =
                PaddingValues(
                    top = 4.dp,
                    bottom = NotesFabListBottomClearance,
                ),
            ) {
                items(entries, key = { it.documentId }) { entry ->
                    when (entry) {
                        is NotesEntry.Folder -> {
                            NotesFolderRow(
                                folder = entry,
                                density = density,
                                showDate = showNoteDates,
                                pinned = entry.documentId in pinnedDocumentIds,
                                onOpen = { onOpenFolder(entry) },
                                onShowMergedNote = { onShowMergedNote(entry) },
                                onPin = { onPinFolder(entry) },
                                onUnpin = { onUnpinFolder(entry) },
                                onCopy = { onCopyEntry(entry) },
                                onCut = { onCutEntry(entry) },
                                onDuplicate = { onDuplicateEntry(entry) },
                                onDelete = { onDeleteEntry(entry) },
                            )
                        }

                        is NotesEntry.Note -> {
                            NotesNoteRow(
                                note = entry,
                                density = density,
                                showDate = showNoteDates,
                                pinned = entry.documentId in pinnedDocumentIds,
                                onOpen = { onOpenNote(entry) },
                                onPin = { onPinNote(entry) },
                                onUnpin = { onUnpinNote(entry) },
                                onCopy = { onCopyEntry(entry) },
                                onCut = { onCutEntry(entry) },
                                onDuplicate = { onDuplicateEntry(entry) },
                                onDelete = { onDeleteEntry(entry) },
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotesFolderRow(
    folder: NotesEntry.Folder,
    density: NotesListDensity,
    showDate: Boolean,
    pinned: Boolean,
    onOpen: () -> Unit,
    onShowMergedNote: () -> Unit,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val iconSize = density.iconSizeDp.dp
    val menuButtonSize = density.mergedButtonHeightDp.dp
    val rowHeight =
        if (showDate) {
            density.listRowHeightWithDateDp.dp
        } else {
            density.listRowHeightDp.dp
        }
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .combinedClickable(
                onClick = onOpen,
                onLongClick = { menuExpanded = true },
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NotesFolderGlyph(size = iconSize)
        Spacer(modifier = Modifier.width(10.dp))
        NotesListTitleBlock(
            title = folder.name,
            lastModifiedEpochMs = folder.lastModifiedEpochMs,
            showDate = showDate,
            modifier = Modifier.weight(1f),
        )
        if (pinned) {
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = stringResource(R.string.markdown_notes_pinned),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                Modifier
                    .padding(end = 2.dp)
                    .size((density.iconSizeDp * 0.85f).dp),
            )
        }
        Box(modifier = Modifier.size(menuButtonSize)) {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.fillMaxSize(),
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreHoriz,
                    contentDescription = stringResource(R.string.markdown_notes_folder_menu),
                )
            }
            NotesEntryContextMenu(
                expanded = menuExpanded,
                onDismiss = { menuExpanded = false },
                pinned = pinned,
                showMergedNote = folder.hasMergedNote,
                onShowMergedNote = onShowMergedNote,
                onPin = onPin,
                onUnpin = onUnpin,
                onCopy = onCopy,
                onCut = onCut,
                onDuplicate = onDuplicate,
                onDelete = onDelete,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotesNoteRow(
    note: NotesEntry.Note,
    density: NotesListDensity,
    showDate: Boolean,
    pinned: Boolean,
    onOpen: () -> Unit,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val iconSize = density.iconSizeDp.dp
    val menuButtonSize = density.mergedButtonHeightDp.dp
    val rowHeight =
        if (showDate) {
            density.listRowHeightWithDateDp.dp
        } else {
            density.listRowHeightDp.dp
        }
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .combinedClickable(
                onClick = onOpen,
                onLongClick = { menuExpanded = true },
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NotesNoteGlyph(icon = note.displayIcon, size = iconSize)
        Spacer(modifier = Modifier.width(10.dp))
        NotesListTitleBlock(
            title = note.displayLabel,
            lastModifiedEpochMs = note.lastModifiedEpochMs,
            showDate = showDate,
            modifier = Modifier.weight(1f),
        )
        if (pinned) {
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = stringResource(R.string.markdown_notes_pinned),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                Modifier
                    .padding(end = 2.dp)
                    .size((density.iconSizeDp * 0.85f).dp),
            )
        }
        Box(modifier = Modifier.size(menuButtonSize)) {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.fillMaxSize(),
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreHoriz,
                    contentDescription = stringResource(R.string.markdown_notes_note_menu),
                )
            }
            NotesEntryContextMenu(
                expanded = menuExpanded,
                onDismiss = { menuExpanded = false },
                pinned = pinned,
                showMergedNote = false,
                allowClipboardActions = !NotesTreeRepository.isGMd(note.name),
                onShowMergedNote = {},
                onPin = onPin,
                onUnpin = onUnpin,
                onCopy = onCopy,
                onCut = onCut,
                onDuplicate = onDuplicate,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun NotesListTitleBlock(
    title: String,
    lastModifiedEpochMs: Long?,
    showDate: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (showDate) {
            val dateText =
                lastModifiedEpochMs?.let { NotesDateFormats.formatListDateTime(it) }.orEmpty()
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotesFolderIconCell(
    folder: NotesEntry.Folder,
    density: NotesListDensity,
    pinned: Boolean,
    onOpen: () -> Unit,
    onShowMergedNote: () -> Unit,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        NotesIconCell(
            label = folder.name,
            density = density,
            icon = {
                Box {
                    NotesFolderGlyph(size = notesGridIconSize(density))
                    if (pinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = stringResource(R.string.markdown_notes_pinned),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .size(12.dp)
                                .offset(x = 4.dp, y = (-2).dp),
                        )
                    }
                }
            },
            onOpen = onOpen,
            onLongClick = { menuExpanded = true },
        )
        NotesEntryContextMenu(
            expanded = menuExpanded,
            onDismiss = { menuExpanded = false },
            pinned = pinned,
            showMergedNote = folder.hasMergedNote,
            onShowMergedNote = onShowMergedNote,
            onPin = onPin,
            onUnpin = onUnpin,
            onCopy = onCopy,
            onCut = onCut,
            onDuplicate = onDuplicate,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun NotesNoteIconCell(
    note: NotesEntry.Note,
    density: NotesListDensity,
    pinned: Boolean,
    onOpen: () -> Unit,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        NotesIconCell(
            label = note.displayLabel,
            density = density,
            icon = {
                Box {
                    NotesNoteGlyph(
                        icon = note.displayIcon,
                        size = notesGridIconSize(density),
                    )
                    if (pinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = stringResource(R.string.markdown_notes_pinned),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .size(12.dp)
                                .offset(x = 4.dp, y = (-2).dp),
                        )
                    }
                }
            },
            onOpen = onOpen,
            onLongClick = { menuExpanded = true },
        )
        NotesEntryContextMenu(
            expanded = menuExpanded,
            onDismiss = { menuExpanded = false },
            pinned = pinned,
            showMergedNote = false,
            allowClipboardActions = !NotesTreeRepository.isGMd(note.name),
            onShowMergedNote = {},
            onPin = onPin,
            onUnpin = onUnpin,
            onCopy = onCopy,
            onCut = onCut,
            onDuplicate = onDuplicate,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun NotesEntryContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    pinned: Boolean,
    showMergedNote: Boolean,
    allowClipboardActions: Boolean = true,
    onShowMergedNote: () -> Unit,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        if (showMergedNote) {
            NotesDropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.markdown_notes_show_merged),
                        maxLines = 2,
                    )
                },
                onClick = {
                    onDismiss()
                    onShowMergedNote()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = null,
                    )
                },
            )
        }
        NotesDropdownMenuItem(
            text = {
                Text(
                    text =
                    stringResource(
                        if (pinned) {
                            R.string.markdown_notes_unpin
                        } else {
                            R.string.markdown_notes_pin
                        },
                    ),
                    maxLines = 2,
                )
            },
            onClick = {
                onDismiss()
                if (pinned) {
                    onUnpin()
                } else {
                    onPin()
                }
            },
            leadingIcon = {
                Icon(
                    imageVector =
                    if (pinned) {
                        Icons.Outlined.PushPin
                    } else {
                        Icons.Filled.PushPin
                    },
                    contentDescription = null,
                )
            },
        )
        if (allowClipboardActions) {
            HorizontalDivider()
            NotesDropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.markdown_notes_cut),
                        maxLines = 2,
                    )
                },
                onClick = {
                    onDismiss()
                    onCut()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.ContentCut,
                        contentDescription = null,
                    )
                },
            )
            NotesDropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.markdown_notes_copy),
                        maxLines = 2,
                    )
                },
                onClick = {
                    onDismiss()
                    onCopy()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = null,
                    )
                },
            )
            NotesDropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.markdown_notes_duplicate),
                        maxLines = 2,
                    )
                },
                onClick = {
                    onDismiss()
                    onDuplicate()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.FileCopy,
                        contentDescription = null,
                    )
                },
            )
        } else {
            HorizontalDivider()
        }
        NotesDropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.markdown_notes_delete),
                    maxLines = 2,
                )
            },
            onClick = {
                onDismiss()
                onDelete()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                )
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotesIconCell(
    label: String,
    density: NotesListDensity,
    icon: @Composable () -> Unit,
    onOpen: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val verticalPadding = density.verticalPaddingDp.dp
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = onOpen,
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier.clickable(onClick = onOpen)
                },
            ).padding(horizontal = 4.dp, vertical = verticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon()
        Spacer(modifier = Modifier.height(6.dp))
        NotesAutoSizeLabel(
            text = label,
            modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 36.dp),
        )
    }
}

@Composable
private fun NotesAutoSizeLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val baseStyle = MaterialTheme.typography.labelMedium
    val textMeasurer = rememberTextMeasurer()
    val maxLines =
        if (isCompactHeight()) {
            NotesIconsLabelCompactMaxLines
        } else {
            NotesIconsLabelMaxLines
        }
    var fontSize by remember(text) { mutableStateOf(NotesIconsLabelMaxFontSize) }

    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = constraints.maxWidth
        LaunchedEffect(text, maxWidthPx, baseStyle, maxLines) {
            if (maxWidthPx <= 0) {
                return@LaunchedEffect
            }
            var candidate = NotesIconsLabelMaxFontSize
            while (candidate > NotesIconsLabelMinFontSize) {
                val layout =
                    textMeasurer.measure(
                        text = text,
                        style = baseStyle.copy(fontSize = candidate, lineHeight = candidate * 1.2f),
                        overflow = TextOverflow.Clip,
                        softWrap = true,
                        maxLines = maxLines,
                        constraints = Constraints(maxWidth = maxWidthPx),
                    )
                if (!layout.hasVisualOverflow) {
                    break
                }
                candidate = (candidate.value - NotesIconsLabelFontStepSp).coerceAtLeast(
                    NotesIconsLabelMinFontSize.value,
                ).sp
            }
            fontSize = candidate
        }
        Text(
            text = text,
            style = baseStyle.copy(fontSize = fontSize, lineHeight = fontSize * 1.2f),
            textAlign = TextAlign.Center,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            softWrap = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun notesGridIconSize(density: NotesListDensity) = (density.iconSizeDp * 2).dp

@Composable
private fun NotesPathWelcomeContent(
    onChooseFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.markdown_notes_welcome_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.markdown_notes_welcome_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onChooseFolder,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                text = stringResource(R.string.markdown_notes_choose_folder),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Shared notes-folder picker + path summary for settings. */
@Composable
fun NotesFolderPathControls(
    treeUri: String?,
    onTreeUriChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val preferences = remember { NotesViewerPreferences(context.applicationContext) }
    val folderPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree(),
        ) { uri: Uri? ->
            if (uri == null) {
                return@rememberLauncherForActivityResult
            }
            takeNotesFolderPermission(context, uri)
            val value = uri.toString()
            preferences.saveNotesTreeUri(value)
            onTreeUriChange(value)
        }
    val buttonPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_markdown_notes_path),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text =
            if (treeUri.isNullOrBlank()) {
                stringResource(R.string.settings_markdown_notes_path_none)
            } else {
                notesFolderDisplayName(context, treeUri)
            },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Button(
            onClick = { folderPicker.launch(null) },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = buttonPadding,
        ) {
            Text(
                text = stringResource(R.string.markdown_notes_choose_folder),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        OutlinedButton(
            onClick = {
                preferences.clearNotesTreeUri()
                onTreeUriChange(null)
            },
            enabled = !treeUri.isNullOrBlank(),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = buttonPadding,
        ) {
            Text(
                text = stringResource(R.string.settings_markdown_notes_path_clear),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

package dev.harrix.notes.ui.notes

import android.net.Uri
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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.harrix.notes.NoteMetaUpdates
import dev.harrix.notes.NotesBrowseLayout
import dev.harrix.notes.NotesEntry
import dev.harrix.notes.NotesListDensity
import dev.harrix.notes.NotesPathSegment
import dev.harrix.notes.NotesPinnedItem
import dev.harrix.notes.NotesPinnedKind
import dev.harrix.notes.NotesTitleSource
import dev.harrix.notes.NotesTreeRepository
import dev.harrix.notes.NotesViewerPreferences
import dev.harrix.notes.OpenNoteTab
import dev.harrix.notes.R
import dev.harrix.notes.notesFolderDisplayName
import dev.harrix.notes.takeNotesFolderPermission
import dev.harrix.notes.ui.adaptiveContentWidth
import dev.harrix.notes.ui.isCompactHeight
import dev.harrix.notes.ui.notesIconsGridColumnCount
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

@Composable
fun NotesViewerScreen(
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
    settingsRevision: Int = 0,
    viewModel: NotesViewerViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = viewModel.preferences
    val repository = viewModel.repository
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var notesTreeUri by viewModel.notesTreeUri
    var menuExpanded by remember { mutableStateOf(false) }
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
    var autoEditDocumentId by viewModel.autoEditDocumentId
    var draftText by viewModel.draftText
    var lastSavedText by viewModel.lastSavedText
    var isSaving by viewModel.isSaving
    var autosaveJob by viewModel.autosaveJob
    var folderListRequestId by viewModel.folderListRequestId
    var listDensity by viewModel.listDensity
    var browseLayout by viewModel.browseLayout
    var titleSource by viewModel.titleSource
    var maxOpenTabs by viewModel.maxOpenTabs
    var pinnedBarEnabled by viewModel.pinnedBarEnabled
    var maxPinnedItems by viewModel.maxPinnedItems
    var pinnedItems by viewModel.pinnedItems
    var pinnedRestoredForTree by viewModel.pinnedRestoredForTree
    var treeRoot by viewModel.treeRoot
    var treeChildrenByFolderId by viewModel.treeChildrenByFolderId
    var treeExpandedFolderIds by viewModel.treeExpandedFolderIds
    var treeLoadingRoot by viewModel.treeLoadingRoot

    fun reloadPath() {
        notesTreeUri = preferences.loadNotesTreeUri()
        listDensity = preferences.loadListDensity()
        browseLayout = preferences.loadBrowseLayout()
        titleSource = preferences.loadTitleSource()
        maxOpenTabs = preferences.loadMaxOpenTabs()
        pinnedBarEnabled = preferences.loadPinnedBarEnabled()
        maxPinnedItems = preferences.loadMaxPinnedItems()
    }

    fun clearTreeState() {
        treeRoot = null
        treeChildrenByFolderId = emptyMap()
        treeExpandedFolderIds = emptySet()
        treeLoadingRoot = false
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
        return result
            .onSuccess {
                lastSavedText = text
                noteContent = text
                statusMessage = null
                Toast
                    .makeText(context, R.string.markdown_notes_saved, Toast.LENGTH_SHORT)
                    .show()
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
            }.isSuccess
    }

    fun persistCurrentDraft(after: (() -> Unit)? = null) {
        val tab = openTabs.firstOrNull { it.documentId == selectedTabDocumentId }
        if (tab == null || !isEditing) {
            after?.invoke()
            return
        }
        if (draftText == lastSavedText) {
            after?.invoke()
            return
        }
        scope.launch {
            saveNoteText(tab.uri, draftText)
            after?.invoke()
        }
    }

    fun scheduleAutosave() {
        val tab = openTabs.firstOrNull { it.documentId == selectedTabDocumentId } ?: return
        if (!isEditing || draftText == lastSavedText) {
            return
        }
        autosaveJob?.cancel()
        autosaveJob =
            scope.launch {
                delay(AutosaveDelayMs)
                if (isEditing && draftText != lastSavedText) {
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
        val limit = maxOpenTabs.coerceAtLeast(NotesViewerPreferences.MIN_OPEN_TABS)
        if (openTabs.size <= limit) {
            if (preferredSelectedId != null && openTabs.any { it.documentId == preferredSelectedId }) {
                selectedTabDocumentId = preferredSelectedId
            } else if (selectedTabDocumentId != null &&
                openTabs.none { it.documentId == selectedTabDocumentId }
            ) {
                selectedTabDocumentId = openTabs.lastOrNull()?.documentId
            }
            return
        }
        // Drop oldest tabs first (left side of the tab bar) until within the limit.
        val kept = openTabs.takeLast(limit)
        openTabs = kept
        if (preferredSelectedId != null && kept.any { it.documentId == preferredSelectedId }) {
            selectedTabDocumentId = preferredSelectedId
        } else {
            selectedTabDocumentId = kept.lastOrNull()?.documentId
        }
    }

    fun appendOpenTab(tab: OpenNoteTab) {
        openTabs = openTabs + tab
        ensureMaxOpenTabs(preferredSelectedId = tab.documentId)
    }

    fun openNote(
        note: NotesEntry.Note,
        pathForNote: List<NotesPathSegment>,
    ) {
        val existing = openTabs.firstOrNull { it.documentId == note.documentId }
        if (existing == null) {
            appendOpenTab(
                OpenNoteTab(
                    documentId = note.documentId,
                    uri = note.uri,
                    title = note.displayLabel,
                    fileName = note.name,
                    folderPath = pathForNote,
                ),
            )
        }
        if (selectedTabDocumentId != note.documentId) {
            noteLoading = true
            noteContent = null
            resetEditorState()
        }
        selectedTabDocumentId = note.documentId
    }

    fun createNewNote() {
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
            val note =
                withContext(Dispatchers.IO) {
                    runCatching {
                        repository.createMarkdownNote(treeUri, dir.documentId)
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
            autoEditDocumentId = note.documentId
            openNote(note, path)
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
        val existing = openTabs.firstOrNull { it.documentId == documentId }
        if (existing == null) {
            appendOpenTab(
                OpenNoteTab(
                    documentId = documentId,
                    uri = uri,
                    title = title,
                    fileName = fileName,
                    folderPath = pathForFolder,
                ),
            )
        }
        if (selectedTabDocumentId != documentId) {
            noteLoading = true
            noteContent = null
            resetEditorState()
        }
        selectedTabDocumentId = documentId
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

    fun reorderTabs(
        fromIndex: Int,
        toIndex: Int,
    ) {
        openTabs = openTabs.moved(fromIndex, toIndex)
    }

    fun navigateBack() {
        when {
            isEditing -> {
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
        val previousMaxPinned = maxPinnedItems
        val hadSession = viewModel.appliedSettingsRevision >= 0
        reloadPath()
        viewModel.appliedSettingsRevision = settingsRevision
        if (previousTitleSource != titleSource) {
            applyTitleSourceToVisibleLists()
        }
        if (previousMaxOpenTabs != maxOpenTabs) {
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
            openTabs = emptyList()
            pinnedItems = emptyList()
            selectedTabDocumentId = null
            noteContent = null
            noteLoading = false
            resetEditorState()
            preferences.clearOpenTabsSession()
        }
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

    LaunchedEffect(selectedTabDocumentId, selectedTab?.uri) {
        val tab = selectedTab
        if (tab == null) {
            noteContent = null
            noteLoading = false
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
        result
            .onSuccess { loaded ->
                noteContent = loaded
                draftText = loaded
                lastSavedText = loaded
                val shouldEdit = autoEditDocumentId == tab.documentId
                if (shouldEdit) {
                    autoEditDocumentId = null
                }
                isEditing = shouldEdit
                statusMessage = null
                viewModel.markNoteLoaded(tab.documentId, tabUri)
            }.onFailure { error ->
                noteContent = null
                draftText = ""
                lastSavedText = null
                if (autoEditDocumentId == tab.documentId) {
                    autoEditDocumentId = null
                }
                isEditing = false
                statusMessage =
                    error.message ?: context.getString(R.string.markdown_notes_load_failed)
                viewModel.clearLoadedNote()
            }
        noteLoading = false
    }

    BackHandler {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            navigateBack()
        }
    }

    val treeRows =
        remember(treeRoot, treeChildrenByFolderId, treeExpandedFolderIds) {
            val root = treeRoot ?: return@remember emptyList()
            buildVisibleNotesTreeRows(
                root = root,
                childrenByFolderId = treeChildrenByFolderId,
                expandedFolderIds = treeExpandedFolderIds,
            )
        }
    ModalNavigationDrawer(
        drawerState = drawerState,
        modifier = modifier,
        // Allow dismiss by swipe/scrim when open; only block edge-swipe open on welcome.
        gesturesEnabled = drawerState.isOpen || !notesTreeUri.isNullOrBlank(),
        drawerContent = {
            NotesTreeDrawerContent(
                rows = treeRows,
                expandedFolderIds = treeExpandedFolderIds,
                selectedNoteDocumentId = selectedTabDocumentId,
                isLoadingRoot = treeLoadingRoot,
                density = listDensity,
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
                    openNote(note, parentPath)
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
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
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
                    NotesNavigationRow(
                        onBack = { navigateBack() },
                        openTabs = openTabs,
                        selectedTabDocumentId = selectedTabDocumentId,
                        onSelectTab = { selectTab(it) },
                        onCloseTab = { closeTab(it) },
                        onReorderTabs = { from, to -> reorderTabs(from, to) },
                        onCreateNote = { createNewNote() },
                        showEditActions = selectedTab != null && !noteLoading && noteContent != null,
                        isEditing = isEditing,
                        isSaving = isSaving,
                        onSave = { persistCurrentDraft() },
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
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface),
                    ) {
                        when {
                            selectedTab != null -> {
                                if (isEditing) {
                                    NotesPlainTextEditorPane(
                                        isLoading = noteLoading,
                                        draftText = draftText,
                                        errorMessage = statusMessage,
                                        hasContent = noteContent != null,
                                        onDraftChange = { value ->
                                            draftText = value
                                            scheduleAutosave()
                                        },
                                    )
                                } else {
                                    // Preview mode (temporary HTML <pre> viewer).
                                    NotesHtmlPreviewPane(
                                        isLoading = noteLoading,
                                        content = noteContent,
                                        errorMessage = statusMessage,
                                    )
                                }
                            }

                            isLoading -> {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }

                            else -> {
                                NotesFolderList(
                                    entries = entries,
                                    statusMessage = statusMessage,
                                    density = listDensity,
                                    layout = browseLayout,
                                    pinnedDocumentIds =
                                    pinnedItems
                                        .filter { it.kind != NotesPinnedKind.Home }
                                        .map { it.documentId }
                                        .toSet(),
                                    listFirstVisibleIndex = viewModel.folderListFirstVisibleIndex,
                                    listFirstVisibleOffset = viewModel.folderListFirstVisibleOffset,
                                    gridFirstVisibleIndex = viewModel.folderGridFirstVisibleIndex,
                                    gridFirstVisibleOffset = viewModel.folderGridFirstVisibleOffset,
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
                                        openNote(note, folderPath)
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
                                        pinNote(note, folderPath)
                                    },
                                    onUnpinNote = { note ->
                                        unpinByDocumentId(note.documentId)
                                    },
                                )
                            }
                        }
                    }
                    if (pinnedBarEnabled) {
                        NotesPinnedBar(
                            items = pinnedItems,
                            maxSlots = maxPinnedItems,
                            density = listDensity,
                            onOpen = { openPinnedItem(it) },
                            onUnpin = { unpinItem(it.id) },
                        )
                    }
                }
            }
        }
    }
}

private const val AutosaveDelayMs = 800L
private val NotesTabMaxWidth = 128.dp
private val NotesOpenTabsMenuMaxHeight = 360.dp
private val NotesTabSwipeCloseThreshold = 40.dp
private val NotesMenuReorderStepHeight = 48.dp

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
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
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
        Box {
            IconButton(onClick = { onMenuExpandedChange(true) }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.markdown_notes_menu),
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { onMenuExpandedChange(false) },
            ) {
                DropdownMenuItem(
                    text = {
                        Text(stringResource(R.string.markdown_notes_settings))
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
                DropdownMenuItem(
                    text = {
                        Text(stringResource(R.string.markdown_notes_about))
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotesTabChip(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onLongPress: () -> Unit,
) {
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { NotesTabSwipeCloseThreshold.toPx() }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Surface(
        shape = MaterialTheme.shapes.large,
        color =
        if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
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
            color =
            if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
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
                if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
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
                .padding(vertical = 10.dp),
        )
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.markdown_notes_close_tab),
            )
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
    isEditing: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit,
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
            if (showEditActions) {
                if (isEditing) {
                    IconButton(
                        onClick = onSave,
                        enabled = !isSaving,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = stringResource(R.string.markdown_notes_save),
                        )
                    }
                    IconButton(
                        onClick = onPreview,
                        enabled = !isSaving,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = stringResource(R.string.markdown_notes_preview),
                        )
                    }
                } else {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.markdown_notes_edit),
                        )
                    }
                }
            }
            if (showCloseNote) {
                IconButton(onClick = onCloseNote) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.markdown_notes_close_tab),
                    )
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
                        .size(18.dp)
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
) {
    when {
        statusMessage != null && entries.isEmpty() -> {
            Text(
                text = statusMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(24.dp),
            )
        }

        entries.isEmpty() -> {
            Text(
                text = stringResource(R.string.markdown_notes_folder_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(entries, key = { it.documentId }) { entry ->
                    when (entry) {
                        is NotesEntry.Folder -> {
                            NotesFolderRow(
                                folder = entry,
                                density = density,
                                pinned = entry.documentId in pinnedDocumentIds,
                                onOpen = { onOpenFolder(entry) },
                                onShowMergedNote = { onShowMergedNote(entry) },
                                onPin = { onPinFolder(entry) },
                                onUnpin = { onUnpinFolder(entry) },
                            )
                        }

                        is NotesEntry.Note -> {
                            NotesNoteRow(
                                note = entry,
                                density = density,
                                pinned = entry.documentId in pinnedDocumentIds,
                                onOpen = { onOpenNote(entry) },
                                onPin = { onPinNote(entry) },
                                onUnpin = { onUnpinNote(entry) },
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun NotesFolderRow(
    folder: NotesEntry.Folder,
    density: NotesListDensity,
    pinned: Boolean,
    onOpen: () -> Unit,
    onShowMergedNote: () -> Unit,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val iconSize = density.iconSizeDp.dp
    val menuButtonSize = density.mergedButtonHeightDp.dp
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(density.listRowHeightDp.dp)
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(iconSize),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = folder.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
            )
        }
    }
}

@Composable
private fun NotesNoteRow(
    note: NotesEntry.Note,
    density: NotesListDensity,
    pinned: Boolean,
    onOpen: () -> Unit,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val iconSize = density.iconSizeDp.dp
    val menuButtonSize = density.mergedButtonHeightDp.dp
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(density.listRowHeightDp.dp)
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NotesNoteGlyph(icon = note.displayIcon, size = iconSize)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = note.displayLabel,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
                onShowMergedNote = {},
                onPin = onPin,
                onUnpin = onUnpin,
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
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        NotesIconCell(
            label = folder.name,
            density = density,
            icon = {
                Box {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(notesGridIconSize(density)),
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
            showMergedNote = folder.hasMergedNote,
            onShowMergedNote = onShowMergedNote,
            onPin = onPin,
            onUnpin = onUnpin,
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
            onShowMergedNote = {},
            onPin = onPin,
            onUnpin = onUnpin,
        )
    }
}

@Composable
private fun NotesEntryContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    pinned: Boolean,
    showMergedNote: Boolean,
    onShowMergedNote: () -> Unit,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        if (showMergedNote) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.markdown_notes_show_merged)) },
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
        DropdownMenuItem(
            text = {
                Text(
                    stringResource(
                        if (pinned) {
                            R.string.markdown_notes_unpin
                        } else {
                            R.string.markdown_notes_pin
                        },
                    ),
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

/**
 * Note **editor** mode (plain text with lightweight Markdown highlighting).
 * Separate from [NotesHtmlPreviewPane] — preview will later become real HTML rendering.
 */
@Composable
private fun NotesPlainTextEditorPane(
    isLoading: Boolean,
    draftText: String,
    errorMessage: String?,
    hasContent: Boolean,
    onDraftChange: (String) -> Unit,
) {
    val highlightColors = rememberMarkdownHighlightColors()
    val editTransformation =
        remember(highlightColors) {
            MarkdownSyntaxVisualTransformation(highlightColors)
        }

    when {
        isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        errorMessage != null && !hasContent -> {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(24.dp),
            )
        }

        else -> {
            TextField(
                value = draftText,
                onValueChange = onDraftChange,
                modifier = Modifier.fillMaxSize(),
                textStyle = MaterialTheme.typography.bodyMedium,
                visualTransformation = editTransformation,
                colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.surface,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    }
}

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

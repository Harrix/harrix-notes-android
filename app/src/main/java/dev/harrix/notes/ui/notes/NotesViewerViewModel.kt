package dev.harrix.notes.ui.notes

import android.app.Application
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import dev.harrix.notes.NotesEntry
import dev.harrix.notes.NotesOpenMode
import dev.harrix.notes.NotesPathSegment
import dev.harrix.notes.NotesPinnedItem
import dev.harrix.notes.NotesTreeRepository
import dev.harrix.notes.NotesViewerPreferences
import dev.harrix.notes.OpenNoteTab
import kotlinx.coroutines.Job

/**
 * Survives configuration changes (rotation) for the notes viewer session.
 */
class NotesViewerViewModel(
    application: Application,
) : AndroidViewModel(application) {
    val preferences = NotesViewerPreferences(application.applicationContext)
    val repository = NotesTreeRepository(application.applicationContext)

    val notesTreeUri = mutableStateOf(preferences.loadNotesTreeUri())
    val folderPath = mutableStateOf<List<NotesPathSegment>>(emptyList())
    val entries = mutableStateOf<List<NotesEntry>>(emptyList())
    val isLoading = mutableStateOf(false)
    val statusMessage = mutableStateOf<String?>(null)
    val openTabs = mutableStateOf<List<OpenNoteTab>>(emptyList())
    val selectedTabDocumentId = mutableStateOf<String?>(null)
    val sessionRestoredForTree = mutableStateOf<String?>(null)
    val noteContent = mutableStateOf<String?>(null)
    val noteLoading = mutableStateOf(false)
    val isEditing = mutableStateOf(false)
    val autoEditDocumentId = mutableStateOf<String?>(null)
    val draftText = mutableStateOf("")
    val lastSavedText = mutableStateOf<String?>(null)
    val isSaving = mutableStateOf(false)
    val autosaveJob = mutableStateOf<Job?>(null)
    val folderListRequestId = mutableIntStateOf(0)
    val listDensity = mutableStateOf(preferences.loadListDensity())
    val browseLayout = mutableStateOf(preferences.loadBrowseLayout())
    val titleSource = mutableStateOf(preferences.loadTitleSource())
    val noteOpenMode = mutableStateOf(preferences.loadNoteOpenMode())
    val previewFontSizeSp = mutableIntStateOf(preferences.loadPreviewFontSizeSp())
    val editorFontSizeSp = mutableIntStateOf(preferences.loadEditorFontSizeSp())
    val maxOpenTabs = mutableIntStateOf(preferences.loadMaxOpenTabs())
    val pinnedBarEnabled = mutableStateOf(preferences.loadPinnedBarEnabled())
    val maxPinnedItems = mutableIntStateOf(preferences.loadMaxPinnedItems())
    val pinnedItems = mutableStateOf<List<NotesPinnedItem>>(emptyList())
    val pinnedRestoredForTree = mutableStateOf<String?>(null)
    val treeRoot = mutableStateOf<NotesPathSegment?>(null)
    val treeChildrenByFolderId = mutableStateOf<Map<String, List<NotesEntry>>>(emptyMap())
    val treeExpandedFolderIds = mutableStateOf<Set<String>>(emptySet())
    val treeLoadingRoot = mutableStateOf(false)

    /** Document id last expanded in the drawer tree; skips reset on rotation. */
    var treeExpandedForTabId: String? = null

    /** Note currently held in [noteContent] / editor; skips disk reload on rotation. */
    var loadedNoteDocumentId: String? = null
    var loadedNoteUri: String? = null

    var noteListFirstVisibleIndex: Int = 0
    var noteListFirstVisibleOffset: Int = 0
    var folderListFirstVisibleIndex: Int = 0
    var folderListFirstVisibleOffset: Int = 0
    var folderGridFirstVisibleIndex: Int = 0
    var folderGridFirstVisibleOffset: Int = 0

    var appliedSettingsRevision: Int = -1

    fun clearLoadedNote() {
        loadedNoteDocumentId = null
        loadedNoteUri = null
        noteListFirstVisibleIndex = 0
        noteListFirstVisibleOffset = 0
    }

    fun markNoteLoaded(
        documentId: String,
        uri: String,
    ) {
        loadedNoteDocumentId = documentId
        loadedNoteUri = uri
    }

    fun resetFolderScroll() {
        folderListFirstVisibleIndex = 0
        folderListFirstVisibleOffset = 0
        folderGridFirstVisibleIndex = 0
        folderGridFirstVisibleOffset = 0
    }

    override fun onCleared() {
        autosaveJob.value?.cancel()
        autosaveJob.value = null
        super.onCleared()
    }
}

package dev.harrix.notes

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract

/** Preferences for the Markdown notes viewer utility. */
class NotesViewerPreferences(
    context: Context,
) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadNotesTreeUri(): String? = prefs.getString(KEY_NOTES_TREE_URI, null)?.takeIf { it.isNotBlank() }

    fun saveNotesTreeUri(uri: String) {
        val previous = loadNotesTreeUri()
        prefs.edit().putString(KEY_NOTES_TREE_URI, uri).apply()
        if (previous != null && previous != uri) {
            clearOpenTabsSession()
        }
    }

    fun clearNotesTreeUri() {
        prefs.edit().remove(KEY_NOTES_TREE_URI).apply()
        clearOpenTabsSession()
    }

    fun hasNotesPath(): Boolean = !loadNotesTreeUri().isNullOrBlank()

    fun loadListDensity(): NotesListDensity = NotesListDensity.fromStorageKey(prefs.getString(KEY_LIST_DENSITY, null))

    fun saveListDensity(density: NotesListDensity) {
        prefs.edit().putString(KEY_LIST_DENSITY, density.name).apply()
    }

    /** Drawer tree density; falls back to [loadListDensity] for older installs. */
    fun loadTreeDensity(): NotesListDensity = NotesListDensity.fromStorageKey(
        prefs.getString(KEY_TREE_DENSITY, null) ?: prefs.getString(KEY_LIST_DENSITY, null),
    )

    fun saveTreeDensity(density: NotesListDensity) {
        prefs.edit().putString(KEY_TREE_DENSITY, density.name).apply()
    }

    /** Pinned bar density; falls back to [loadListDensity] for older installs. */
    fun loadPinnedBarDensity(): NotesListDensity = NotesListDensity.fromStorageKey(
        prefs.getString(KEY_PINNED_BAR_DENSITY, null) ?: prefs.getString(KEY_LIST_DENSITY, null),
    )

    fun savePinnedBarDensity(density: NotesListDensity) {
        prefs.edit().putString(KEY_PINNED_BAR_DENSITY, density.name).apply()
    }

    fun loadBrowseLayout(): NotesBrowseLayout = NotesBrowseLayout.fromStorageKey(prefs.getString(KEY_BROWSE_LAYOUT, null))

    fun saveBrowseLayout(layout: NotesBrowseLayout) {
        prefs.edit().putString(KEY_BROWSE_LAYOUT, layout.name).apply()
    }

    fun loadTitleSource(): NotesTitleSource = NotesTitleSource.fromStorageKey(prefs.getString(KEY_TITLE_SOURCE, null))

    fun saveTitleSource(source: NotesTitleSource) {
        prefs.edit().putString(KEY_TITLE_SOURCE, source.name).apply()
    }

    fun loadNoteOpenMode(): NotesOpenMode = NotesOpenMode.fromStorageKey(prefs.getString(KEY_NOTE_OPEN_MODE, null))

    fun saveNoteOpenMode(mode: NotesOpenMode) {
        prefs.edit().putString(KEY_NOTE_OPEN_MODE, mode.name).apply()
    }

    fun loadPreviewFontSizeSp(): Int = prefs
        .getInt(KEY_PREVIEW_FONT_SIZE_SP, DEFAULT_PREVIEW_FONT_SIZE_SP)
        .coerceIn(AppPreferences.MIN_FONT_SIZE_SP, AppPreferences.MAX_FONT_SIZE_SP)

    fun savePreviewFontSizeSp(value: Int) {
        prefs
            .edit()
            .putInt(
                KEY_PREVIEW_FONT_SIZE_SP,
                value.coerceIn(AppPreferences.MIN_FONT_SIZE_SP, AppPreferences.MAX_FONT_SIZE_SP),
            ).apply()
    }

    fun loadEditorFontSizeSp(): Int = prefs
        .getInt(KEY_EDITOR_FONT_SIZE_SP, DEFAULT_EDITOR_FONT_SIZE_SP)
        .coerceIn(AppPreferences.MIN_FONT_SIZE_SP, AppPreferences.MAX_FONT_SIZE_SP)

    fun saveEditorFontSizeSp(value: Int) {
        prefs
            .edit()
            .putInt(
                KEY_EDITOR_FONT_SIZE_SP,
                value.coerceIn(AppPreferences.MIN_FONT_SIZE_SP, AppPreferences.MAX_FONT_SIZE_SP),
            ).apply()
    }

    fun loadMaxOpenTabs(): Int = prefs.getInt(KEY_MAX_OPEN_TABS, DEFAULT_MAX_OPEN_TABS).coerceIn(MIN_OPEN_TABS, MAX_OPEN_TABS)

    fun saveMaxOpenTabs(value: Int) {
        prefs.edit().putInt(KEY_MAX_OPEN_TABS, value.coerceIn(MIN_OPEN_TABS, MAX_OPEN_TABS)).apply()
    }

    fun loadPinnedBarEnabled(): Boolean = prefs.getBoolean(KEY_PINNED_BAR_ENABLED, DEFAULT_PINNED_BAR_ENABLED)

    fun savePinnedBarEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PINNED_BAR_ENABLED, enabled).apply()
    }

    fun loadMaxPinnedItems(): Int = prefs
        .getInt(KEY_MAX_PINNED_ITEMS, DEFAULT_MAX_PINNED_ITEMS)
        .coerceIn(MIN_PINNED_ITEMS, MAX_PINNED_ITEMS)

    fun saveMaxPinnedItems(value: Int) {
        prefs.edit().putInt(KEY_MAX_PINNED_ITEMS, value.coerceIn(MIN_PINNED_ITEMS, MAX_PINNED_ITEMS)).apply()
    }

    fun loadPinnedItemsStore(): NotesPinnedItemsStore {
        val raw = prefs.getString(KEY_PINNED_ITEMS, null) ?: return NotesPinnedItemsStore.empty()
        return NotesPinnedItemsStore.fromJson(raw) ?: NotesPinnedItemsStore.empty()
    }

    fun savePinnedItemsStore(store: NotesPinnedItemsStore) {
        prefs.edit().putString(KEY_PINNED_ITEMS, store.toJson()).apply()
    }

    fun loadPinnedItems(
        treeUri: String?,
        root: NotesPathSegment?,
    ): List<NotesPinnedItem> {
        if (treeUri.isNullOrBlank() || root == null) {
            return emptyList()
        }
        val stored = loadPinnedItemsStore().itemsFor(treeUri)
        if (stored != null) {
            return stored.take(loadMaxPinnedItems())
        }
        return listOf(NotesPinnedItemsStore.defaultHome(root))
    }

    fun savePinnedItems(
        treeUri: String,
        items: List<NotesPinnedItem>,
    ) {
        val limited = items.take(loadMaxPinnedItems())
        val store = loadPinnedItemsStore().withItems(treeUri, limited)
        savePinnedItemsStore(store)
    }

    fun loadOpenTabsSession(treeUri: String?): NotesOpenTabsSession {
        if (treeUri.isNullOrBlank()) {
            return NotesOpenTabsSession(treeUri = "", selectedDocumentId = null, tabs = emptyList())
        }
        val raw = prefs.getString(KEY_OPEN_TABS_SESSION, null) ?: return emptySession(treeUri)
        val session = NotesOpenTabsSession.fromJson(raw) ?: return emptySession(treeUri)
        if (session.treeUri != treeUri) {
            return emptySession(treeUri)
        }
        return session
    }

    fun saveOpenTabsSession(
        treeUri: String,
        tabs: List<OpenNoteTab>,
        selectedDocumentId: String?,
    ) {
        val session =
            NotesOpenTabsSession(
                treeUri = treeUri,
                selectedDocumentId = selectedDocumentId,
                tabs = tabs,
            )
        prefs.edit().putString(KEY_OPEN_TABS_SESSION, session.toJson()).apply()
    }

    fun clearOpenTabsSession() {
        prefs.edit().remove(KEY_OPEN_TABS_SESSION).apply()
    }

    /**
     * Restores viewer preferences to defaults.
     * Keeps the chosen notes folder URI ([KEY_NOTES_TREE_URI]).
     */
    fun resetSettingsToDefaults() {
        prefs
            .edit()
            .remove(KEY_LIST_DENSITY)
            .remove(KEY_TREE_DENSITY)
            .remove(KEY_PINNED_BAR_DENSITY)
            .remove(KEY_BROWSE_LAYOUT)
            .remove(KEY_TITLE_SOURCE)
            .remove(KEY_NOTE_OPEN_MODE)
            .remove(KEY_PREVIEW_FONT_SIZE_SP)
            .remove(KEY_EDITOR_FONT_SIZE_SP)
            .remove(KEY_MAX_OPEN_TABS)
            .remove(KEY_OPEN_TABS_SESSION)
            .remove(KEY_PINNED_BAR_ENABLED)
            .remove(KEY_MAX_PINNED_ITEMS)
            .remove(KEY_PINNED_ITEMS)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "notes_viewer"
        private const val KEY_NOTES_TREE_URI = "notes_tree_uri"
        private const val KEY_LIST_DENSITY = "list_density"
        private const val KEY_TREE_DENSITY = "tree_density"
        private const val KEY_PINNED_BAR_DENSITY = "pinned_bar_density"
        private const val KEY_BROWSE_LAYOUT = "browse_layout"
        private const val KEY_TITLE_SOURCE = "title_source"
        private const val KEY_NOTE_OPEN_MODE = "note_open_mode"
        private const val KEY_PREVIEW_FONT_SIZE_SP = "preview_font_size_sp"
        private const val KEY_EDITOR_FONT_SIZE_SP = "editor_font_size_sp"
        private const val KEY_MAX_OPEN_TABS = "max_open_tabs"
        private const val KEY_OPEN_TABS_SESSION = "open_tabs_session"
        private const val KEY_PINNED_BAR_ENABLED = "pinned_bar_enabled"
        private const val KEY_MAX_PINNED_ITEMS = "max_pinned_items"
        private const val KEY_PINNED_ITEMS = "pinned_items"

        const val DEFAULT_MAX_OPEN_TABS = 10
        const val MIN_OPEN_TABS = 1
        const val MAX_OPEN_TABS = 50

        const val DEFAULT_PINNED_BAR_ENABLED = true
        const val DEFAULT_MAX_PINNED_ITEMS = 5
        const val MIN_PINNED_ITEMS = 1
        const val MAX_PINNED_ITEMS = 20

        const val DEFAULT_PREVIEW_FONT_SIZE_SP = 14
        const val DEFAULT_EDITOR_FONT_SIZE_SP = 14

        private fun emptySession(treeUri: String) = NotesOpenTabsSession(
            treeUri = treeUri,
            selectedDocumentId = null,
            tabs = emptyList(),
        )
    }
}

/** Persist read/write access to a notes folder chosen via SAF. */
fun takeNotesFolderPermission(
    context: Context,
    treeUri: Uri,
) {
    val flags =
        Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    context.contentResolver.takePersistableUriPermission(treeUri, flags)
}

/** Human-readable label for a stored notes tree URI. */
fun notesFolderDisplayName(
    context: Context,
    treeUriString: String,
): String {
    val treeUri = Uri.parse(treeUriString)
    val docId =
        runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
            ?: return treeUri.lastPathSegment ?: treeUriString
    val name = docId.substringAfterLast(':', missingDelimiterValue = docId)
    return name.ifBlank {
        context.getString(dev.harrix.notes.R.string.markdown_notes_path_unknown)
    }
}

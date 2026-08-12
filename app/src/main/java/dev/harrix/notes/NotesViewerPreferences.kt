package dev.harrix.notes

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import org.json.JSONArray
import org.json.JSONObject

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

    fun loadIconStyle(): NotesIconStyle = NotesIconStyle.fromStorageKey(prefs.getString(KEY_ICON_STYLE, null))

    fun saveIconStyle(style: NotesIconStyle) {
        prefs.edit().putString(KEY_ICON_STYLE, style.name).apply()
    }

    fun loadTitleSource(): NotesTitleSource = NotesTitleSource.fromStorageKey(prefs.getString(KEY_TITLE_SOURCE, null))

    fun saveTitleSource(source: NotesTitleSource) {
        prefs.edit().putString(KEY_TITLE_SOURCE, source.name).apply()
    }

    fun loadNoteOpenMode(): NotesOpenMode = NotesOpenMode.fromStorageKey(prefs.getString(KEY_NOTE_OPEN_MODE, null))

    fun saveNoteOpenMode(mode: NotesOpenMode) {
        prefs.edit().putString(KEY_NOTE_OPEN_MODE, mode.name).apply()
    }

    /** Last canvas pen color (ARGB), or null if the user has never drawn yet. */
    fun loadCanvasPenColorArgb(): Int? = if (prefs.contains(KEY_CANVAS_PEN_COLOR_ARGB)) {
        prefs.getInt(KEY_CANVAS_PEN_COLOR_ARGB, 0)
    } else {
        null
    }

    fun saveCanvasPenColorArgb(colorArgb: Int) {
        prefs.edit().putInt(KEY_CANVAS_PEN_COLOR_ARGB, colorArgb).apply()
    }

    fun loadCanvasPenWidth(): Float = prefs
        .getFloat(KEY_CANVAS_PEN_WIDTH, DEFAULT_CANVAS_PEN_WIDTH)
        .coerceIn(MIN_CANVAS_PEN_WIDTH, MAX_CANVAS_PEN_WIDTH)

    fun saveCanvasPenWidth(width: Float) {
        prefs
            .edit()
            .putFloat(
                KEY_CANVAS_PEN_WIDTH,
                width.coerceIn(MIN_CANVAS_PEN_WIDTH, MAX_CANVAS_PEN_WIDTH),
            ).apply()
    }

    fun loadCanvasPaperMode(): CanvasPaperMode = CanvasPaperMode.fromStorageKey(prefs.getString(KEY_CANVAS_PAPER_MODE, null))

    fun saveCanvasPaperMode(mode: CanvasPaperMode) {
        prefs.edit().putString(KEY_CANVAS_PAPER_MODE, mode.yamlKey).apply()
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

    fun loadPreviewFont(): NotesContentFont = NotesContentFont.fromStorageKey(prefs.getString(KEY_PREVIEW_FONT, null))

    fun savePreviewFont(font: NotesContentFont) {
        prefs.edit().putString(KEY_PREVIEW_FONT, font.storageKey).apply()
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

    fun loadEditorFont(): NotesContentFont = NotesContentFont.fromStorageKey(prefs.getString(KEY_EDITOR_FONT, null))

    fun saveEditorFont(font: NotesContentFont) {
        prefs.edit().putString(KEY_EDITOR_FONT, font.storageKey).apply()
    }

    /**
     * Max note size (MiB) that still gets Markdown syntax highlighting in the
     * editor. `0` disables highlighting for every note.
     */
    fun loadHighlightMaxMb(): Int = prefs
        .getInt(KEY_HIGHLIGHT_MAX_MB, DEFAULT_HIGHLIGHT_MAX_MB)
        .coerceIn(MIN_HIGHLIGHT_MAX_MB, MAX_HIGHLIGHT_MAX_MB)

    fun saveHighlightMaxMb(value: Int) {
        prefs
            .edit()
            .putInt(KEY_HIGHLIGHT_MAX_MB, value.coerceIn(MIN_HIGHLIGHT_MAX_MB, MAX_HIGHLIGHT_MAX_MB))
            .apply()
    }

    fun loadMaxOpenTabs(): Int = prefs.getInt(KEY_MAX_OPEN_TABS, DEFAULT_MAX_OPEN_TABS).coerceIn(MIN_OPEN_TABS, MAX_OPEN_TABS)

    fun saveMaxOpenTabs(value: Int) {
        prefs.edit().putInt(KEY_MAX_OPEN_TABS, value.coerceIn(MIN_OPEN_TABS, MAX_OPEN_TABS)).apply()
    }

    /** When true, only one note tab may be open; opening another closes the previous. */
    fun loadSingleNoteMode(): Boolean = prefs.getBoolean(KEY_SINGLE_NOTE_MODE, DEFAULT_SINGLE_NOTE_MODE)

    fun saveSingleNoteMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SINGLE_NOTE_MODE, enabled).apply()
    }

    /**
     * When true, wide landscape devices show editor and preview side by side
     * (tablets / foldables).
     */
    fun loadDualPaneEnabled(): Boolean = prefs.getBoolean(KEY_DUAL_PANE_ENABLED, DEFAULT_DUAL_PANE_ENABLED)

    fun saveDualPaneEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DUAL_PANE_ENABLED, enabled).apply()
    }

    /** When true, the folder list shows last-modified dates under titles. */
    fun loadShowNoteDates(): Boolean = prefs.getBoolean(KEY_SHOW_NOTE_DATES, DEFAULT_SHOW_NOTE_DATES)

    fun saveShowNoteDates(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_NOTE_DATES, enabled).apply()
    }

    /** When true, the full path of the open note is shown above the pinned bar. */
    fun loadShowNotePath(): Boolean = prefs.getBoolean(KEY_SHOW_NOTE_PATH, DEFAULT_SHOW_NOTE_PATH)

    fun saveShowNotePath(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_NOTE_PATH, enabled).apply()
    }

    fun loadSortBy(): NotesSortBy = NotesSortBy.fromStorageKey(prefs.getString(KEY_SORT_BY, null))

    fun saveSortBy(sortBy: NotesSortBy) {
        prefs.edit().putString(KEY_SORT_BY, sortBy.name).apply()
    }

    fun loadFoldersFirst(): Boolean = prefs.getBoolean(KEY_FOLDERS_FIRST, DEFAULT_FOLDERS_FIRST)

    fun saveFoldersFirst(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FOLDERS_FIRST, enabled).apply()
    }

    fun loadSortReverseOrder(): Boolean = prefs.getBoolean(KEY_SORT_REVERSE_ORDER, DEFAULT_SORT_REVERSE_ORDER)

    fun saveSortReverseOrder(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SORT_REVERSE_ORDER, enabled).apply()
    }

    fun loadShowGmdFiles(): Boolean = prefs.getBoolean(KEY_SHOW_GMD_FILES, DEFAULT_SHOW_GMD_FILES)

    fun saveShowGmdFiles(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_GMD_FILES, enabled).apply()
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

    fun loadMaxRecentNotes(): Int = prefs
        .getInt(KEY_MAX_RECENT_NOTES, DEFAULT_MAX_RECENT_NOTES)
        .coerceIn(MIN_RECENT_NOTES, MAX_RECENT_NOTES)

    fun saveMaxRecentNotes(value: Int) {
        prefs.edit().putInt(KEY_MAX_RECENT_NOTES, value.coerceIn(MIN_RECENT_NOTES, MAX_RECENT_NOTES)).apply()
    }

    fun loadRecentItemsStore(): NotesRecentItemsStore {
        val raw = prefs.getString(KEY_RECENT_ITEMS, null) ?: return NotesRecentItemsStore.empty()
        return NotesRecentItemsStore.fromJson(raw) ?: NotesRecentItemsStore.empty()
    }

    fun saveRecentItemsStore(store: NotesRecentItemsStore) {
        prefs.edit().putString(KEY_RECENT_ITEMS, store.toJson()).apply()
    }

    fun loadRecentItems(treeUri: String?): List<NotesRecentItem> {
        if (treeUri.isNullOrBlank()) {
            return emptyList()
        }
        return loadRecentItemsStore().itemsFor(treeUri).orEmpty().take(loadMaxRecentNotes())
    }

    fun saveRecentItems(
        treeUri: String,
        items: List<NotesRecentItem>,
    ) {
        val limited = items.take(loadMaxRecentNotes())
        val store = loadRecentItemsStore().withItems(treeUri, limited)
        saveRecentItemsStore(store)
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

    // @hsk-sync:new-note — personal data + beginning templates
    fun loadPersonalDataEnabled(): Boolean = prefs.getBoolean(KEY_PERSONAL_DATA_ENABLED, false)

    fun savePersonalDataEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PERSONAL_DATA_ENABLED, enabled).apply()
    }

    fun loadPersonalDataAuthor(): String = prefs.getString(KEY_PERSONAL_DATA_AUTHOR, DEFAULT_PERSONAL_DATA_AUTHOR)
        ?: DEFAULT_PERSONAL_DATA_AUTHOR

    fun savePersonalDataAuthor(author: String) {
        prefs.edit().putString(KEY_PERSONAL_DATA_AUTHOR, author).apply()
    }

    fun loadPersonalDataAuthorEmail(): String = prefs.getString(KEY_PERSONAL_DATA_AUTHOR_EMAIL, "") ?: ""

    fun savePersonalDataAuthorEmail(email: String) {
        prefs.edit().putString(KEY_PERSONAL_DATA_AUTHOR_EMAIL, email).apply()
    }

    fun loadPersonalData(): NewNoteContent.PersonalData = NewNoteContent.PersonalData(
        enabled = loadPersonalDataEnabled(),
        author = loadPersonalDataAuthor().ifBlank { DEFAULT_PERSONAL_DATA_AUTHOR },
        authorEmail = loadPersonalDataAuthorEmail(),
    )

    fun loadBeginningTemplates(): List<NewNoteContent.BeginningTemplate> {
        val raw = prefs.getString(KEY_BEGINNING_TEMPLATES, null)
        if (raw.isNullOrBlank()) {
            return NewNoteContent.defaultBeginningTemplates
        }
        return runCatching { parseBeginningTemplatesJson(raw) }.getOrElse {
            NewNoteContent.defaultBeginningTemplates
        }
    }

    fun saveBeginningTemplates(templates: List<NewNoteContent.BeginningTemplate>) {
        prefs.edit().putString(KEY_BEGINNING_TEMPLATES, beginningTemplatesToJson(templates)).apply()
    }

    fun loadDefaultBeginningTemplateId(): String {
        val stored = prefs.getString(KEY_DEFAULT_BEGINNING_TEMPLATE_ID, null)
        if (!stored.isNullOrBlank()) {
            return stored
        }
        return NewNoteContent.defaultBeginningTemplates.first().id
    }

    fun saveDefaultBeginningTemplateId(id: String) {
        prefs.edit().putString(KEY_DEFAULT_BEGINNING_TEMPLATE_ID, id).apply()
    }

    fun resolveBeginningTemplate(templateId: String? = null): NewNoteContent.BeginningTemplate {
        val templates = loadBeginningTemplates()
        val wanted = templateId?.takeIf { it.isNotBlank() } ?: loadDefaultBeginningTemplateId()
        return templates.firstOrNull { it.id == wanted || it.label == wanted }
            ?: templates.firstOrNull()
            ?: NewNoteContent.defaultBeginningTemplates.first()
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
            .remove(KEY_ICON_STYLE)
            .remove(KEY_TITLE_SOURCE)
            .remove(KEY_NOTE_OPEN_MODE)
            .remove(KEY_PREVIEW_FONT_SIZE_SP)
            .remove(KEY_PREVIEW_FONT)
            .remove(KEY_EDITOR_FONT_SIZE_SP)
            .remove(KEY_EDITOR_FONT)
            .remove(KEY_HIGHLIGHT_MAX_MB)
            .remove(KEY_MAX_OPEN_TABS)
            .remove(KEY_SINGLE_NOTE_MODE)
            .remove(KEY_DUAL_PANE_ENABLED)
            .remove(KEY_SHOW_NOTE_DATES)
            .remove(KEY_SHOW_NOTE_PATH)
            .remove(KEY_SORT_BY)
            .remove(KEY_FOLDERS_FIRST)
            .remove(KEY_SORT_REVERSE_ORDER)
            .remove(KEY_SHOW_GMD_FILES)
            .remove(KEY_OPEN_TABS_SESSION)
            .remove(KEY_PINNED_BAR_ENABLED)
            .remove(KEY_MAX_PINNED_ITEMS)
            .remove(KEY_PINNED_ITEMS)
            .remove(KEY_MAX_RECENT_NOTES)
            .remove(KEY_RECENT_ITEMS)
            .remove(KEY_PERSONAL_DATA_ENABLED)
            .remove(KEY_PERSONAL_DATA_AUTHOR)
            .remove(KEY_PERSONAL_DATA_AUTHOR_EMAIL)
            .remove(KEY_BEGINNING_TEMPLATES)
            .remove(KEY_DEFAULT_BEGINNING_TEMPLATE_ID)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "notes_viewer"
        private const val KEY_NOTES_TREE_URI = "notes_tree_uri"
        private const val KEY_LIST_DENSITY = "list_density"
        private const val KEY_TREE_DENSITY = "tree_density"
        private const val KEY_PINNED_BAR_DENSITY = "pinned_bar_density"
        private const val KEY_BROWSE_LAYOUT = "browse_layout"
        private const val KEY_ICON_STYLE = "icon_style"
        private const val KEY_TITLE_SOURCE = "title_source"
        private const val KEY_NOTE_OPEN_MODE = "note_open_mode"
        private const val KEY_PREVIEW_FONT_SIZE_SP = "preview_font_size_sp"
        private const val KEY_EDITOR_FONT_SIZE_SP = "editor_font_size_sp"
        private const val KEY_HIGHLIGHT_MAX_MB = "highlight_max_mb"
        private const val KEY_MAX_OPEN_TABS = "max_open_tabs"
        private const val KEY_SINGLE_NOTE_MODE = "single_note_mode"
        private const val KEY_DUAL_PANE_ENABLED = "dual_pane_enabled"
        private const val KEY_SHOW_NOTE_DATES = "show_note_dates"
        private const val KEY_SHOW_NOTE_PATH = "show_note_path"
        private const val KEY_SORT_BY = "sort_by"
        private const val KEY_FOLDERS_FIRST = "folders_first"
        private const val KEY_SORT_REVERSE_ORDER = "sort_reverse_order"
        private const val KEY_SHOW_GMD_FILES = "show_gmd_files"
        private const val KEY_OPEN_TABS_SESSION = "open_tabs_session"
        private const val KEY_PINNED_BAR_ENABLED = "pinned_bar_enabled"
        private const val KEY_MAX_PINNED_ITEMS = "max_pinned_items"
        private const val KEY_PINNED_ITEMS = "pinned_items"
        private const val KEY_MAX_RECENT_NOTES = "max_recent_notes"
        private const val KEY_RECENT_ITEMS = "recent_items"
        private const val KEY_PERSONAL_DATA_ENABLED = "personal_data_enabled"
        private const val KEY_PERSONAL_DATA_AUTHOR = "personal_data_author"
        private const val KEY_PERSONAL_DATA_AUTHOR_EMAIL = "personal_data_author_email"
        private const val KEY_BEGINNING_TEMPLATES = "beginning_templates_json"
        private const val KEY_DEFAULT_BEGINNING_TEMPLATE_ID = "default_beginning_template_id"
        private const val KEY_CANVAS_PEN_COLOR_ARGB = "canvas_pen_color_argb"
        private const val KEY_CANVAS_PEN_WIDTH = "canvas_pen_width"
        private const val KEY_CANVAS_PAPER_MODE = "canvas_paper_mode"
        private const val KEY_PREVIEW_FONT = "preview_font"
        private const val KEY_EDITOR_FONT = "editor_font"

        const val DEFAULT_PERSONAL_DATA_AUTHOR = "noname"
        const val DEFAULT_CANVAS_PEN_WIDTH = 12f
        const val MIN_CANVAS_PEN_WIDTH = 2f
        const val MAX_CANVAS_PEN_WIDTH = 28f

        private fun beginningTemplatesToJson(templates: List<NewNoteContent.BeginningTemplate>): String {
            val array = JSONArray()
            for (template in templates) {
                array.put(
                    JSONObject()
                        .put("id", template.id)
                        .put("label", template.label)
                        .put("content", template.content),
                )
            }
            return array.toString()
        }

        private fun parseBeginningTemplatesJson(raw: String): List<NewNoteContent.BeginningTemplate> {
            val array = JSONArray(raw)
            val result = mutableListOf<NewNoteContent.BeginningTemplate>()
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val id = obj.optString("id").trim()
                val label = obj.optString("label").trim().ifEmpty { id }
                val content = obj.optString("content")
                if (id.isEmpty() && content.isEmpty()) {
                    continue
                }
                result +=
                    NewNoteContent.BeginningTemplate(
                        id = id.ifEmpty { label },
                        label = label.ifEmpty { id },
                        content = content,
                    )
            }
            return result.ifEmpty { NewNoteContent.defaultBeginningTemplates }
        }

        const val DEFAULT_MAX_OPEN_TABS = 10
        const val MIN_OPEN_TABS = 1
        const val MAX_OPEN_TABS = 50

        const val DEFAULT_SINGLE_NOTE_MODE = false

        const val DEFAULT_DUAL_PANE_ENABLED = false

        const val DEFAULT_SHOW_NOTE_DATES = false

        const val DEFAULT_SHOW_NOTE_PATH = false

        const val DEFAULT_FOLDERS_FIRST = false
        const val DEFAULT_SORT_REVERSE_ORDER = false
        const val DEFAULT_SHOW_GMD_FILES = false

        const val DEFAULT_PINNED_BAR_ENABLED = true
        const val DEFAULT_MAX_PINNED_ITEMS = 5
        const val MIN_PINNED_ITEMS = 1
        const val MAX_PINNED_ITEMS = 20

        const val DEFAULT_MAX_RECENT_NOTES = 10
        const val MIN_RECENT_NOTES = 1
        const val MAX_RECENT_NOTES = 50

        const val DEFAULT_PREVIEW_FONT_SIZE_SP = 14
        const val DEFAULT_EDITOR_FONT_SIZE_SP = 14

        const val DEFAULT_HIGHLIGHT_MAX_MB = 8
        const val MIN_HIGHLIGHT_MAX_MB = 0
        const val MAX_HIGHLIGHT_MAX_MB = 64
        private const val BYTES_PER_MIB = 1024 * 1024

        /** Character budget matching [loadHighlightMaxMb] for the editor bridge. */
        fun highlightMaxChars(maxMb: Int): Int {
            val mb = maxMb.coerceIn(MIN_HIGHLIGHT_MAX_MB, MAX_HIGHLIGHT_MAX_MB)
            return mb * BYTES_PER_MIB
        }

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

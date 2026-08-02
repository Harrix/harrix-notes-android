package dev.harrix.notes

/**
 * Mode used when opening an existing note (new notes still open in the editor).
 */
enum class NotesOpenMode {
    /** Read-only HTML preview (`NotesHtmlPreviewPane`). */
    Preview,

    /** CodeMirror editor (`NotesMarkdownEditorPane`). */
    Edit,
    ;

    companion object {
        val Default: NotesOpenMode = Preview

        fun fromStorageKey(key: String?): NotesOpenMode = entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: Default
    }
}

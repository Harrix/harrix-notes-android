package dev.harrix.notes

/**
 * Icon set used for folders and notes in the browser, drawer, and pinned bar.
 */
enum class NotesIconStyle {
    /** Colored Harrix Vector Icons (`it__folder_01` / `it__file-text_01`). */
    Harrix,

    /** Material Icons (previous default look). */
    Material,
    ;

    companion object {
        val Default: NotesIconStyle = Harrix

        fun fromStorageKey(key: String?): NotesIconStyle =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: Default
    }
}

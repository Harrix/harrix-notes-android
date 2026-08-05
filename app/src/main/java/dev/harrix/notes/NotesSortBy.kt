package dev.harrix.notes

/**
 * Sort key for the notes folder browser (list and icons).
 */
enum class NotesSortBy {
    Name,
    Date,
    Size,
    ;

    companion object {
        val Default: NotesSortBy = Name

        fun fromStorageKey(key: String?): NotesSortBy =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: Default
    }
}

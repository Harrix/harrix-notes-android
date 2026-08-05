package dev.harrix.notes

/**
 * Filters and sorts folder-browser entries (list, icons, drawer tree children).
 */
object NotesListingOptions {
    fun apply(
        entries: List<NotesEntry>,
        sortBy: NotesSortBy,
        foldersFirst: Boolean,
        reverseOrder: Boolean,
        showGmdFiles: Boolean,
    ): List<NotesEntry> {
        val filtered =
            if (showGmdFiles) {
                entries
            } else {
                entries.filterNot { entry ->
                    entry is NotesEntry.Note && NotesTreeRepository.isGMd(entry.name)
                }
            }
        val byField =
            when (sortBy) {
                NotesSortBy.Name ->
                    compareBy(String.CASE_INSENSITIVE_ORDER) { entry: NotesEntry -> entry.sortLabel }

                NotesSortBy.Date ->
                    compareBy<NotesEntry> { entry -> entry.lastModifiedOrZero() }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.sortLabel }

                NotesSortBy.Size ->
                    compareBy<NotesEntry> { entry -> entry.sizeOrZero() }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.sortLabel }
            }
        val ordered = if (reverseOrder) byField.reversed() else byField
        return if (foldersFirst) {
            filtered.sortedWith(
                compareBy<NotesEntry> { entry -> entry !is NotesEntry.Folder }.then(ordered),
            )
        } else {
            filtered.sortedWith(ordered)
        }
    }

    private fun NotesEntry.lastModifiedOrZero(): Long = when (this) {
        is NotesEntry.Folder -> lastModifiedEpochMs ?: 0L
        is NotesEntry.Note -> lastModifiedEpochMs ?: 0L
    }

    private fun NotesEntry.sizeOrZero(): Long = when (this) {
        is NotesEntry.Folder -> sizeBytes ?: 0L
        is NotesEntry.Note -> sizeBytes ?: 0L
    }
}

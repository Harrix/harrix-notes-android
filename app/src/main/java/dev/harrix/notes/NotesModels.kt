package dev.harrix.notes

import android.net.Uri

/** A folder or note row in the Markdown Notes browser. */
sealed class NotesEntry {
    abstract val documentId: String
    abstract val name: String
    abstract val uri: Uri
    abstract val sortLabel: String

    data class Folder(
        override val documentId: String,
        override val name: String,
        override val uri: Uri,
        val hasMergedNote: Boolean,
        val mergedNoteDocumentId: String?,
        val mergedNoteUri: Uri?,
        /** Last modified time from the document provider, if known. */
        val lastModifiedEpochMs: Long? = null,
        /** Document size in bytes from the provider, if known (often unset for folders). */
        val sizeBytes: Long? = null,
    ) : NotesEntry() {
        override val sortLabel: String get() = name
    }

    data class Note(
        override val documentId: String,
        override val name: String,
        override val uri: Uri,
        val displayLabel: String,
        /** YAML `icon:` emoji when resolved; empty until background meta resolve finishes. */
        val displayIcon: String = "",
        /**
         * When this row is a collapsed `Folder/Folder.md` listing entry, the folder that
         * actually contains the note (and typically `img/`). Callers must append this to
         * the listing path so relative assets resolve.
         */
        val containingFolder: NotesPathSegment? = null,
        /** Last modified time from the document provider, if known. */
        val lastModifiedEpochMs: Long? = null,
        /** Document size in bytes from the provider, if known. */
        val sizeBytes: Long? = null,
    ) : NotesEntry() {
        override val sortLabel: String get() = displayLabel
    }
}

/**
 * Listing/tree [path] plus [NotesEntry.Note.containingFolder] when the note was collapsed
 * from a same-name folder.
 */
fun noteAssetFolderPath(
    path: List<NotesPathSegment>,
    note: NotesEntry.Note,
): List<NotesPathSegment> {
    val containing = note.containingFolder ?: return path
    if (path.lastOrNull()?.documentId == containing.documentId) {
        return path
    }
    return path + containing
}

/** Title and/or icon updates from a background note-prefix read. */
data class NoteMetaUpdates(
    val titles: Map<String, String> = emptyMap(),
    val icons: Map<String, String> = emptyMap(),
) {
    val isEmpty: Boolean get() = titles.isEmpty() && icons.isEmpty()
}

/** Metadata for the note info dialog (SAF / content URI query). */
data class NotesDocumentInfo(
    val displayName: String,
    val sizeBytes: Long? = null,
    val lastModifiedEpochMs: Long? = null,
    val mimeType: String? = null,
)

/** One segment in the folder navigation / breadcrumb path. */
data class NotesPathSegment(
    val documentId: String,
    val name: String,
    val uri: Uri,
)

/** An open note tab in the viewer. */
data class OpenNoteTab(
    val documentId: String,
    val uri: Uri,
    val title: String,
    /** Original document file name (e.g. `Note.md`), used when titles come from file names. */
    val fileName: String = "",
    /** Path from notes root through parent folders (excludes the note itself). */
    val folderPath: List<NotesPathSegment>,
    /**
     * True when the note was opened from outside the configured notes tree
     * (Android “Open with” / Share). External tabs use a distinct chip color.
     */
    val isExternal: Boolean = false,
) {
    /** Tab chip / tabs popup label; year/date titles include the parent folder. */
    val displayTitle: String
        get() = contextualNoteTitle(title, folderPath)
}

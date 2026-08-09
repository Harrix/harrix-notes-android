package dev.harrix.notes

import android.net.Uri

/** In-app clipboard for note/folder Copy and Cut (not the system clipboard). */
enum class NotesClipboardMode {
    Copy,
    Cut,
}

enum class NotesClipboardKind {
    Note,
    Folder,
}

data class NotesClipboardEntry(
    val treeUri: String,
    val documentId: String,
    val uri: Uri,
    val displayName: String,
    val kind: NotesClipboardKind,
    val mode: NotesClipboardMode,
    val sourceParentDocumentId: String,
    /**
     * When the clipboard holds a folder-per-note package (`Name/Name.md` + assets),
     * the original note stem inside that folder. Used to rename the inner `.md`
     * (and `_<Stem>.g.md` if present) after paste under a new folder name.
     */
    val folderPerNoteStem: String? = null,
    /**
     * Document id to clear from pins on cut. For folder-per-note packages this is
     * the note id, while [documentId] is the containing folder.
     */
    val pinDocumentId: String = documentId,
)

/**
 * True when cut / copy / duplicate would break relative links (`.g.md` notes).
 * Delete remains allowed.
 */
fun NotesEntry.blocksClipboardRelocation(): Boolean = this is NotesEntry.Note && NotesTreeRepository.isGMd(name)

/**
 * Document to copy, cut, duplicate, or delete for a browser listing row.
 * Collapsed folder-per-note rows operate on the containing folder so assets
 * (e.g. `img/`) move with the note.
 */
fun NotesEntry.mutationDocument(
    listingParentDocumentId: String,
): NotesMutationDocument = when (this) {
    is NotesEntry.Folder ->
        NotesMutationDocument(
            documentId = documentId,
            uri = uri,
            displayName = name,
            kind = NotesClipboardKind.Folder,
            sourceParentDocumentId = listingParentDocumentId,
            folderPerNoteStem = null,
            pinDocumentId = documentId,
        )

    is NotesEntry.Note -> {
        val folder = containingFolder
        if (folder != null) {
            NotesMutationDocument(
                documentId = folder.documentId,
                uri = folder.uri,
                displayName = folder.name,
                kind = NotesClipboardKind.Folder,
                sourceParentDocumentId = listingParentDocumentId,
                folderPerNoteStem = folder.name,
                pinDocumentId = documentId,
            )
        } else {
            NotesMutationDocument(
                documentId = documentId,
                uri = uri,
                displayName = name,
                kind = NotesClipboardKind.Note,
                sourceParentDocumentId = listingParentDocumentId,
                folderPerNoteStem = null,
                pinDocumentId = documentId,
            )
        }
    }
}

data class NotesMutationDocument(
    val documentId: String,
    val uri: Uri,
    val displayName: String,
    val kind: NotesClipboardKind,
    val sourceParentDocumentId: String,
    val folderPerNoteStem: String?,
    val pinDocumentId: String,
) {
    val isDirectory: Boolean get() = kind == NotesClipboardKind.Folder

    fun toClipboardEntry(
        treeUri: String,
        mode: NotesClipboardMode,
    ): NotesClipboardEntry = NotesClipboardEntry(
        treeUri = treeUri,
        documentId = documentId,
        uri = uri,
        displayName = displayName,
        kind = kind,
        mode = mode,
        sourceParentDocumentId = sourceParentDocumentId,
        folderPerNoteStem = folderPerNoteStem,
        pinDocumentId = pinDocumentId,
    )
}

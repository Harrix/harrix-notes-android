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
)

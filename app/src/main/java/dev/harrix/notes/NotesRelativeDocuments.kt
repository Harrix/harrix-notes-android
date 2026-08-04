package dev.harrix.notes

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract

/**
 * Resolves note-relative paths (for preview images) against the SAF tree.
 *
 * - `foo/bar.png`, `./foo.png` — relative to the note's parent folder
 * - `/foo/bar.png` — from the notes tree root
 * - `../` segments walk up toward the root
 *
 * Prefer [noteDocumentId] so merged notes (`_Folder.g.md` inside `Folder/`) resolve
 * `img/` against `Folder/`, not the parent listing path.
 */
object NotesRelativeDocuments {
    fun resolve(
        resolver: ContentResolver,
        treeUri: Uri,
        folderPath: List<NotesPathSegment>,
        relativePath: String,
        noteDocumentId: String? = null,
    ): Uri? {
        val raw = Uri.decode(relativePath.trim()).replace('\\', '/')
        val fromRoot = raw.startsWith("/")
        val parts =
            raw
                .split('/')
                .filter { it.isNotEmpty() && it != "." }
        if (parts.isEmpty()) {
            return null
        }
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        var currentDirId =
            when {
                fromRoot -> rootId

                else ->
                    noteParentDocumentId(noteDocumentId)
                        ?: folderPath.lastOrNull()?.documentId
                        ?: rootId
            }
        for (part in parts.dropLast(1)) {
            if (part == "..") {
                currentDirId = parentTreeDocumentId(currentDirId) ?: rootId
                continue
            }
            currentDirId =
                findChildDocumentId(resolver, treeUri, currentDirId, part, wantDirectory = true)
                    ?: return null
        }
        val fileName = parts.last()
        if (fileName == "..") {
            return null
        }
        val fileId =
            findChildDocumentId(resolver, treeUri, currentDirId, fileName, wantDirectory = false)
                ?: return null
        return DocumentsContract.buildDocumentUriUsingTree(treeUri, fileId)
    }

    fun readBytes(
        resolver: ContentResolver,
        uri: Uri,
    ): ByteArray? = runCatching {
        resolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull()

    /**
     * Parent document id for a note file id (`primary:a/b/note.md` → `primary:a/b`).
     */
    fun noteParentDocumentId(noteDocumentId: String?): String? {
        if (noteDocumentId.isNullOrBlank()) {
            return null
        }
        return parentTreeDocumentId(noteDocumentId)
    }

    private fun parentTreeDocumentId(documentId: String): String? {
        val slash = documentId.lastIndexOf('/')
        if (slash <= 0) {
            return null
        }
        val colon = documentId.indexOf(':')
        if (colon >= 0 && slash <= colon) {
            return null
        }
        return documentId.substring(0, slash)
    }

    private fun findChildDocumentId(
        resolver: ContentResolver,
        treeUri: Uri,
        parentDocumentId: String,
        name: String,
        wantDirectory: Boolean,
    ): String? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        return runCatching {
            resolver
                .query(
                    childrenUri,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                    ),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    val idIndex =
                        cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIndex =
                        cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIndex =
                        cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    var preferred: String? = null
                    var fallback: String? = null
                    var caseFallback: String? = null
                    while (cursor.moveToNext() && preferred == null) {
                        val childName = cursor.getString(nameIndex) ?: ""
                        val mime = cursor.getString(mimeIndex).orEmpty()
                        val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                        val id = cursor.getString(idIndex)
                        if (id == null) {
                            // skip
                        } else if (childName == name) {
                            if (isDir == wantDirectory) {
                                preferred = id
                            } else if (fallback == null) {
                                fallback = id
                            }
                        } else if (caseFallback == null && childName.equals(name, ignoreCase = true)) {
                            caseFallback = id
                        }
                    }
                    preferred ?: fallback ?: caseFallback
                }
        }.getOrNull()
    }
}

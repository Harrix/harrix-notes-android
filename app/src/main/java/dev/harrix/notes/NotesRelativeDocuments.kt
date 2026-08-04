package dev.harrix.notes

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract

/**
 * Resolves note-relative paths (for preview images) against the SAF tree.
 *
 * - `foo/bar.png`, `./foo.png` — relative to the note parent ([folderPath])
 * - `/foo/bar.png` — from the notes tree root
 * - `../` segments walk up toward the root
 */
object NotesRelativeDocuments {
    fun resolve(
        resolver: ContentResolver,
        treeUri: Uri,
        folderPath: List<NotesPathSegment>,
        relativePath: String,
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
        val stack = ArrayList<String>(folderPath.size + 4)
        if (fromRoot || folderPath.isEmpty()) {
            stack.add(rootId)
        } else {
            folderPath.forEach { stack.add(it.documentId) }
        }
        for (part in parts.dropLast(1)) {
            if (part == "..") {
                if (stack.size > 1) {
                    stack.removeAt(stack.lastIndex)
                }
                continue
            }
            val childId =
                findChildDocumentId(resolver, treeUri, stack.last(), part, wantDirectory = true)
                    ?: return null
            stack.add(childId)
        }
        val fileName = parts.last()
        if (fileName == "..") {
            return null
        }
        val fileId =
            findChildDocumentId(resolver, treeUri, stack.last(), fileName, wantDirectory = false)
                ?: return null
        return DocumentsContract.buildDocumentUriUsingTree(treeUri, fileId)
    }

    fun readBytes(
        resolver: ContentResolver,
        uri: Uri,
    ): ByteArray? = runCatching {
        resolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull()

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
                    while (cursor.moveToNext() && preferred == null) {
                        val childName = cursor.getString(nameIndex)
                        if (childName == name) {
                            val mime = cursor.getString(mimeIndex).orEmpty()
                            val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                            val id = cursor.getString(idIndex)
                            if (id != null) {
                                if (isDir == wantDirectory) {
                                    preferred = id
                                } else if (fallback == null) {
                                    fallback = id
                                }
                            }
                        }
                    }
                    preferred ?: fallback
                }
        }.getOrNull()
    }
}

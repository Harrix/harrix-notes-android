package dev.harrix.notes

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract

/**
 * Resolves note-relative paths (for preview images) against the SAF tree.
 */
object NotesRelativeDocuments {
    fun resolve(
        resolver: ContentResolver,
        treeUri: Uri,
        folderPath: List<NotesPathSegment>,
        relativePath: String,
    ): Uri? {
        val parts =
            relativePath
                .replace('\\', '/')
                .split('/')
                .filter { it.isNotEmpty() && it != "." }
        if (parts.isEmpty()) {
            return null
        }
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val stack = ArrayList<String>(folderPath.size + 4)
        if (folderPath.isEmpty()) {
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
            val parentId = stack.last()
            val childId = findChildDocumentId(resolver, treeUri, parentId, part, directory = true)
                ?: return null
            stack.add(childId)
        }
        val fileName = parts.last()
        if (fileName == "..") {
            return null
        }
        val fileId =
            findChildDocumentId(resolver, treeUri, stack.last(), fileName, directory = false)
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
        directory: Boolean,
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
                    var foundId: String? = null
                    while (cursor.moveToNext() && foundId == null) {
                        val childName = cursor.getString(nameIndex)
                        val mime = cursor.getString(mimeIndex).orEmpty()
                        val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                        if (childName == name && isDir == directory) {
                            foundId = cursor.getString(idIndex)
                        }
                    }
                    foundId
                }
        }.getOrNull()
    }
}

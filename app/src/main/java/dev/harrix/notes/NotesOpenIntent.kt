package dev.harrix.notes

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.util.Locale

/**
 * Resolves a file URI from [Intent.ACTION_VIEW] / [Intent.ACTION_SEND] into an
 * [OpenNoteTab]. Notes under the configured SAF tree open as normal; others as
 * external tabs ([OpenNoteTab.isExternal]).
 */
object NotesOpenIntent {
    private const val EXTERNAL_ID_PREFIX = "external:"

    fun extractUri(intent: Intent?): Uri? {
        if (intent == null) {
            return null
        }
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.data

            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                    ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                @Suppress("DEPRECATION")
                (intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM))?.firstOrNull()
                    ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            }

            else -> intent.data
        }
    }

    fun isLikelyMarkdown(
        context: Context,
        uri: Uri,
        intent: Intent?,
    ): Boolean {
        val type =
            intent?.type
                ?: context.contentResolver.getType(uri)
                ?: ""
        val lowerType = type.lowercase(Locale.ROOT)
        if (lowerType == "text/markdown" ||
            lowerType == "text/x-markdown" ||
            lowerType == "text/plain"
        ) {
            return true
        }
        val name = queryDisplayName(context.contentResolver, uri).orEmpty()
        if (NotesTreeRepository.isMd(name)) {
            return true
        }
        val path = uri.path.orEmpty().lowercase(Locale.ROOT)
        return path.endsWith(".md")
    }

    /**
     * Takes persistable grants when the sender allows them; temporary grants
     * from the intent remain for this activity either way.
     */
    fun takeReadWritePermissionIfPossible(
        context: Context,
        intent: Intent,
        uri: Uri,
    ) {
        val resolver = context.contentResolver
        val takeFlags =
            intent.flags and
                (
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
        if (takeFlags == 0) {
            return
        }
        runCatching {
            resolver.takePersistableUriPermission(uri, takeFlags)
        }
    }

    fun resolveTab(
        context: Context,
        treeUriString: String?,
        fileUri: Uri,
    ): OpenNoteTab {
        val resolver = context.contentResolver
        val displayName =
            queryDisplayName(resolver, fileUri)
                ?: fileUri.lastPathSegment
                    ?.substringAfterLast('/')
                    ?.takeIf { it.isNotBlank() }
                ?: "note.md"
        val treeUri = treeUriString?.takeIf { it.isNotBlank() }?.let(Uri::parse)
        if (treeUri != null) {
            resolveInsideTree(context, resolver, treeUri, fileUri, displayName)?.let { return it }
        }
        return externalTab(fileUri, displayName)
    }

    private fun resolveInsideTree(
        context: Context,
        resolver: ContentResolver,
        treeUri: Uri,
        fileUri: Uri,
        fallbackName: String,
    ): OpenNoteTab? {
        val documentId = documentIdFor(context, fileUri) ?: return null
        val treeId =
            runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
                ?: return null
        if (!isDocumentUnderTree(resolver, treeUri, treeId, documentId, fileUri)) {
            return null
        }
        val noteUri =
            runCatching {
                DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
            }.getOrDefault(fileUri)
        val pathIds =
            runCatching {
                DocumentsContract.findDocumentPath(resolver, noteUri)?.path
            }.getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?: listOf(treeId, documentId).distinct()
        if (pathIds.isEmpty()) {
            return null
        }
        val folderIds = pathIds.dropLast(1)
        val folderPath =
            folderIds.mapNotNull { id ->
                val segmentUri =
                    runCatching {
                        DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                    }.getOrNull() ?: return@mapNotNull null
                NotesPathSegment(
                    documentId = id,
                    name = queryDisplayName(resolver, segmentUri) ?: id.substringAfterLast(':'),
                    uri = segmentUri,
                )
            }
        val fileName = queryDisplayName(resolver, noteUri) ?: fallbackName
        val title = NotesTreeRepository.noteDisplayLabel(fileName)
        return OpenNoteTab(
            documentId = documentId,
            uri = noteUri,
            title = title,
            fileName = fileName,
            folderPath = folderPath,
            isExternal = false,
        )
    }

    private fun isDocumentUnderTree(
        resolver: ContentResolver,
        treeUri: Uri,
        treeId: String,
        documentId: String,
        fileUri: Uri,
    ): Boolean {
        if (documentId == treeId) {
            return false
        }
        if (documentId.startsWith("$treeId/")) {
            return true
        }
        // ExternalStorage-style ids: "primary:Notes/a.md" under tree "primary:Notes"
        if (documentId.startsWith("$treeId%2F", ignoreCase = true)) {
            return true
        }
        val parentUri =
            runCatching {
                DocumentsContract.buildDocumentUriUsingTree(treeUri, treeId)
            }.getOrNull() ?: return false
        val childUri =
            runCatching {
                DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
            }.getOrElse { fileUri }
        return runCatching {
            DocumentsContract.isChildDocument(resolver, parentUri, childUri)
        }.getOrDefault(false)
    }

    private fun documentIdFor(
        context: Context,
        uri: Uri,
    ): String? {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            return runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
        }
        if (DocumentsContract.isTreeUri(uri)) {
            return runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
        }
        // Tree-document URI: .../tree/<treeId>/document/<documentId>
        val segments = uri.pathSegments
        val treeIndex = segments.indexOf("tree")
        val documentIndex = segments.indexOf("document")
        if (treeIndex >= 0 && documentIndex > treeIndex && documentIndex + 1 < segments.size) {
            return Uri.decode(segments[documentIndex + 1])
        }
        return null
    }

    private fun externalTab(
        fileUri: Uri,
        displayName: String,
    ): OpenNoteTab {
        val title = NotesTreeRepository.noteDisplayLabel(displayName)
        return OpenNoteTab(
            documentId = EXTERNAL_ID_PREFIX + fileUri.toString(),
            uri = fileUri,
            title = title,
            fileName = displayName,
            folderPath = emptyList(),
            isExternal = true,
        )
    }

    fun queryDisplayName(
        resolver: ContentResolver,
        uri: Uri,
    ): String? {
        val openable =
            runCatching {
                resolver
                    .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (index >= 0) {
                                cursor.getString(index)?.takeIf { it.isNotBlank() }
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    }
            }.getOrNull()
        if (openable != null) {
            return openable
        }
        return runCatching {
            resolver
                .query(
                    uri,
                    arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index =
                            cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                        if (index >= 0) {
                            cursor.getString(index)?.takeIf { it.isNotBlank() }
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
        }.getOrNull()
    }
}

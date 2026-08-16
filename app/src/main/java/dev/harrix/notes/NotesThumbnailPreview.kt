package dev.harrix.notes

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import java.util.Locale

/**
 * Resolves a Samsung Notes-style thumbnail: featured image, canvas page,
 * first Markdown image, first `img/` file, or a text excerpt.
 *
 * `@hsk-sync:notes-browse`
 */
object NotesThumbnailPreview {
    private val IMAGE_EXT = setOf("png", "jpg", "jpeg", "webp", "avif", "gif")
    private val CANVAS_NAME = Regex("^canvas(?:_\\d{2})?\\.png$", RegexOption.IGNORE_CASE)
    private const val NOTE_PREFIX_BYTES = 16 * 1024
    private const val THUMB_MAX_PX = 512

    data class Loaded(
        val excerpt: String,
        val bitmap: Bitmap?,
    )

    fun load(
        resolver: ContentResolver,
        treeUri: Uri?,
        folderPath: List<NotesPathSegment>,
        note: NotesEntry.Note,
    ): Loaded {
        val markdown = readPrefix(resolver, note.uri)
        val excerpt = NotesMarkdownExcerpt.excerptFromMarkdown(markdown)
        if (treeUri == null) {
            return Loaded(excerpt = excerpt, bitmap = null)
        }
        val imageUri =
            findImageUri(resolver, treeUri, folderPath, note, markdown)
                ?: return Loaded(excerpt = excerpt, bitmap = null)
        return Loaded(excerpt = excerpt, bitmap = decodeDownsampled(resolver, imageUri))
    }

    private fun findImageUri(
        resolver: ContentResolver,
        treeUri: Uri,
        folderPath: List<NotesPathSegment>,
        note: NotesEntry.Note,
        markdown: String,
    ): Uri? {
        val parentId =
            note.containingFolder?.documentId
                ?: NotesRelativeDocuments.noteParentDocumentId(note.documentId)
                ?: folderPath.lastOrNull()?.documentId
                ?: return firstMarkdownUri(resolver, treeUri, folderPath, note, markdown)
        val siblings = listChildren(resolver, treeUri, parentId)
        siblings.firstOrNull { !it.isDirectory && isFeaturedImageName(it.name) }?.let {
            return DocumentsContract.buildDocumentUriUsingTree(treeUri, it.documentId)
        }
        val imgDir = siblings.firstOrNull { it.isDirectory && it.name.equals("img", ignoreCase = true) }
        val images =
            if (imgDir != null) {
                listChildren(resolver, treeUri, imgDir.documentId)
                    .filter { !it.isDirectory && isImageFileName(it.name) }
            } else {
                emptyList()
            }
        images.firstOrNull { CANVAS_NAME.matches(it.name) }?.let {
            return DocumentsContract.buildDocumentUriUsingTree(treeUri, it.documentId)
        }
        firstMarkdownUri(resolver, treeUri, folderPath, note, markdown)?.let { return it }
        images.minByOrNull { it.name.lowercase(Locale.ROOT) }?.let {
            return DocumentsContract.buildDocumentUriUsingTree(treeUri, it.documentId)
        }
        return null
    }

    private fun firstMarkdownUri(
        resolver: ContentResolver,
        treeUri: Uri,
        folderPath: List<NotesPathSegment>,
        note: NotesEntry.Note,
        markdown: String,
    ): Uri? {
        val rel = NotesMarkdownExcerpt.firstMarkdownImageSrc(markdown)
        if (rel.isEmpty()) {
            return null
        }
        return NotesRelativeDocuments.resolve(
            resolver = resolver,
            treeUri = treeUri,
            folderPath = noteAssetFolderPath(folderPath, note),
            relativePath = rel,
            noteDocumentId = note.documentId,
        )
    }

    private fun decodeDownsampled(
        resolver: ContentResolver,
        uri: Uri,
    ): Bitmap? {
        val bytes = NotesRelativeDocuments.readBytes(resolver, uri) ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) {
            return null
        }
        while (width / sample > THUMB_MAX_PX || height / sample > THUMB_MAX_PX) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    private fun readPrefix(
        resolver: ContentResolver,
        uri: Uri,
    ): String = runCatching {
        resolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(NOTE_PREFIX_BYTES)
            val read = input.read(buffer)
            if (read <= 0) {
                ""
            } else {
                String(buffer, 0, read, Charsets.UTF_8)
            }
        } ?: ""
    }.getOrDefault("")

    private fun isFeaturedImageName(name: String): Boolean {
        val dot = name.lastIndexOf('.')
        if (dot <= 0) {
            return false
        }
        val base = name.substring(0, dot).lowercase(Locale.ROOT)
        return base == "featured-image" || base == "featured_image"
    }

    private fun isImageFileName(name: String): Boolean {
        val dot = name.lastIndexOf('.')
        if (dot <= 0 || dot == name.lastIndex) {
            return false
        }
        return name.substring(dot + 1).lowercase(Locale.ROOT) in IMAGE_EXT
    }

    private data class Child(
        val documentId: String,
        val name: String,
        val isDirectory: Boolean,
    )

    private fun listChildren(
        resolver: ContentResolver,
        treeUri: Uri,
        parentDocumentId: String,
    ): List<Child> {
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
                    val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    buildList {
                        while (cursor.moveToNext()) {
                            val id = cursor.getString(idIndex)
                            val name = cursor.getString(nameIndex)
                            if (id != null && name != null) {
                                val mime = cursor.getString(mimeIndex).orEmpty()
                                add(
                                    Child(
                                        documentId = id,
                                        name = name,
                                        isDirectory = mime == DocumentsContract.Document.MIME_TYPE_DIR,
                                    ),
                                )
                            }
                        }
                    }
                } ?: emptyList()
        }.getOrDefault(emptyList())
    }
}

package dev.harrix.notes

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.provider.DocumentsContract
import java.io.ByteArrayOutputStream

data class CanvasPageRef(
    /** 1-based display order in the note. */
    val index: Int,
    val fileName: String,
    val relativePath: String,
    val uri: Uri,
    val documentId: String,
)

/**
 * Lists / creates / deletes multi-page canvas PNGs under `img/`,
 * and keeps Markdown `![canvas](img/…)` links in sync.
 */
object CanvasPages {
    private val canvasImageLineRegex =
        Regex(
            """^\s*!\[canvas\]\(img/canvas(?:_\d{2})?\.png\)\s*$""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
        )

    fun listPages(
        resolver: ContentResolver,
        treeUri: Uri,
        folderPath: List<NotesPathSegment>,
        noteDocumentId: String,
    ): List<CanvasPageRef> {
        val imgFolderId =
            resolveImgFolderDocumentId(resolver, treeUri, folderPath, noteDocumentId)
                ?: return emptyList()
        val children = queryChildFiles(resolver, treeUri, imgFolderId)
        val numbered =
            children
                .mapNotNull { child ->
                    val number = CanvasNoteDefaults.parsePageNumber(child.name) ?: return@mapNotNull null
                    number to child
                }.sortedBy { it.first }
        if (numbered.isNotEmpty()) {
            return numbered.mapIndexed { index, (_, child) ->
                CanvasPageRef(
                    index = index + 1,
                    fileName = child.name,
                    relativePath = "${CanvasNoteDefaults.IMAGE_FOLDER}/${child.name}",
                    uri = child.uri,
                    documentId = child.documentId,
                )
            }
        }
        val legacy =
            children.firstOrNull { CanvasNoteDefaults.isLegacyCanvasFile(it.name) }
                ?: return emptyList()
        return listOf(
            CanvasPageRef(
                index = 1,
                fileName = legacy.name,
                relativePath = CanvasNoteDefaults.LEGACY_RELATIVE_PATH,
                uri = legacy.uri,
                documentId = legacy.documentId,
            ),
        )
    }

    fun createBlankPngBytes(): ByteArray {
        val bitmap =
            Bitmap.createBitmap(
                CanvasNoteDefaults.WIDTH_PX,
                CanvasNoteDefaults.HEIGHT_PX,
                Bitmap.Config.ARGB_8888,
            )
        bitmap.eraseColor(Color.TRANSPARENT)
        return try {
            ByteArrayOutputStream().use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    error("Could not encode blank canvas")
                }
                stream.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Ensures `img/` exists, creates the next `canvas_NN.png`, and migrates legacy
     * `canvas.png` → `canvas_01.png` when needed.
     */
    fun addPage(
        resolver: ContentResolver,
        treeUri: Uri,
        folderPath: List<NotesPathSegment>,
        noteDocumentId: String,
    ): List<CanvasPageRef> {
        val noteFolderId =
            NotesRelativeDocuments.noteParentDocumentId(noteDocumentId)
                ?: folderPath.lastOrNull()?.documentId
                ?: error("Could not resolve note folder")
        val noteFolderUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, noteFolderId)
        var imgFolderId = resolveImgFolderDocumentId(resolver, treeUri, folderPath, noteDocumentId)
        if (imgFolderId == null) {
            val created =
                DocumentsContract.createDocument(
                    resolver,
                    noteFolderUri,
                    DocumentsContract.Document.MIME_TYPE_DIR,
                    CanvasNoteDefaults.IMAGE_FOLDER,
                ) ?: error("Could not create img folder")
            imgFolderId = DocumentsContract.getDocumentId(created)
        }
        val imgFolderUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, imgFolderId)
        val children = queryChildFiles(resolver, treeUri, imgFolderId)
        val legacy = children.firstOrNull { CanvasNoteDefaults.isLegacyCanvasFile(it.name) }
        val maxNumber =
            children.mapNotNull { CanvasNoteDefaults.parsePageNumber(it.name) }.maxOrNull() ?: 0
        if (legacy != null && maxNumber == 0) {
            runCatching {
                DocumentsContract.renameDocument(
                    resolver,
                    legacy.uri,
                    CanvasNoteDefaults.pageFileName(1),
                )
            }
        }
        val refreshed = queryChildFiles(resolver, treeUri, imgFolderId)
        val nextNumber =
            (
                refreshed.mapNotNull { CanvasNoteDefaults.parsePageNumber(it.name) }.maxOrNull()
                    ?: 0
                ) + 1
        val fileName = CanvasNoteDefaults.pageFileName(nextNumber)
        val pngUri =
            DocumentsContract.createDocument(
                resolver,
                imgFolderUri,
                "image/png",
                fileName,
            ) ?: error("Could not create $fileName")
        resolver.openOutputStream(pngUri, "w")?.use { output ->
            output.write(createBlankPngBytes())
            output.flush()
        } ?: error("Could not write $fileName")
        return listPages(resolver, treeUri, folderPath, noteDocumentId)
    }

    fun deletePage(
        resolver: ContentResolver,
        page: CanvasPageRef,
    ): Boolean = runCatching {
        DocumentsContract.deleteDocument(resolver, page.uri)
    }.getOrDefault(false)

    fun syncMarkdownImageLinks(
        markdown: String,
        pages: List<CanvasPageRef>,
    ): String {
        val links =
            pages.joinToString("\n\n") { page ->
                "![canvas](${page.relativePath})"
            }
        val withoutCanvasLines =
            canvasImageLineRegex.replace(markdown) { "" }
                .replace(Regex("\n{3,}"), "\n\n")
                .trimEnd()
        if (links.isEmpty()) {
            return if (withoutCanvasLines.endsWith("\n")) {
                withoutCanvasLines
            } else {
                "$withoutCanvasLines\n"
            }
        }
        val insertion = "\n\n$links\n"
        val h1Regex = Regex("""(?m)^#\s+.+$""")
        val h1 = h1Regex.find(withoutCanvasLines)
        return if (h1 != null) {
            val insertAt = h1.range.last + 1
            val before = withoutCanvasLines.substring(0, insertAt).trimEnd()
            val after =
                withoutCanvasLines
                    .substring(insertAt)
                    .replace(Regex("""^\s*\n+"""), "\n")
                    .trimStart('\n')
            buildString {
                append(before)
                append(insertion)
                if (after.isNotEmpty()) {
                    append(after)
                    if (!after.endsWith("\n")) {
                        append('\n')
                    }
                }
            }
        } else {
            withoutCanvasLines.trimEnd() + insertion
        }
    }

    fun markdownImageBlock(pageCount: Int): String {
        if (pageCount <= 0) {
            return ""
        }
        return (1..pageCount).joinToString("\n\n") { number ->
            "![canvas](${CanvasNoteDefaults.pageRelativePath(number)})"
        }
    }

    private data class ChildFile(
        val documentId: String,
        val name: String,
        val uri: Uri,
    )

    private fun resolveImgFolderDocumentId(
        resolver: ContentResolver,
        treeUri: Uri,
        folderPath: List<NotesPathSegment>,
        noteDocumentId: String,
    ): String? {
        val baseIds =
            listOfNotNull(
                NotesRelativeDocuments.noteParentDocumentId(noteDocumentId),
                folderPath.lastOrNull()?.documentId,
            ).distinct()
        for (baseId in baseIds) {
            findChildDirectoryId(resolver, treeUri, baseId, CanvasNoteDefaults.IMAGE_FOLDER)
                ?.let { return it }
        }
        return null
    }

    private fun findChildDirectoryId(
        resolver: ContentResolver,
        treeUri: Uri,
        parentDocumentId: String,
        name: String,
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
                    while (cursor.moveToNext()) {
                        val childName = cursor.getString(nameIndex) ?: continue
                        val mime = cursor.getString(mimeIndex).orEmpty()
                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR &&
                            childName.equals(name, ignoreCase = true)
                        ) {
                            return@use cursor.getString(idIndex)
                        }
                    }
                    null
                }
        }.getOrNull()
    }

    private fun queryChildFiles(
        resolver: ContentResolver,
        treeUri: Uri,
        parentDocumentId: String,
    ): List<ChildFile> {
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
                    buildList {
                        while (cursor.moveToNext()) {
                            val mime = cursor.getString(mimeIndex).orEmpty()
                            val id = cursor.getString(idIndex)
                            val name = cursor.getString(nameIndex)
                            if (mime != DocumentsContract.Document.MIME_TYPE_DIR &&
                                id != null &&
                                name != null
                            ) {
                                add(
                                    ChildFile(
                                        documentId = id,
                                        name = name,
                                        uri =
                                        DocumentsContract.buildDocumentUriUsingTree(treeUri, id),
                                    ),
                                )
                            }
                        }
                    }
                }
        }.getOrNull().orEmpty()
    }
}

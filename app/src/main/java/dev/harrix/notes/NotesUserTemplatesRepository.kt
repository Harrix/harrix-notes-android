package dev.harrix.notes

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.util.Locale

/** Lists and writes user beginning templates as flat `.md` files under a SAF tree. */
class NotesUserTemplatesRepository(
    context: Context,
) {
    private val resolver = context.applicationContext.contentResolver

    fun listTemplates(treeUriString: String?): List<NewNoteContent.BeginningTemplate> {
        if (treeUriString.isNullOrBlank()) {
            return emptyList()
        }
        val treeUri = Uri.parse(treeUriString)
        val rootId =
            runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
                ?: return emptyList()
        return queryMdChildren(treeUri, rootId)
            .sortedBy { it.label.lowercase(Locale.ROOT) }
    }

    fun createTemplate(
        treeUriString: String,
        fileStem: String,
        content: String,
    ): NewNoteContent.BeginningTemplate {
        val treeUri = Uri.parse(treeUriString)
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootId)
        val existing =
            queryMdChildren(treeUri, rootId)
                .map { it.label.lowercase(Locale.ROOT) }
                .toSet()
        val stem = uniqueStem(fileStem, existing)
        val displayName = "$stem.md"
        val created =
            DocumentsContract.createDocument(
                resolver,
                parentUri,
                "text/markdown",
                displayName,
            ) ?: DocumentsContract.createDocument(
                resolver,
                parentUri,
                "text/plain",
                displayName,
            ) ?: error("Could not create template")
        resolver.openOutputStream(created, "wt")?.use { output ->
            output.write(content.toByteArray(Charsets.UTF_8))
            output.flush()
        } ?: error("Could not write template")
        val documentId = DocumentsContract.getDocumentId(created)
        val noteUri =
            runCatching {
                DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
            }.getOrDefault(created)
        return NewNoteContent.BeginningTemplate(
            id = userTemplateId(documentId),
            label = displayName,
            content = content,
            source = NotesTemplateSource.User,
            uri = noteUri,
        )
    }

    fun writeTemplate(
        template: NewNoteContent.BeginningTemplate,
        content: String,
        newFileStem: String? = null,
    ): NewNoteContent.BeginningTemplate {
        require(template.source == NotesTemplateSource.User)
        val uri = template.uri ?: error("Missing template URI")
        var resultUri = uri
        var label = template.label
        val stem =
            newFileStem
                ?.let { NotesTreeRepository.normalizeMarkdownFileStem(it) }
                ?.takeIf { it.isNotBlank() }
        if (stem != null) {
            val desiredName = "$stem.md"
            if (!desiredName.equals(template.label, ignoreCase = true)) {
                val renamed =
                    runCatching {
                        DocumentsContract.renameDocument(resolver, uri, desiredName)
                    }.getOrNull()
                if (renamed != null) {
                    resultUri = renamed
                    label = desiredName
                }
            }
        }
        resolver.openOutputStream(resultUri, "wt")?.use { output ->
            output.write(content.toByteArray(Charsets.UTF_8))
            output.flush()
        } ?: error("Could not save template")
        return template.copy(
            label = label,
            content = content,
            uri = resultUri,
        )
    }

    fun readContent(template: NewNoteContent.BeginningTemplate): String {
        val uri = template.uri ?: return template.content
        return resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: template.content
    }

    private fun queryMdChildren(
        treeUri: Uri,
        parentDocumentId: String,
    ): List<NewNoteContent.BeginningTemplate> {
        val childrenUri =
            DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        val result = mutableListOf<NewNoteContent.BeginningTemplate>()
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
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                if (idIdx < 0 || nameIdx < 0) {
                    return emptyList()
                }
                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(idIdx)
                    val name = cursor.getString(nameIdx).orEmpty()
                    val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) else null
                    if (
                        documentId != null &&
                        mime != DocumentsContract.Document.MIME_TYPE_DIR &&
                        name.lowercase(Locale.ROOT).endsWith(".md")
                    ) {
                        val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                        val content =
                            runCatching {
                                resolver
                                    .openInputStream(uri)
                                    ?.bufferedReader()
                                    ?.use { it.readText() }
                            }.getOrNull().orEmpty()
                        result +=
                            NewNoteContent.BeginningTemplate(
                                id = userTemplateId(documentId),
                                label = name,
                                content = content,
                                source = NotesTemplateSource.User,
                                uri = uri,
                            )
                    }
                }
            }
        return result
    }

    companion object {
        fun userTemplateId(documentId: String): String = "user:$documentId"

        fun uniqueStem(
            desired: String,
            existingLowerNames: Set<String>,
        ): String {
            val base =
                NotesTreeRepository
                    .normalizeMarkdownFileStem(desired)
                    .ifBlank { "template" }
            var candidate = base
            var index = 2
            while (
                existingLowerNames.contains(candidate.lowercase(Locale.ROOT)) ||
                existingLowerNames.contains("$candidate.md".lowercase(Locale.ROOT))
            ) {
                candidate = "$base-$index"
                index += 1
            }
            return candidate
        }

        fun loadAllTemplates(
            preferences: NotesViewerPreferences,
            userRepository: NotesUserTemplatesRepository,
        ): List<NewNoteContent.BeginningTemplate> {
            val system =
                preferences.loadBeginningTemplates().map {
                    it.copy(source = NotesTemplateSource.System, uri = null)
                }
            val user = userRepository.listTemplates(preferences.loadUserTemplatesTreeUri())
            return system + user
        }
    }
}

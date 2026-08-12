package dev.harrix.notes

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/** One recently opened note, scoped to a notes tree URI. */
data class NotesRecentItem(
    val documentId: String,
    val uri: Uri,
    val title: String,
    /** Original note file name (e.g. `Note.md`). */
    val fileName: String = "",
    /** Path from notes root through parent folders (excludes the note). */
    val folderPath: List<NotesPathSegment> = emptyList(),
) {
    /** Menu label; year/date titles include the parent folder. */
    val displayTitle: String
        get() = contextualNoteTitle(title, folderPath)
}

/** JSON map of tree URI → recent notes (most recent first). */
data class NotesRecentItemsStore(
    val byTreeUri: Map<String, List<NotesRecentItem>>,
) {
    fun itemsFor(treeUri: String?): List<NotesRecentItem>? {
        if (treeUri.isNullOrBlank()) {
            return null
        }
        return byTreeUri[treeUri]
    }

    fun withItems(
        treeUri: String,
        items: List<NotesRecentItem>,
    ): NotesRecentItemsStore = copy(byTreeUri = byTreeUri + (treeUri to items))

    fun toJson(): String {
        val root = JSONObject()
        val trees = JSONObject()
        byTreeUri.forEach { (treeUri, items) ->
            val itemsJson = JSONArray()
            items.forEach { itemsJson.put(it.toJson()) }
            trees.put(treeUri, itemsJson)
        }
        root.put(KEY_TREES, trees)
        return root.toString()
    }

    companion object {
        private const val KEY_TREES = "trees"
        private const val KEY_DOCUMENT_ID = "documentId"
        private const val KEY_URI = "uri"
        private const val KEY_TITLE = "title"
        private const val KEY_FILE_NAME = "fileName"
        private const val KEY_FOLDER_PATH = "folderPath"
        private const val KEY_NAME = "name"

        fun empty(): NotesRecentItemsStore = NotesRecentItemsStore(byTreeUri = emptyMap())

        fun fromJson(raw: String): NotesRecentItemsStore? = runCatching {
            val root = JSONObject(raw)
            val treesJson = root.optJSONObject(KEY_TREES) ?: return empty()
            val map = linkedMapOf<String, List<NotesRecentItem>>()
            treesJson.keys().forEach { treeUri ->
                if (treeUri.isNullOrBlank()) {
                    return@forEach
                }
                val itemsJson = treesJson.optJSONArray(treeUri) ?: JSONArray()
                val items =
                    buildList {
                        for (index in 0 until itemsJson.length()) {
                            val itemJson = itemsJson.optJSONObject(index) ?: continue
                            parseItem(itemJson)?.let(::add)
                        }
                    }
                map[treeUri] = items
            }
            NotesRecentItemsStore(byTreeUri = map)
        }.getOrNull()

        private fun parseItem(json: JSONObject): NotesRecentItem? {
            val documentId =
                json.optString(KEY_DOCUMENT_ID).takeIf { it.isNotBlank() } ?: return null
            val uriString = json.optString(KEY_URI).takeIf { it.isNotBlank() } ?: return null
            val title = json.optString(KEY_TITLE)
            val fileName = json.optString(KEY_FILE_NAME)
            val pathJson = json.optJSONArray(KEY_FOLDER_PATH) ?: JSONArray()
            val folderPath =
                buildList {
                    for (index in 0 until pathJson.length()) {
                        val segmentJson = pathJson.optJSONObject(index) ?: continue
                        parseSegment(segmentJson)?.let(::add)
                    }
                }
            return NotesRecentItem(
                documentId = documentId,
                uri = Uri.parse(uriString),
                title = title,
                fileName = fileName,
                folderPath = folderPath,
            )
        }

        private fun parseSegment(json: JSONObject): NotesPathSegment? {
            val documentId =
                json.optString(KEY_DOCUMENT_ID).takeIf { it.isNotBlank() } ?: return null
            val name = json.optString(KEY_NAME).ifBlank { documentId }
            val uriString = json.optString(KEY_URI).takeIf { it.isNotBlank() } ?: return null
            return NotesPathSegment(
                documentId = documentId,
                name = name,
                uri = Uri.parse(uriString),
            )
        }

        private fun NotesRecentItem.toJson(): JSONObject {
            val json = JSONObject()
            json.put(KEY_DOCUMENT_ID, documentId)
            json.put(KEY_URI, uri.toString())
            json.put(KEY_TITLE, title)
            json.put(KEY_FILE_NAME, fileName)
            val pathJson = JSONArray()
            folderPath.forEach { segment ->
                pathJson.put(segment.toJson())
            }
            json.put(KEY_FOLDER_PATH, pathJson)
            return json
        }

        private fun NotesPathSegment.toJson(): JSONObject {
            val json = JSONObject()
            json.put(KEY_DOCUMENT_ID, documentId)
            json.put(KEY_NAME, name)
            json.put(KEY_URI, uri.toString())
            return json
        }
    }
}

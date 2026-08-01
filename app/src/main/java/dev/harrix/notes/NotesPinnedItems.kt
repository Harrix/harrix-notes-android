package dev.harrix.notes

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/** Kind of entry shown in the pinned bottom bar. */
enum class NotesPinnedKind {
    Home,
    Folder,
    Note,
    ;

    companion object {
        fun fromStorageKey(raw: String?): NotesPinnedKind? =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
    }
}

/** One pinned home / folder / note entry, scoped to a notes tree URI. */
data class NotesPinnedItem(
    val id: String,
    val kind: NotesPinnedKind,
    val documentId: String,
    val uri: Uri,
    val title: String,
    /** YAML `icon:` emoji for notes; unused for home/folder. */
    val icon: String = "",
    /** Original note file name (e.g. `Note.md`); unused for home/folder. */
    val fileName: String = "",
    /**
     * Navigation path:
     * - Home: empty
     * - Folder: root → folder (inclusive)
     * - Note: root → parent folders (excludes the note)
     */
    val folderPath: List<NotesPathSegment> = emptyList(),
) {
    companion object {
        const val HOME_ID = "home"
    }
}

/** JSON map of tree URI → pinned items (keeps pins when switching folders and restoring). */
data class NotesPinnedItemsStore(
    val byTreeUri: Map<String, List<NotesPinnedItem>>,
) {
    fun itemsFor(treeUri: String?): List<NotesPinnedItem>? {
        if (treeUri.isNullOrBlank()) {
            return null
        }
        return byTreeUri[treeUri]
    }

    fun withItems(
        treeUri: String,
        items: List<NotesPinnedItem>,
    ): NotesPinnedItemsStore = copy(byTreeUri = byTreeUri + (treeUri to items))

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
        private const val KEY_ID = "id"
        private const val KEY_KIND = "kind"
        private const val KEY_DOCUMENT_ID = "documentId"
        private const val KEY_URI = "uri"
        private const val KEY_TITLE = "title"
        private const val KEY_ICON = "icon"
        private const val KEY_FILE_NAME = "fileName"
        private const val KEY_FOLDER_PATH = "folderPath"
        private const val KEY_NAME = "name"

        fun empty(): NotesPinnedItemsStore = NotesPinnedItemsStore(byTreeUri = emptyMap())

        fun fromJson(raw: String): NotesPinnedItemsStore? =
            runCatching {
                val root = JSONObject(raw)
                val treesJson = root.optJSONObject(KEY_TREES) ?: return empty()
                val map = linkedMapOf<String, List<NotesPinnedItem>>()
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
                NotesPinnedItemsStore(byTreeUri = map)
            }.getOrNull()

        fun defaultHome(root: NotesPathSegment): NotesPinnedItem =
            NotesPinnedItem(
                id = NotesPinnedItem.HOME_ID,
                kind = NotesPinnedKind.Home,
                documentId = root.documentId,
                uri = root.uri,
                title = "",
                folderPath = emptyList(),
            )

        private fun parseItem(json: JSONObject): NotesPinnedItem? {
            val id = json.optString(KEY_ID).takeIf { it.isNotBlank() } ?: return null
            val kind = NotesPinnedKind.fromStorageKey(json.optString(KEY_KIND)) ?: return null
            val documentId =
                json.optString(KEY_DOCUMENT_ID).takeIf { it.isNotBlank() } ?: return null
            val uriString = json.optString(KEY_URI).takeIf { it.isNotBlank() } ?: return null
            val title = json.optString(KEY_TITLE)
            val icon = json.optString(KEY_ICON)
            val fileName = json.optString(KEY_FILE_NAME)
            val pathJson = json.optJSONArray(KEY_FOLDER_PATH) ?: JSONArray()
            val folderPath =
                buildList {
                    for (index in 0 until pathJson.length()) {
                        val segmentJson = pathJson.optJSONObject(index) ?: continue
                        parseSegment(segmentJson)?.let(::add)
                    }
                }
            return NotesPinnedItem(
                id = id,
                kind = kind,
                documentId = documentId,
                uri = Uri.parse(uriString),
                title = title,
                icon = icon,
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

        private fun NotesPinnedItem.toJson(): JSONObject {
            val json = JSONObject()
            json.put(KEY_ID, id)
            json.put(KEY_KIND, kind.name)
            json.put(KEY_DOCUMENT_ID, documentId)
            json.put(KEY_URI, uri.toString())
            json.put(KEY_TITLE, title)
            json.put(KEY_ICON, icon)
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

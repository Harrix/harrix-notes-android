package dev.harrix.notes

/**
 * Display labels for pinned bar / settings rows.
 * When several pins share the same title, shows a short path that highlights
 * the first differing folder, e.g. `Folder1/.../Note` vs `Folder2/.../Note`.
 */
fun pinnedDisplayLabels(
    items: List<NotesPinnedItem>,
    homeLabel: String,
): Map<String, String> {
    if (items.isEmpty()) {
        return emptyMap()
    }
    val baseById =
        items.associate { item ->
            item.id to basePinnedLabel(item, homeLabel)
        }
    val ambiguousTitles =
        baseById.values
            .groupingBy { it }
            .eachCount()
            .filter { (title, count) -> title != homeLabel && count > 1 }
            .keys
    if (ambiguousTitles.isEmpty()) {
        return baseById
    }

    val result = baseById.toMutableMap()
    for (title in ambiguousTitles) {
        val group = items.filter { baseById[it.id] == title }
        val paths = group.map { pinnedPathSegments(it, title) }
        group.forEachIndexed { index, item ->
            result[item.id] = formatDisambiguatedPath(paths[index], paths)
        }
    }
    return result
}

private fun basePinnedLabel(
    item: NotesPinnedItem,
    homeLabel: String,
): String = when {
    item.kind == NotesPinnedKind.Home || item.id == NotesPinnedItem.HOME_ID -> homeLabel
    else -> item.title.ifBlank { item.documentId }
}

/**
 * Path segments including the leaf title.
 * Folder pins already end with the folder in [NotesPinnedItem.folderPath];
 * note pins append the note title after parent folders.
 */
internal fun pinnedPathSegments(
    item: NotesPinnedItem,
    leafTitle: String,
): List<String> {
    val parents = item.folderPath.map { it.name }.filter { it.isNotBlank() }
    return when (item.kind) {
        NotesPinnedKind.Home -> listOf(leafTitle)

        NotesPinnedKind.Folder -> {
            if (parents.isEmpty()) {
                listOf(leafTitle)
            } else if (parents.last().equals(leafTitle, ignoreCase = true)) {
                parents
            } else {
                parents + leafTitle
            }
        }

        NotesPinnedKind.Note -> parents + leafTitle
    }
}

/**
 * Among [peerPaths], shorten [path] to `FirstDiff/.../Leaf` (or `FirstDiff/Leaf`
 * when they are adjacent).
 */
internal fun formatDisambiguatedPath(
    path: List<String>,
    peerPaths: List<List<String>>,
): String {
    if (path.isEmpty()) {
        return ""
    }
    val leaf = path.last()
    if (peerPaths.size <= 1 || path.size == 1) {
        return leaf
    }

    val minLen = peerPaths.minOf { it.size }
    var divergeAt = 0
    while (divergeAt < minLen) {
        val namesAtIndex = peerPaths.map { it.getOrNull(divergeAt) }.toSet()
        if (namesAtIndex.size > 1) {
            break
        }
        divergeAt++
    }

    val leafIndex = path.lastIndex
    if (divergeAt >= leafIndex) {
        return leaf
    }
    val distinct = path[divergeAt]
    return if (divergeAt == leafIndex - 1) {
        "$distinct/$leaf"
    } else {
        "$distinct/.../$leaf"
    }
}

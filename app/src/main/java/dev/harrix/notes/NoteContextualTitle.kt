package dev.harrix.notes

/**
 * When a note title is only a year or ISO date (`2026`, `2026-05`, `2026-05-06`),
 * pins and tabs append the parent folder: `cases/2026`.
 *
 * For folder-per-note packages (`cases/2026/2026.md`) the immediate parent matches
 * the title, so the grandparent is used instead.
 */
fun looksLikeYearOrDateTitle(title: String): Boolean {
    val t = title.trim()
    if (t.isEmpty()) {
        return false
    }
    return YEAR_TITLE_REGEX.matches(t) ||
        YEAR_MONTH_TITLE_REGEX.matches(t) ||
        ISO_DATE_TITLE_REGEX.matches(t)
}

fun contextualNoteTitle(
    title: String,
    folderPath: List<NotesPathSegment>,
): String {
    val leaf = title.trim()
    if (!looksLikeYearOrDateTitle(leaf)) {
        return title
    }
    val parents = folderPath.map { it.name.trim() }.filter { it.isNotEmpty() }
    if (parents.isEmpty()) {
        return title
    }
    val parent =
        if (parents.last().equals(leaf, ignoreCase = true)) {
            parents.getOrNull(parents.lastIndex - 1) ?: return title
        } else {
            parents.last()
        }
    return "$parent/$leaf"
}

private val YEAR_TITLE_REGEX = Regex("^\\d{4}$")
private val YEAR_MONTH_TITLE_REGEX = Regex("^\\d{4}-\\d{2}$")
private val ISO_DATE_TITLE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")

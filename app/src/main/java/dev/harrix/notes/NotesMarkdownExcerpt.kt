package dev.harrix.notes

/**
 * Plain-text card preview from Markdown (Samsung Notes-style thumbnails).
 *
 * `@hsk-sync:notes-browse` — keep aligned with VS Code `excerptFromMarkdown`
 * / `firstMarkdownImageSrc` in `icons-browse-listing.js`.
 */
object NotesMarkdownExcerpt {
    private val FRONTMATTER = Regex("""^---\r?\n[\s\S]*?\r?\n---\r?\n?""")
    private val HTML_COMMENT = Regex("""<!--[\s\S]*?-->""")
    private val MD_IMAGE = Regex("""!\[[^\]]*]\(([^)]+)\)""")
    private val MD_IMAGE_FULL = Regex("""!\[[^\]]*]\([^)]+\)""")
    private val MD_LINK = Regex("""\[([^\]]+)]\([^)]+\)""")
    private val HEADING = Regex("""^#{1,6}\s+""")
    private val BULLET = Regex("""^[-*+]\s+""")
    private val NUMBERED = Regex("""^\d+\.\s+""")
    private val MD_MARKS = Regex("""[`*_>#]""")
    private val REMOTE_PREFIXES = listOf("https:", "http:", "data:", "mailto:")

    fun excerptFromMarkdown(
        text: String,
        maxLen: Int = 220,
    ): String {
        var src = text.removePrefix("\uFEFF")
        src = FRONTMATTER.replaceFirst(src, "")
        src = HTML_COMMENT.replace(src, "")
        val lines = ArrayList<String>()
        for (raw in src.split("\r\n", "\n")) {
            val line = cleanExcerptLine(raw)
            if (line.isNotEmpty()) {
                lines.add(line)
                if (lines.joinToString(" ").length >= maxLen) {
                    break
                }
            }
        }
        val out = lines.joinToString(" ")
        return if (out.length > maxLen) {
            "${out.take(maxLen).trim()}…"
        } else {
            out
        }
    }

    fun firstMarkdownImageSrc(text: String): String {
        var src = text.removePrefix("\uFEFF")
        src = FRONTMATTER.replaceFirst(src, "")
        for (match in MD_IMAGE.findAll(src)) {
            val raw =
                match.groupValues
                    .getOrNull(1)
                    .orEmpty()
                    .trim()
                    .trim('<', '>')
                    .split(Regex("""\s+"""))
                    .firstOrNull()
                    .orEmpty()
            if (isLocalImageSrc(raw)) {
                return raw
            }
        }
        return ""
    }

    private fun cleanExcerptLine(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("```")) {
            return ""
        }
        return trimmed
            .replace(HEADING, "")
            .replace(BULLET, "")
            .replace(NUMBERED, "")
            .replace(MD_IMAGE_FULL, "")
            .replace(MD_LINK, "$1")
            .replace(MD_MARKS, "")
            .trim()
    }

    private fun isLocalImageSrc(raw: String): Boolean {
        if (raw.isEmpty()) {
            return false
        }
        return REMOTE_PREFIXES.none { prefix -> raw.startsWith(prefix, ignoreCase = true) }
    }
}

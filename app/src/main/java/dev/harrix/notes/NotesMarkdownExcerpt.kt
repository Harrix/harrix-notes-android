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
    private val DETAILS_BLOCK = Regex("""<details\b[^>]*>[\s\S]*?</details>""", RegexOption.IGNORE_CASE)
    private val TOC_HINT = Regex("""содержан|оглавлени|table\s+of\s+contents|\bcontents\b""", RegexOption.IGNORE_CASE)
    private val TOC_HEADING =
        Regex(
            """^(?:содержание|оглавление|contents|table of contents)$""",
            RegexOption.IGNORE_CASE,
        )
    private val NON_WORD = Regex("""[^\p{L}\p{N}\s]""")
    private val EXTRA_SPACES = Regex("""\s+""")
    private val TOC_HASH_LINK = Regex("""^\[[^\]]+]\(#[^)]+\)$""")
    private val DETAILS_OR_SUMMARY_TAG = Regex("""^</?(?:details|summary)\b""", RegexOption.IGNORE_CASE)
    private val HTML_TAG = Regex("""<[^>]+>""")
    private val REMOTE_PREFIXES = listOf("https:", "http:", "data:", "mailto:")

    fun excerptFromMarkdown(
        text: String,
        maxLen: Int = 220,
    ): String {
        var src = text.removePrefix("\uFEFF")
        src = FRONTMATTER.replaceFirst(src, "")
        src = HTML_COMMENT.replace(src, "")
        src = stripTocBlocks(src)
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

    private fun stripTocBlocks(text: String): String = DETAILS_BLOCK.replace(text) { match ->
        if (TOC_HINT.containsMatchIn(match.value)) {
            ""
        } else {
            match.value
        }
    }

    private fun isTocOnlyLine(raw: String): Boolean {
        val line = raw.trim()
        if (line.isEmpty() || DETAILS_OR_SUMMARY_TAG.containsMatchIn(line)) {
            return true
        }
        val heading =
            line
                .replace(HEADING, "")
                .replace(HTML_TAG, "")
                .replace(MD_MARKS, "")
                .replace(NON_WORD, "")
                .replace(EXTRA_SPACES, " ")
                .trim()
        if (TOC_HEADING.matches(heading)) {
            return true
        }
        val withoutList = line.replace(BULLET, "").replace(NUMBERED, "").trim()
        return TOC_HASH_LINK.matches(withoutList)
    }

    private fun cleanExcerptLine(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("```") || isTocOnlyLine(trimmed)) {
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

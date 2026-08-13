package dev.harrix.notes

/**
 * Extracts note display metadata from Markdown.
 *
 * `@hsk-sync:note-meta` — title/icon parsing stays aligned with
 * `note-meta.js` (VS Code) and `harrix_pylib.note_meta`. Prefer
 * [NoteMetaResolver.resolveTitle] when a file-stem fallback is needed.
 */
object NoteTitleExtractor {
    private val FRONTMATTER_REGEX = Regex("^---\\r?\\n([\\s\\S]*?)\\r?\\n---\\r?\\n?")
    private val TITLE_LINE_REGEX = Regex("^title\\s*:\\s*(.*)$", RegexOption.IGNORE_CASE)
    private val ICON_LINE_REGEX = Regex("^icon\\s*:\\s*(.*)$", RegexOption.IGNORE_CASE)
    private val TYPE_LINE_REGEX = Regex("^type\\s*:\\s*(.*)$", RegexOption.IGNORE_CASE)
    private val PAPER_LINE_REGEX = Regex("^paper\\s*:\\s*(.*)$", RegexOption.IGNORE_CASE)
    private val H1_REGEX = Regex("^#\\s+(.+)$")
    private val HTTP_URL_REGEX = Regex("^https?://", RegexOption.IGNORE_CASE)
    private val IMAGE_EXT_REGEX = Regex("\\.(png|jpe?g|gif|svg|webp|avif|ico)$", RegexOption.IGNORE_CASE)

    data class Meta(
        val title: String,
        val icon: String,
        /** YAML `type:` value when present (e.g. `canvas`). */
        val type: String = "",
        /** YAML `paper:` value when present (`light` / `dark` / `theme`). */
        val paper: String = "",
    )

    fun extract(text: String): String = extractMeta(text).title

    fun isCanvas(text: String): Boolean = extractMeta(text).type.equals("canvas", ignoreCase = true)

    /** Canvas underlay from YAML `paper:`, or null when the key is absent / unknown. */
    fun extractCanvasPaperMode(text: String): CanvasPaperMode? {
        val raw = extractMeta(text).paper.trim()
        if (raw.isEmpty()) {
            return null
        }
        return CanvasPaperMode.entries.firstOrNull {
            it.yamlKey.equals(raw, ignoreCase = true) ||
                it.name.equals(raw, ignoreCase = true)
        }
    }

    fun extractMeta(text: String): Meta {
        var src = text
        if (src.isNotEmpty() && src[0] == '\uFEFF') {
            src = src.substring(1)
        }
        val fmMatch = FRONTMATTER_REGEX.find(src)
        val title: String
        val icon: String
        val type: String
        val paper: String
        if (fmMatch != null) {
            val fm = fmMatch.groupValues[1]
            title =
                titleFromFrontmatterBlock(fm)
                    .ifEmpty { firstH1AfterFrontmatter(src.substring(fmMatch.range.last + 1)) }
            icon = iconFromFrontmatterBlock(fm)
            type = typeFromFrontmatterBlock(fm)
            paper = paperFromFrontmatterBlock(fm)
        } else {
            title = firstH1AfterFrontmatter(src)
            icon = ""
            type = ""
            paper = ""
        }
        return Meta(title = stripHtmlComments(title), icon = icon, type = type, paper = paper)
    }

    private fun titleFromFrontmatterBlock(fmText: String): String {
        for (line in fmText.lineSequence()) {
            val match = TITLE_LINE_REGEX.find(line) ?: continue
            val title = unquoteYamlScalar(match.groupValues[1])
            if (title.isNotEmpty()) {
                return title
            }
        }
        return ""
    }

    private fun iconFromFrontmatterBlock(fmText: String): String {
        for (line in fmText.lineSequence()) {
            val match = ICON_LINE_REGEX.find(line) ?: continue
            val icon = unquoteYamlScalar(match.groupValues[1])
            if (icon.isNotEmpty() && isNoteTreeEmojiIcon(icon)) {
                return icon
            }
        }
        return ""
    }

    private fun typeFromFrontmatterBlock(fmText: String): String {
        for (line in fmText.lineSequence()) {
            val match = TYPE_LINE_REGEX.find(line) ?: continue
            val type = unquoteYamlScalar(match.groupValues[1])
            if (type.isNotEmpty()) {
                return type
            }
        }
        return ""
    }

    private fun paperFromFrontmatterBlock(fmText: String): String {
        for (line in fmText.lineSequence()) {
            val match = PAPER_LINE_REGEX.find(line) ?: continue
            val paper = unquoteYamlScalar(match.groupValues[1])
            if (paper.isNotEmpty()) {
                return paper
            }
        }
        return ""
    }

    /** Short emoji/symbol for the tree (not a path or image URL). */
    fun isNoteTreeEmojiIcon(value: String): Boolean {
        val v = value.trim()
        if (v.isEmpty()) {
            return false
        }
        if (v.codePointCount(0, v.length) > 8) {
            return false
        }
        if (HTTP_URL_REGEX.containsMatchIn(v) || v.contains('/') || v.contains('\\')) {
            return false
        }
        if (IMAGE_EXT_REGEX.containsMatchIn(v)) {
            return false
        }
        return true
    }

    private fun firstH1AfterFrontmatter(body: String): String {
        var inFence = false
        for (rawLine in body.lineSequence()) {
            val heading = headingCandidate(rawLine, inFence) ?: continue
            when {
                heading.isFence -> inFence = !inFence
                heading.h1Text != null -> return heading.h1Text
            }
        }
        return ""
    }

    private data class HeadingCandidate(
        val isFence: Boolean,
        val h1Text: String?,
    )

    private fun headingCandidate(
        rawLine: String,
        inFence: Boolean,
    ): HeadingCandidate? {
        val line = rawLine.trim()
        if (line.isEmpty()) {
            return null
        }
        if (line.startsWith("```")) {
            return HeadingCandidate(isFence = true, h1Text = null)
        }
        if (inFence || isHtmlCommentLine(line)) {
            return null
        }
        if (line.startsWith("##")) {
            return null
        }
        val h1 = H1_REGEX.find(line) ?: return null
        return HeadingCandidate(isFence = false, h1Text = h1.groupValues[1].trim())
    }

    private fun isHtmlCommentLine(line: String): Boolean = line.startsWith("<!--") && line.contains("-->")

    private fun unquoteYamlScalar(value: String): String {
        var v = value.trim()
        if (isQuotedScalar(v)) {
            v = v.substring(1, v.length - 1)
        }
        return v.trim()
    }

    private fun isQuotedScalar(value: String): Boolean {
        if (value.length < 2) {
            return false
        }
        val doubleQuoted = value.startsWith("\"") && value.endsWith("\"")
        val singleQuoted = value.startsWith("'") && value.endsWith("'")
        return doubleQuoted || singleQuoted
    }

    private fun stripHtmlComments(text: String): String = text.replace(Regex("<!--[\\s\\S]*?-->"), "").trim()
}

package dev.harrix.notes

/**
 * Minimal Markdown → HTML converter for note preview.
 *
 * Supports headings (with anchor ids), paragraphs, emphasis, links, images,
 * inline/fenced code, lists, blockquotes, GFM pipe tables, thematic breaks,
 * and raw `<details>` / `<summary>` HTML blocks. YAML front matter is wrapped
 * in `<details>` (collapsed). No formulas or footnotes.
 *
 * Relative image URLs are rewritten to `/__notes_local__/…` tokens and then
 * embedded as `data:` URIs by the preview pane (SAF cannot be loaded directly).
 */
object SimpleMarkdownToHtml {
    private val FRONTMATTER_REGEX = Regex("^---\\r?\\n([\\s\\S]*?)\\r?\\n---\\r?\\n?")
    private val HEADING_REGEX = Regex("^(#{1,6})\\s+(.+?)\\s*#*\\s*$")
    private val UNORDERED_ITEM_REGEX = Regex("^([-*+])\\s+(.+)$")
    private val ORDERED_ITEM_REGEX = Regex("^(\\d+)[.]\\s+(.+)$")
    private val BLOCKQUOTE_REGEX = Regex("^>\\s?(.*)$")
    private val HR_REGEX = Regex("^(\\*{3,}|-{3,}|_{3,})\\s*$")
    private val FENCE_OPEN_REGEX = Regex("^```([^\\s`]*)\\s*$")
    private val DETAILS_OPEN_REGEX = Regex("^<details\\b[^>]*>\\s*$", RegexOption.IGNORE_CASE)
    private val DETAILS_CLOSE_REGEX = Regex("^</details\\s*>\\s*$", RegexOption.IGNORE_CASE)
    private val SUMMARY_LINE_REGEX =
        Regex("^<summary\\b[^>]*>([\\s\\S]*?)</summary\\s*>\\s*$", RegexOption.IGNORE_CASE)
    private val HTML_IMG_SRC_REGEX =
        Regex("""(?i)(<img\b[^>]*?\bsrc\s*=\s*)(["'])([^"']+)\2""")

    private val INLINE_CODE_REGEX = Regex("`([^`]+)`")
    private val IMAGE_REGEX = Regex("!\\[([^\\]]*)]\\(([^)]+)\\)")
    private val LINK_REGEX = Regex("\\[([^\\]]+)]\\(([^)]+)\\)")
    private val BOLD_REGEX = Regex("(\\*\\*|__)(.+?)\\1")
    private val ITALIC_STAR_REGEX = Regex("\\*([^*]+)\\*")
    private val ITALIC_UNDERSCORE_REGEX = Regex("_([^_]+)_")
    private val SLUG_STRIP_REGEX = Regex("[^\\p{L}\\p{N}\\s\\-_]+")
    private val SLUG_SPACE_REGEX = Regex("[\\s\\-_]+")

    /** Path prefix under the preview base URL for SAF-backed images. */
    const val LOCAL_IMAGE_PATH_PREFIX = "/__notes_local__/"

    private val LOCAL_IMAGE_SRC_REGEX =
        Regex("""src="(/__notes_local__/[^"]+)"""")

    fun convert(source: String): String {
        var text = source
        if (text.isNotEmpty() && text[0] == '\uFEFF') {
            text = text.substring(1)
        }
        val frontmatter = FRONTMATTER_REGEX.find(text)
        val body =
            if (frontmatter != null) {
                text.substring(frontmatter.range.last + 1)
            } else {
                text
            }
        val out = StringBuilder(text.length + 64)
        val slugCounts = linkedMapOf<String, Int>()
        if (frontmatter != null) {
            out.append("<details class=\"frontmatter\"><summary>YAML</summary><pre>")
            out.append(escapeHtml(frontmatter.groupValues[1]))
            out.append("</pre></details>\n")
        }
        out.append(parseBlocks(body, slugCounts))
        return out.toString()
    }

    private fun parseBlocks(
        body: String,
        slugCounts: MutableMap<String, Int>,
    ): String {
        val lines = body.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val out = StringBuilder(body.length + 64)
        var index = 0
        while (index < lines.size) {
            index = appendNextBlock(lines, index, out, slugCounts)
        }
        return out.toString()
    }

    private fun appendNextBlock(
        lines: List<String>,
        start: Int,
        out: StringBuilder,
        slugCounts: MutableMap<String, Int>,
    ): Int {
        val line = lines[start]
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            return start + 1
        }
        if (DETAILS_OPEN_REGEX.matches(trimmed) || trimmed.startsWith("<details", ignoreCase = true)) {
            return appendDetails(lines, start, out, slugCounts)
        }
        FENCE_OPEN_REGEX.matchEntire(trimmed)?.let { fence ->
            return appendFence(lines, start, fence.groupValues[1], out)
        }
        HEADING_REGEX.matchEntire(trimmed)?.let { heading ->
            val level = heading.groupValues[1].length
            val title = heading.groupValues[2]
            val id = uniqueSlug(slugify(title), slugCounts)
            out.append("<h").append(level).append(" id=\"").append(escapeHtml(id)).append("\">")
            out.append(renderInline(title))
            out.append("</h").append(level).append(">\n")
            return start + 1
        }
        if (HR_REGEX.matches(trimmed)) {
            out.append("<hr/>\n")
            return start + 1
        }
        if (BLOCKQUOTE_REGEX.matches(line)) {
            return appendBlockquote(lines, start, out, slugCounts)
        }
        if (UNORDERED_ITEM_REGEX.matches(trimmed)) {
            return appendUnorderedList(lines, start, out)
        }
        if (ORDERED_ITEM_REGEX.matches(trimmed)) {
            return appendOrderedList(lines, start, out)
        }
        if (isTableHeader(lines, start)) {
            return appendTable(lines, start, out)
        }
        return appendParagraph(lines, start, out)
    }

    private fun isTableHeader(
        lines: List<String>,
        start: Int,
    ): Boolean {
        if (start + 1 >= lines.size) {
            return false
        }
        val header = lines[start].trim()
        val separator = lines[start + 1].trim()
        return looksLikeTableRow(header) && isTableSeparator(separator)
    }

    private fun looksLikeTableRow(line: String): Boolean {
        if (line.isEmpty() || !line.contains('|')) {
            return false
        }
        // Avoid treating emphasis / plain pipes in prose as a table start alone.
        return splitTableCells(line).isNotEmpty()
    }

    private fun isTableSeparator(line: String): Boolean {
        if (!line.contains('|') && !line.contains('-')) {
            return false
        }
        val cells = splitTableCells(line)
        if (cells.isEmpty()) {
            return false
        }
        return cells.all { cell ->
            val trimmed = cell.trim()
            if (trimmed.isEmpty()) {
                return@all false
            }
            val core = trimmed.trim(':').trim()
            core.isNotEmpty() && core.all { it == '-' }
        }
    }

    private fun splitTableCells(line: String): List<String> {
        var body = line.trim()
        if (body.startsWith("|")) {
            body = body.substring(1)
        }
        if (body.endsWith("|")) {
            body = body.substring(0, body.length - 1)
        }
        if (body.isEmpty()) {
            return emptyList()
        }
        return body.split('|').map { it.trim() }
    }

    private fun tableCellAlign(separatorCell: String): String? {
        val trimmed = separatorCell.trim()
        val left = trimmed.startsWith(':')
        val right = trimmed.endsWith(':')
        return when {
            left && right -> "center"
            right -> "right"
            left -> "left"
            else -> null
        }
    }

    private fun appendTable(
        lines: List<String>,
        start: Int,
        out: StringBuilder,
    ): Int {
        val headerCells = splitTableCells(lines[start].trim())
        val alignCells = splitTableCells(lines[start + 1].trim())
        val aligns =
            headerCells.indices.map { index ->
                alignCells.getOrNull(index)?.let { tableCellAlign(it) }
            }
        var i = start + 2
        val bodyRows = ArrayList<List<String>>()
        while (i < lines.size) {
            val trimmed = lines[i].trim()
            if (trimmed.isEmpty() || !looksLikeTableRow(trimmed)) {
                break
            }
            bodyRows += splitTableCells(trimmed)
            i += 1
        }
        out.append("<div class=\"table-wrap\"><table>\n<thead><tr>")
        headerCells.forEachIndexed { index, cell ->
            out.append(tableCellHtml("th", cell, aligns.getOrNull(index)))
        }
        out.append("</tr></thead>\n")
        if (bodyRows.isNotEmpty()) {
            out.append("<tbody>\n")
            for (row in bodyRows) {
                out.append("<tr>")
                for (index in headerCells.indices) {
                    val cell = row.getOrElse(index) { "" }
                    out.append(tableCellHtml("td", cell, aligns.getOrNull(index)))
                }
                out.append("</tr>\n")
            }
            out.append("</tbody>\n")
        }
        out.append("</table></div>\n")
        return i
    }

    private fun tableCellHtml(
        tag: String,
        text: String,
        align: String?,
    ): String {
        val style =
            if (align != null) {
                " style=\"text-align:$align\""
            } else {
                ""
            }
        return "<$tag$style>${renderInline(text)}</$tag>"
    }

    private fun appendDetails(
        lines: List<String>,
        start: Int,
        out: StringBuilder,
        slugCounts: MutableMap<String, Int>,
    ): Int {
        val openTrimmed = lines[start].trim()
        val openTagEnd = openTrimmed.indexOf('>')
        val openTag =
            if (openTagEnd >= 0) {
                openTrimmed.substring(0, openTagEnd + 1)
            } else {
                "<details>"
            }
        val sameLineRest =
            if (openTagEnd >= 0 && openTagEnd < openTrimmed.lastIndex) {
                openTrimmed.substring(openTagEnd + 1).trim()
            } else {
                ""
            }
        var i = start + 1
        val innerLines = ArrayList<String>()
        if (sameLineRest.isNotEmpty() && !sameLineRest.equals("</details>", ignoreCase = true)) {
            innerLines += sameLineRest
        }
        var closed = sameLineRest.equals("</details>", ignoreCase = true)
        while (i < lines.size && !closed) {
            if (DETAILS_CLOSE_REGEX.matches(lines[i].trim())) {
                closed = true
                i += 1
            } else {
                innerLines += lines[i]
                i += 1
            }
        }
        val (summaryHtml, bodyLines) = splitDetailsSummary(innerLines)
        out.append(openTag).append('\n')
        if (summaryHtml != null) {
            out.append(summaryHtml).append('\n')
        }
        if (bodyLines.isNotEmpty()) {
            out.append(parseBlocks(bodyLines.joinToString("\n"), slugCounts))
        }
        out.append("</details>\n")
        return if (closed) i else lines.size
    }

    private fun splitDetailsSummary(innerLines: List<String>): Pair<String?, List<String>> {
        if (innerLines.isEmpty()) {
            return null to emptyList()
        }
        val first = innerLines[0].trim()
        val oneLine = SUMMARY_LINE_REGEX.matchEntire(first)
        if (oneLine != null) {
            val summary =
                "<summary>${renderInline(stripTags(oneLine.groupValues[1]))}</summary>"
            return summary to innerLines.drop(1)
        }
        if (!first.startsWith("<summary", ignoreCase = true)) {
            return null to innerLines
        }
        val collected = StringBuilder()
        var endIndex = -1
        for (idx in innerLines.indices) {
            val line = innerLines[idx]
            if (collected.isNotEmpty()) {
                collected.append('\n')
            }
            collected.append(line)
            if (line.contains("</summary>", ignoreCase = true)) {
                endIndex = idx
                break
            }
        }
        if (endIndex < 0) {
            return null to innerLines
        }
        val block = collected.toString().trim()
        val openEnd = block.indexOf('>')
        val closeStart = block.indexOf("</summary>", ignoreCase = true)
        if (openEnd < 0 || closeStart < 0 || closeStart <= openEnd) {
            return null to innerLines
        }
        val summaryText = block.substring(openEnd + 1, closeStart).trim()
        val summary = "<summary>${renderInline(stripTags(summaryText))}</summary>"
        return summary to innerLines.drop(endIndex + 1)
    }

    private fun appendFence(
        lines: List<String>,
        start: Int,
        lang: String,
        out: StringBuilder,
    ): Int {
        val code = StringBuilder()
        var i = start + 1
        while (i < lines.size && !lines[i].trim().startsWith("```")) {
            if (code.isNotEmpty()) {
                code.append('\n')
            }
            code.append(lines[i])
            i += 1
        }
        if (i < lines.size) {
            i += 1
        }
        out.append("<pre><code")
        if (lang.isNotEmpty()) {
            out.append(" class=\"language-").append(escapeHtml(lang)).append('"')
        }
        out.append('>')
        out.append(escapeHtml(code.toString()))
        out.append("</code></pre>\n")
        return i
    }

    private fun appendBlockquote(
        lines: List<String>,
        start: Int,
        out: StringBuilder,
        slugCounts: MutableMap<String, Int>,
    ): Int {
        val quote = StringBuilder()
        var i = start
        while (i < lines.size) {
            val match = BLOCKQUOTE_REGEX.matchEntire(lines[i]) ?: break
            if (quote.isNotEmpty()) {
                quote.append('\n')
            }
            quote.append(match.groupValues[1])
            i += 1
        }
        out.append("<blockquote>")
        out.append(parseBlocks(quote.toString(), slugCounts))
        out.append("</blockquote>\n")
        return i
    }

    private fun appendUnorderedList(
        lines: List<String>,
        start: Int,
        out: StringBuilder,
    ): Int {
        out.append("<ul>\n")
        var i = start
        while (i < lines.size) {
            val item = UNORDERED_ITEM_REGEX.matchEntire(lines[i].trim()) ?: break
            out.append("<li>")
            out.append(renderInline(item.groupValues[2]))
            out.append("</li>\n")
            i += 1
        }
        out.append("</ul>\n")
        return i
    }

    private fun appendOrderedList(
        lines: List<String>,
        start: Int,
        out: StringBuilder,
    ): Int {
        out.append("<ol>\n")
        var i = start
        while (i < lines.size) {
            val item = ORDERED_ITEM_REGEX.matchEntire(lines[i].trim()) ?: break
            out.append("<li>")
            out.append(renderInline(item.groupValues[2]))
            out.append("</li>\n")
            i += 1
        }
        out.append("</ol>\n")
        return i
    }

    private fun appendParagraph(
        lines: List<String>,
        start: Int,
        out: StringBuilder,
    ): Int {
        val para = StringBuilder(lines[start])
        var i = start + 1
        while (i < lines.size && !isBlockBoundary(lines, i)) {
            para.append('\n')
            para.append(lines[i])
            i += 1
        }
        out.append("<p>")
        out.append(renderInline(para.toString().replace('\n', ' ')))
        out.append("</p>\n")
        return i
    }

    private fun isBlockBoundary(
        lines: List<String>,
        index: Int,
    ): Boolean {
        val line = lines[index]
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            return true
        }
        if (trimmed.startsWith("<details", ignoreCase = true) || DETAILS_CLOSE_REGEX.matches(trimmed)) {
            return true
        }
        if (FENCE_OPEN_REGEX.matches(trimmed)) {
            return true
        }
        if (HEADING_REGEX.matches(trimmed)) {
            return true
        }
        if (HR_REGEX.matches(trimmed)) {
            return true
        }
        if (BLOCKQUOTE_REGEX.matches(line)) {
            return true
        }
        if (UNORDERED_ITEM_REGEX.matches(trimmed)) {
            return true
        }
        if (ORDERED_ITEM_REGEX.matches(trimmed)) {
            return true
        }
        return isTableHeader(lines, index)
    }

    private fun renderInline(text: String): String {
        val placeholders = mutableListOf<String>()

        fun stash(html: String): String {
            val token = "\uE000${placeholders.size}\uE001"
            placeholders += html
            return token
        }

        var work =
            INLINE_CODE_REGEX.replace(text) { match ->
                stash("<code>${escapeHtml(match.groupValues[1])}</code>")
            }
        work =
            IMAGE_REGEX.replace(work) { match ->
                val alt = escapeHtml(match.groupValues[1])
                val src = rewriteImageSrc(match.groupValues[2].trim())
                stash("""<img src="$src" alt="$alt"/>""")
            }
        work =
            LINK_REGEX.replace(work) { match ->
                val label = escapeHtml(match.groupValues[1])
                val href = rewriteLinkHref(match.groupValues[2].trim())
                stash("""<a href="$href">$label</a>""")
            }
        work = escapeHtml(work)
        work =
            BOLD_REGEX.replace(work) { match ->
                "<strong>${match.groupValues[2]}</strong>"
            }
        work =
            ITALIC_STAR_REGEX.replace(work) { match ->
                "<em>${match.groupValues[1]}</em>"
            }
        work =
            ITALIC_UNDERSCORE_REGEX.replace(work) { match ->
                "<em>${match.groupValues[1]}</em>"
            }
        placeholders.forEachIndexed { index, html ->
            work = work.replace("\uE000$index\uE001", html)
        }
        return work
    }

    /**
     * Turns relative paths into host-absolute `/__notes_local__/…` URLs.
     * The path after the prefix is percent-encoded as one token (slashes → `%2F`)
     * so it round-trips through [decodeLocalImagePath].
     */
    fun rewriteImageSrc(raw: String): String {
        val src = stripUrlTitle(raw.trim().trim('"').trim('\''))
        // Idempotent: convert() already rewrites markdown images; rewriteHtmlImageSources
        // must not encode `/__notes_local__/…` a second time.
        if (src.startsWith(LOCAL_IMAGE_PATH_PREFIX)) {
            return escapeHtml(src)
        }
        if (isExternalImageSrc(src)) {
            return escapeHtml(src)
        }
        val normalized = src.replace('\\', '/').removePrefix("./")
        val token = UriEncode.encode(normalized)
        return escapeHtml(LOCAL_IMAGE_PATH_PREFIX + token)
    }

    /**
     * Replaces `/__notes_local__/…` image URLs with `data:` URIs using [load].
     * [load] receives the vault-relative path (leading `/` means from notes root).
     */
    fun embedLocalImages(
        html: String,
        load: (relativePath: String) -> Pair<String, ByteArray>?,
    ): String = LOCAL_IMAGE_SRC_REGEX.replace(html) { match ->
        val relative =
            decodeLocalImagePath(match.groupValues[1])
                ?: return@replace match.value
        val loaded = load(relative) ?: return@replace match.value
        val (mime, bytes) = loaded
        val base64 = Base64Encoder.encode(bytes)
        """src="data:$mime;base64,$base64""""
    }

    fun decodeLocalImagePath(urlPath: String): String? {
        val prefix = LOCAL_IMAGE_PATH_PREFIX
        if (!urlPath.startsWith(prefix) && !urlPath.startsWith(prefix.trimEnd('/'))) {
            return null
        }
        val token =
            when {
                urlPath.startsWith(prefix) -> urlPath.substring(prefix.length)
                else -> urlPath.substring(prefix.trimEnd('/').length).trimStart('/')
            }
        if (token.isEmpty()) {
            return null
        }
        return UriEncode.decode(token).replace('\\', '/')
    }

    private fun rewriteLinkHref(raw: String): String {
        val href = stripUrlTitle(raw.trim().trim('"').trim('\''))
        if (href.startsWith("#")) {
            val slug = slugify(href.removePrefix("#"))
            return escapeHtml("#$slug")
        }
        return escapeHtml(href)
    }

    private fun stripUrlTitle(value: String): String {
        // Markdown: (url "title") or (url 'title')
        val spaced = value.indexOf(' ')
        if (spaced <= 0) {
            return value
        }
        return value.substring(0, spaced).trim()
    }

    private fun isExternalImageSrc(src: String): Boolean {
        if (src.startsWith("https://", ignoreCase = true)) {
            return true
        }
        if (src.startsWith("http://", ignoreCase = true)) {
            return true
        }
        if (src.startsWith("data:", ignoreCase = true)) {
            return true
        }
        return src.startsWith("content:", ignoreCase = true)
    }

    fun slugify(text: String): String {
        val plain = stripTags(text).trim().lowercase(java.util.Locale.ROOT)
        val stripped = SLUG_STRIP_REGEX.replace(plain, "")
        return SLUG_SPACE_REGEX.replace(stripped, "-").trim('-')
    }

    private fun uniqueSlug(
        base: String,
        counts: MutableMap<String, Int>,
    ): String {
        val key = base.ifEmpty { "section" }
        val seen = counts[key] ?: 0
        counts[key] = seen + 1
        return if (seen == 0) key else "$key-$seen"
    }

    private fun stripTags(text: String): String = text.replace(Regex("<[^>]+>"), "")

    /** Rewrites relative `src` on raw HTML `<img>` tags (e.g. inside details). */
    fun rewriteHtmlImageSources(html: String): String = HTML_IMG_SRC_REGEX.replace(html) { match ->
        val prefix = match.groupValues[1]
        val quote = match.groupValues[2]
        val src = rewriteImageSrc(match.groupValues[3])
        "$prefix$quote$src$quote"
    }

    fun escapeHtml(text: String): String {
        val out = StringBuilder(text.length + 16)
        for (c in text) {
            when (c) {
                '&' -> out.append("&amp;")
                '<' -> out.append("&lt;")
                '>' -> out.append("&gt;")
                '"' -> out.append("&quot;")
                '\'' -> out.append("&#39;")
                else -> out.append(c)
            }
        }
        return out.toString()
    }
}

/** Minimal percent-encoding for path tokens (UTF-8). */
private object UriEncode {
    fun encode(value: String): String {
        val bytes = value.toByteArray(Charsets.UTF_8)
        val out = StringBuilder(bytes.size * 3)
        for (b in bytes) {
            val c = b.toInt() and 0xFF
            if (isUnreserved(c)) {
                out.append(c.toChar())
            } else {
                out.append('%')
                out.append(HEX[c ushr 4])
                out.append(HEX[c and 0x0F])
            }
        }
        return out.toString()
    }

    fun decode(value: String): String {
        if (!value.contains('%')) {
            return value
        }
        val bytes = ArrayList<Byte>(value.length)
        var i = 0
        while (i < value.length) {
            val ch = value[i]
            val encoded =
                ch == '%' &&
                    i + 2 < value.length &&
                    hexValue(value[i + 1]) >= 0 &&
                    hexValue(value[i + 2]) >= 0
            if (encoded) {
                val hi = hexValue(value[i + 1])
                val lo = hexValue(value[i + 2])
                bytes.add(((hi shl 4) or lo).toByte())
                i += 3
            } else {
                bytes.add(ch.code.toByte())
                i += 1
            }
        }
        return bytes.toByteArray().toString(Charsets.UTF_8)
    }

    private fun hexValue(c: Char): Int = when (c) {
        in '0'..'9' -> c - '0'
        in 'a'..'f' -> c - 'a' + 10
        in 'A'..'F' -> c - 'A' + 10
        else -> -1
    }

    private fun isUnreserved(c: Int): Boolean {
        if (c in 'a'.code..'z'.code) {
            return true
        }
        if (c in 'A'.code..'Z'.code) {
            return true
        }
        if (c in '0'.code..'9'.code) {
            return true
        }
        return c == '-'.code || c == '_'.code || c == '.'.code || c == '~'.code
    }

    private val HEX = "0123456789ABCDEF".toCharArray()
}

/** RFC 4648 Base64 without wrapping (for data URIs). */
private object Base64Encoder {
    private val TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray()

    fun encode(data: ByteArray): String {
        val out = StringBuilder((data.size + 2) / 3 * 4)
        var i = 0
        while (i + 2 < data.size) {
            val n =
                ((data[i].toInt() and 0xFF) shl 16) or
                    ((data[i + 1].toInt() and 0xFF) shl 8) or
                    (data[i + 2].toInt() and 0xFF)
            out.append(TABLE[(n shr 18) and 63])
            out.append(TABLE[(n shr 12) and 63])
            out.append(TABLE[(n shr 6) and 63])
            out.append(TABLE[n and 63])
            i += 3
        }
        when (data.size - i) {
            1 -> {
                val n = (data[i].toInt() and 0xFF) shl 16
                out.append(TABLE[(n shr 18) and 63])
                out.append(TABLE[(n shr 12) and 63])
                out.append('=')
                out.append('=')
            }

            2 -> {
                val n =
                    ((data[i].toInt() and 0xFF) shl 16) or
                        ((data[i + 1].toInt() and 0xFF) shl 8)
                out.append(TABLE[(n shr 18) and 63])
                out.append(TABLE[(n shr 12) and 63])
                out.append(TABLE[(n shr 6) and 63])
                out.append('=')
            }
        }
        return out.toString()
    }
}

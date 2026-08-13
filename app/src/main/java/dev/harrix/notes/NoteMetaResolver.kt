package dev.harrix.notes

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Resolve note title and date from Markdown metadata.
 *
 * `@hsk-sync:note-meta` — keep behavior aligned with:
 * - `harrix_pylib.note_meta`
 * - `harrix-swiss-knife/vscode/harrix-notes-explorer-hsk/note-meta.js`
 *
 * Title priority: YAML `title` → first `#` heading → `titleFromId(fileStem)`.
 *
 * Date priority: date in file name → YAML `date` → file ctime → file mtime.
 */
object NoteMetaResolver {
    private val DATE_LINE_REGEX = Regex("^date\\s*:\\s*(.*)$", RegexOption.IGNORE_CASE)
    private val FRONTMATTER_REGEX = Regex("^---\\r?\\n([\\s\\S]*?)\\r?\\n---\\r?\\n?")
    private val DATE_IN_NAME_REGEX =
        Regex(
            "(?:(?<y4>\\d{4})[.\\-](?<m4>\\d{2})[.\\-](?<d4>\\d{2})" +
                "|(?<y8>\\d{4})(?<m8>\\d{2})(?<d8>\\d{2})" +
                "|(?<dEu>\\d{2})\\.(?<mEu>\\d{2})\\.(?<yEu>\\d{4}))",
        )
    private val ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE

    enum class DateSource {
        Filename,
        Yaml,
        FileCtime,
        FileMtime,
    }

    data class ResolvedNoteDate(
        val date: LocalDate,
        val source: DateSource,
        /** Local start-of-day epoch for list/date display. */
        val epochMs: Long,
    )

    fun noteStemFromName(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".g.md") -> fileName.dropLast(5)
            lower.endsWith(".md") -> fileName.dropLast(3)
            else -> fileName.substringBeforeLast('.', fileName)
        }
    }

    fun resolveTitle(
        mdText: String,
        fileStem: String,
    ): String {
        val fromContent = NoteTitleExtractor.extract(mdText)
        if (fromContent.isNotEmpty()) {
            return fromContent
        }
        return titleFromId(fileStem).ifEmpty { "Untitled" }
    }

    /**
     * Universal note display title from Markdown + file name.
     *
     * Priority: YAML `title` → first `#` heading → stem from [fileName] (via [titleFromId]) →
     * `Untitled`. Use this wherever a note label is derived from content on disk or in memory.
     */
    fun resolveNoteTitle(
        mdText: String,
        fileName: String,
    ): String = resolveTitle(mdText, noteStemFromName(fileName))

    /**
     * Title for open tabs / lists respecting [NotesTitleSource].
     * File-name mode uses the raw stem; content mode uses [resolveNoteTitle].
     */
    fun resolveNoteTitle(
        mdText: String,
        fileName: String,
        source: NotesTitleSource,
    ): String {
        val stem = noteStemFromName(fileName)
        return when (source) {
            NotesTitleSource.FileName -> stem.ifEmpty { "Untitled" }
            NotesTitleSource.Content -> resolveTitle(mdText, stem)
        }
    }

    fun titleFromId(fileStem: String): String {
        val stem = fileStem.trim()
        if (stem.isEmpty()) {
            return ""
        }
        val slug = if ("__" in stem) stem.substringAfter("__") else stem
        return pythonTitle(slug.replace("-", " ").replace("_", " "))
    }

    fun resolveDate(
        mdText: String,
        fileName: String,
        ctimeEpochMs: Long? = null,
        mtimeEpochMs: Long? = null,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): ResolvedNoteDate? {
        parseDateFromFileName(fileName)?.let {
            return ResolvedNoteDate(it, DateSource.Filename, localDateToEpochMs(it, zoneId))
        }
        parseDateFromYaml(mdText)?.let {
            return ResolvedNoteDate(it, DateSource.Yaml, localDateToEpochMs(it, zoneId))
        }
        epochMsToLocalDate(ctimeEpochMs, zoneId)?.let {
            return ResolvedNoteDate(it, DateSource.FileCtime, ctimeEpochMs!!)
        }
        epochMsToLocalDate(mtimeEpochMs, zoneId)?.let {
            return ResolvedNoteDate(it, DateSource.FileMtime, mtimeEpochMs!!)
        }
        return null
    }

    fun parseDateFromFileName(fileName: String): LocalDate? {
        val stem = noteStemFromName(fileName.substringAfterLast('/', fileName).substringAfterLast('\\'))
        val match = DATE_IN_NAME_REGEX.find(stem) ?: return null
        return localDateFromMatch(match)
    }

    fun parseDateFromYaml(mdText: String): LocalDate? {
        var src = mdText
        if (src.isNotEmpty() && src[0] == '\uFEFF') {
            src = src.substring(1)
        }
        val fm = FRONTMATTER_REGEX.find(src) ?: return null
        for (line in fm.groupValues[1].lineSequence()) {
            val match = DATE_LINE_REGEX.find(line.trim()) ?: continue
            val parsed = parseDateValue(unquoteYamlScalar(match.groupValues[1]))
            if (parsed != null) {
                return parsed
            }
        }
        return null
    }

    fun parseDateValue(value: String?): LocalDate? {
        val text = value?.trim().orEmpty()
        if (text.isEmpty()) {
            return null
        }
        val token = text.split(Regex("\\s+")).first()
        DATE_IN_NAME_REGEX.find(token)?.let { return localDateFromMatch(it) }
        return try {
            LocalDate.parse(token, ISO_DATE)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun localDateFromMatch(match: MatchResult): LocalDate? {
        val y: Int
        val m: Int
        val d: Int
        when {
            match.groups["y4"] != null -> {
                y = match.groups["y4"]!!.value.toInt()
                m = match.groups["m4"]!!.value.toInt()
                d = match.groups["d4"]!!.value.toInt()
            }

            match.groups["y8"] != null -> {
                y = match.groups["y8"]!!.value.toInt()
                m = match.groups["m8"]!!.value.toInt()
                d = match.groups["d8"]!!.value.toInt()
            }

            match.groups["yEu"] != null -> {
                y = match.groups["yEu"]!!.value.toInt()
                m = match.groups["mEu"]!!.value.toInt()
                d = match.groups["dEu"]!!.value.toInt()
            }

            else -> return null
        }
        return try {
            LocalDate.of(y, m, d)
        } catch (_: Exception) {
            null
        }
    }

    private fun epochMsToLocalDate(
        epochMs: Long?,
        zoneId: ZoneId,
    ): LocalDate? {
        if (epochMs == null || epochMs <= 0L) {
            return null
        }
        return Instant.ofEpochMilli(epochMs).atZone(zoneId).toLocalDate()
    }

    private fun localDateToEpochMs(
        value: LocalDate,
        zoneId: ZoneId,
    ): Long = value.atStartOfDay(zoneId).toInstant().toEpochMilli()

    private fun pythonTitle(text: String): String {
        val out = StringBuilder(text.length)
        var cap = true
        for (ch in text) {
            if (ch.isLetter()) {
                out.append(if (cap) ch.titlecase() else ch.lowercase())
                cap = false
            } else {
                out.append(ch)
                cap = true
            }
        }
        return out.toString()
    }

    private fun unquoteYamlScalar(value: String): String {
        var v = value.trim()
        if (v.length >= 2) {
            val doubleQuoted = v.startsWith("\"") && v.endsWith("\"")
            val singleQuoted = v.startsWith("'") && v.endsWith("'")
            if (doubleQuoted || singleQuoted) {
                v = v.substring(1, v.length - 1)
            }
        }
        return v.trim()
    }
}

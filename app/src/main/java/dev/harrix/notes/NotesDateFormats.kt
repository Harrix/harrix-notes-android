package dev.harrix.notes

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Formats document last-modified times like Markor (`31.07.2026, 11:24`). */
object NotesDateFormats {
    private val listDateTime: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm").withZone(ZoneId.systemDefault())
    private val listDate: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault())

    fun formatListDateTime(epochMs: Long): String = listDateTime.format(Instant.ofEpochMilli(epochMs))

    /** Date-only list caption (YAML / filename dates without a time-of-day). */
    fun formatListDate(epochMs: Long): String = listDate.format(Instant.ofEpochMilli(epochMs))

    /**
     * Format a resolved note date for list rows.
     * File ctime/mtime keep time-of-day; filename/YAML dates are date-only.
     */
    fun formatResolvedNoteDate(resolved: NoteMetaResolver.ResolvedNoteDate): String = when (resolved.source) {
        NoteMetaResolver.DateSource.FileCtime,
        NoteMetaResolver.DateSource.FileMtime,
        -> formatListDateTime(resolved.epochMs)

        NoteMetaResolver.DateSource.Filename,
        NoteMetaResolver.DateSource.Yaml,
        -> formatListDate(resolved.epochMs)
    }

    fun formatByteSize(bytes: Long): String = when {
        bytes < 1024L -> "$bytes B"

        bytes < 1024L * 1024L ->
            String.format(java.util.Locale.ROOT, "%.1f KB", bytes / 1024.0)

        else ->
            String.format(java.util.Locale.ROOT, "%.2f MB", bytes / (1024.0 * 1024.0))
    }
}

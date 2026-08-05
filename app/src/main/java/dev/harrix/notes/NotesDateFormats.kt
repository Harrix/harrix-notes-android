package dev.harrix.notes

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Formats document last-modified times like Markor (`31.07.2026, 11:24`). */
object NotesDateFormats {
    private val listDateTime: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm").withZone(ZoneId.systemDefault())

    fun formatListDateTime(epochMs: Long): String = listDateTime.format(Instant.ofEpochMilli(epochMs))

    fun formatByteSize(bytes: Long): String =
        when {
            bytes < 1024L -> "$bytes B"
            bytes < 1024L * 1024L ->
                String.format(java.util.Locale.ROOT, "%.1f KB", bytes / 1024.0)

            else ->
                String.format(java.util.Locale.ROOT, "%.2f MB", bytes / (1024.0 * 1024.0))
        }
}

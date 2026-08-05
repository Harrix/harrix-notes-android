package dev.harrix.notes

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Formats document last-modified times like Markor (`31.07.2026, 11:24`). */
object NotesDateFormats {
    private val listDateTime: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm").withZone(ZoneId.systemDefault())

    fun formatListDateTime(epochMs: Long): String = listDateTime.format(Instant.ofEpochMilli(epochMs))
}

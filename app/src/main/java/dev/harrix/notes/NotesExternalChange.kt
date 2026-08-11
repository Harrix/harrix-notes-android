package dev.harrix.notes

import android.net.Uri

/** Conflict between the selected open note and the document on disk (external edit/delete). */
sealed class NotesExternalNoteConflict {
    abstract val tab: OpenNoteTab

    /** File still exists but content differs from the open draft / last saved text. */
    data class Modified(
        override val tab: OpenNoteTab,
        val diskLastModifiedEpochMs: Long?,
        val diskSizeBytes: Long?,
        val diskText: String,
    ) : NotesExternalNoteConflict()

    /** Document URI no longer resolves (deleted or moved away). */
    data class Deleted(
        override val tab: OpenNoteTab,
    ) : NotesExternalNoteConflict()
}

/** Snapshot used to detect external edits after load or successful save. */
data class NotesLoadedDocumentBaseline(
    val documentId: String,
    val uri: String,
    val lastModifiedEpochMs: Long?,
    val sizeBytes: Long?,
)

/** Result of probing the selected open note against the document provider. */
sealed class NotesExternalNoteProbe {
    data class Unchanged(
        val displayName: String?,
        val baseline: NotesLoadedDocumentBaseline,
    ) : NotesExternalNoteProbe()

    data class Modified(
        val displayName: String?,
        val diskLastModifiedEpochMs: Long?,
        val diskSizeBytes: Long?,
        val diskText: String,
    ) : NotesExternalNoteProbe()

    data object Missing : NotesExternalNoteProbe()
}

/**
 * Probes [uri] for external changes.
 * Metadata-only differences that still match any of [knownTexts] (draft / last saved) are
 * treated as unchanged so the app's own saves do not raise a conflict dialog.
 */
fun NotesTreeRepository.probeOpenNote(
    uri: Uri,
    documentId: String,
    baseline: NotesLoadedDocumentBaseline?,
    knownTexts: Collection<String>,
): NotesExternalNoteProbe {
    if (!documentExists(uri)) {
        return NotesExternalNoteProbe.Missing
    }
    val info = queryDocumentInfo(uri)
    val diskModified = info?.lastModifiedEpochMs
    val diskSize = info?.sizeBytes
    val nextBaseline =
        NotesLoadedDocumentBaseline(
            documentId = documentId,
            uri = uri.toString(),
            lastModifiedEpochMs = diskModified,
            sizeBytes = diskSize,
        )
    if (baseline == null ||
        baseline.documentId != documentId ||
        baseline.uri != uri.toString()
    ) {
        return NotesExternalNoteProbe.Unchanged(
            displayName = info?.displayName,
            baseline = nextBaseline,
        )
    }
    val metadataChanged =
        when {
            baseline.lastModifiedEpochMs != null && diskModified != null ->
                diskModified != baseline.lastModifiedEpochMs ||
                    (
                        baseline.sizeBytes != null &&
                            diskSize != null &&
                            diskSize != baseline.sizeBytes
                        )

            baseline.sizeBytes != null && diskSize != null ->
                diskSize != baseline.sizeBytes

            else -> false
        }
    if (!metadataChanged) {
        return NotesExternalNoteProbe.Unchanged(
            displayName = info?.displayName,
            baseline = nextBaseline,
        )
    }
    val diskText =
        runCatching { readText(uri) }.getOrElse {
            return NotesExternalNoteProbe.Unchanged(
                displayName = info?.displayName,
                baseline = nextBaseline,
            )
        }
    if (knownTexts.any { it == diskText }) {
        return NotesExternalNoteProbe.Unchanged(
            displayName = info?.displayName,
            baseline = nextBaseline,
        )
    }
    return NotesExternalNoteProbe.Modified(
        displayName = info?.displayName,
        diskLastModifiedEpochMs = diskModified,
        diskSizeBytes = diskSize,
        diskText = diskText,
    )
}

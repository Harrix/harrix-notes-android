package dev.harrix.notes

/** Defaults for canvas-note PNG assets (approx. A4 at ~150 dpi). */
object CanvasNoteDefaults {
    const val WIDTH_PX = 1240
    const val HEIGHT_PX = 1754

    const val IMAGE_FOLDER = "img"
    const val LEGACY_FILE_NAME = "canvas.png"
    const val LEGACY_RELATIVE_PATH = "$IMAGE_FOLDER/$LEGACY_FILE_NAME"

    private val numberedNameRegex =
        Regex("""^canvas_(\d{2})\.png$""", RegexOption.IGNORE_CASE)

    fun pageFileName(pageNumber1Based: Int): String =
        String.format(java.util.Locale.ROOT, "canvas_%02d.png", pageNumber1Based)

    fun pageRelativePath(pageNumber1Based: Int): String =
        "$IMAGE_FOLDER/${pageFileName(pageNumber1Based)}"

    fun parsePageNumber(fileName: String): Int? =
        numberedNameRegex.matchEntire(fileName.trim())?.groupValues?.get(1)?.toIntOrNull()

    fun isLegacyCanvasFile(fileName: String): Boolean =
        fileName.equals(LEGACY_FILE_NAME, ignoreCase = true)

    fun isCanvasPageFile(fileName: String): Boolean =
        parsePageNumber(fileName) != null || isLegacyCanvasFile(fileName)
}

package dev.harrix.notes

/**
 * Canvas page underlay only (does not alter PNG pixels).
 *
 * Default is [Light] so near-black ink stays visible regardless of app theme.
 * Persisted per note as YAML `paper: light|dark|theme`.
 */
enum class CanvasPaperMode(
    /** Value stored in note YAML `paper:` and SharedPreferences. */
    val yamlKey: String,
) {
    /** Fixed light paper ([dev.harrix.notes.ui.theme.LightColorScheme] surface). */
    Light("light"),

    /** Fixed dark paper ([dev.harrix.notes.ui.theme.DarkColorScheme] surface). */
    Dark("dark"),

    /** Follow current Material theme surface. */
    FollowTheme("theme"),
    ;

    companion object {
        val Default: CanvasPaperMode = Light

        fun fromStorageKey(key: String?): CanvasPaperMode {
            val normalized = key?.trim().orEmpty()
            if (normalized.isEmpty()) {
                return Default
            }
            return entries.firstOrNull {
                it.yamlKey.equals(normalized, ignoreCase = true) ||
                    it.name.equals(normalized, ignoreCase = true)
            } ?: Default
        }
    }
}

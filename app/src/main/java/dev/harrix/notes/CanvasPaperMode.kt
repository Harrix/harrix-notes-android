package dev.harrix.notes

/**
 * Canvas page underlay only (does not alter PNG pixels).
 *
 * Default is [Light] so near-black ink stays visible regardless of app theme.
 */
enum class CanvasPaperMode {
    /** Fixed light paper ([dev.harrix.notes.ui.theme.LightColorScheme] surface). */
    Light,

    /** Fixed dark paper ([dev.harrix.notes.ui.theme.DarkColorScheme] surface). */
    Dark,

    /** Follow current Material theme surface. */
    FollowTheme,
    ;

    companion object {
        val Default: CanvasPaperMode = Light

        fun fromStorageKey(key: String?): CanvasPaperMode = entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: Default
    }
}

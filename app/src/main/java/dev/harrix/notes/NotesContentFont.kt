package dev.harrix.notes

/**
 * Font family for Markdown preview / editor WebViews.
 * Bundled faces live under `assets/fonts/` (OFL).
 */
enum class NotesContentFont(
    val storageKey: String,
    /** CSS `font-family` stack for body / editor text. */
    val cssFontFamily: String,
) {
    System(
        storageKey = "system",
        cssFontFamily = """system-ui, -apple-system, "Segoe UI", Roboto, sans-serif""",
    ),
    JetBrainsMono(
        storageKey = "jetbrains_mono",
        cssFontFamily = """"JetBrains Mono", ui-monospace, monospace""",
    ),
    FiraSans(
        storageKey = "fira_sans",
        cssFontFamily = """"Fira Sans", system-ui, sans-serif""",
    ),
    ;

    companion object {
        val Default: NotesContentFont = JetBrainsMono

        fun fromStorageKey(key: String?): NotesContentFont = entries.firstOrNull { it.storageKey.equals(key, ignoreCase = true) }
            ?: entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
            ?: Default
    }
}

/** Shared `@font-face` rules for bundled preview/editor fonts (WebViewAssetLoader URLs). */
object NotesContentFontCss {
    private const val ASSET_FONTS = "https://appassets.androidplatform.net/assets/fonts"

    fun fontFaceRules(): String =
        """
        @font-face {
          font-family: "JetBrains Mono";
          src: url("$ASSET_FONTS/JetBrainsMono-Regular.woff2") format("woff2");
          font-weight: 400;
          font-style: normal;
          font-display: swap;
        }
        @font-face {
          font-family: "JetBrains Mono";
          src: url("$ASSET_FONTS/JetBrainsMono-Italic.woff2") format("woff2");
          font-weight: 400;
          font-style: italic;
          font-display: swap;
        }
        @font-face {
          font-family: "JetBrains Mono";
          src: url("$ASSET_FONTS/JetBrainsMono-Medium.woff2") format("woff2");
          font-weight: 500;
          font-style: normal;
          font-display: swap;
        }
        @font-face {
          font-family: "JetBrains Mono";
          src: url("$ASSET_FONTS/JetBrainsMono-MediumItalic.woff2") format("woff2");
          font-weight: 500;
          font-style: italic;
          font-display: swap;
        }
        @font-face {
          font-family: "JetBrains Mono";
          src: url("$ASSET_FONTS/JetBrainsMono-Bold.woff2") format("woff2");
          font-weight: 700;
          font-style: normal;
          font-display: swap;
        }
        @font-face {
          font-family: "Fira Sans";
          src: url("$ASSET_FONTS/FiraSans-Regular.woff2") format("woff2");
          font-weight: 400;
          font-style: normal;
          font-display: swap;
        }
        @font-face {
          font-family: "Fira Sans";
          src: url("$ASSET_FONTS/FiraSans-Italic.woff2") format("woff2");
          font-weight: 400;
          font-style: italic;
          font-display: swap;
        }
        @font-face {
          font-family: "Fira Sans";
          src: url("$ASSET_FONTS/FiraSans-Medium.woff2") format("woff2");
          font-weight: 500;
          font-style: normal;
          font-display: swap;
        }
        @font-face {
          font-family: "Fira Sans";
          src: url("$ASSET_FONTS/FiraSans-MediumItalic.woff2") format("woff2");
          font-weight: 500;
          font-style: italic;
          font-display: swap;
        }
        @font-face {
          font-family: "Fira Sans";
          src: url("$ASSET_FONTS/FiraSans-Bold.woff2") format("woff2");
          font-weight: 700;
          font-style: normal;
          font-display: swap;
        }
        """.trimIndent()
}

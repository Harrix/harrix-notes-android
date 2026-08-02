package dev.harrix.notes.ui.notes

import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Temporary note **preview** mode (not the editor).
 *
 * Renders note source as the simplest possible HTML page: the full text is placed
 * inside a single `<pre>` with no Markdown/HTML processing. The slightly bluish
 * page background marks preview mode; later this can become a real HTML preview.
 *
 * Text is selectable for copy; the document is not editable here.
 */
@Composable
fun NotesHtmlPreviewPane(
    isLoading: Boolean,
    content: String?,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    when {
        isLoading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        errorMessage != null && content == null -> {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = modifier.padding(24.dp),
            )
        }

        else -> {
            val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
            val colors =
                remember(darkTheme) {
                    if (darkTheme) PreviewHtmlColors.Dark else PreviewHtmlColors.Light
                }
            val html =
                remember(content, colors) {
                    buildRawPreHtml(content.orEmpty(), colors)
                }
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        // Preview only: no scripts, selection stays enabled for copy.
                        settings.javaScriptEnabled = false
                        settings.domStorageEnabled = false
                        isVerticalScrollBarEnabled = true
                        setBackgroundColor(colors.pageBackground.toArgb())
                    }
                },
                update = { webView ->
                    webView.setBackgroundColor(colors.pageBackground.toArgb())
                    val tag = webView.tag as? String
                    if (tag != html) {
                        webView.tag = html
                        webView.loadDataWithBaseURL(
                            null,
                            html,
                            "text/html",
                            Charsets.UTF_8.name(),
                            null,
                        )
                    }
                },
                modifier = modifier.fillMaxSize(),
            )
        }
    }
}

/** Bluish preview chrome — intentionally distinct from the editor surface. */
private data class PreviewHtmlColors(
    val pageBackground: Color,
    val text: Color,
) {
    companion object {
        val Light =
            PreviewHtmlColors(
                pageBackground = Color(0xFFE8F2F8),
                text = Color(0xFF171C1F),
            )
        val Dark =
            PreviewHtmlColors(
                pageBackground = Color(0xFF152029),
                text = Color(0xFFDDE2E6),
            )
    }
}

/**
 * Builds a minimal HTML document that shows [source] inside `<pre>`.
 * Only escapes `&`, `<`, `>` so the WebView displays the raw note text safely —
 * this is not Markdown processing.
 */
private fun buildRawPreHtml(
    source: String,
    colors: PreviewHtmlColors,
): String {
    val escaped = escapeHtmlForPre(source)
    val bg = colors.pageBackground.toCssHex()
    val fg = colors.text.toCssHex()
    return """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="utf-8"/>
        <meta name="viewport" content="width=device-width, initial-scale=1"/>
        <style>
          html, body {
            margin: 0;
            padding: 0;
            background: $bg;
          }
          pre {
            margin: 0;
            padding: 16px;
            white-space: pre-wrap;
            word-wrap: break-word;
            font-family: monospace;
            font-size: 14px;
            line-height: 1.45;
            color: $fg;
            background: $bg;
          }
        </style>
        </head>
        <body><pre>$escaped</pre></body>
        </html>
        """.trimIndent()
}

private fun escapeHtmlForPre(text: String): String {
    val out = StringBuilder(text.length + 16)
    for (c in text) {
        when (c) {
            '&' -> out.append("&amp;")
            '<' -> out.append("&lt;")
            '>' -> out.append("&gt;")
            else -> out.append(c)
        }
    }
    return out.toString()
}

private fun Color.toCssHex(): String {
    val argb = toArgb()
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return "#%02X%02X%02X".format(r, g, b)
}

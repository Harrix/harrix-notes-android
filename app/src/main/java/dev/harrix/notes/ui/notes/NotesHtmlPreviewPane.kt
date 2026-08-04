package dev.harrix.notes.ui.notes

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.harrix.notes.AppPreferences
import dev.harrix.notes.NotesPathSegment
import dev.harrix.notes.NotesRelativeDocuments
import dev.harrix.notes.NotesViewerPreferences
import dev.harrix.notes.SimpleMarkdownToHtml
import java.io.ByteArrayInputStream
import java.util.Locale

/**
 * Note **preview** mode: simple custom Markdown → HTML in a WebView.
 *
 * White page background; YAML front matter is collapsed in `<details>`.
 * Relative images load from the notes SAF tree via [SimpleMarkdownToHtml.LOCAL_IMAGE_SCHEME].
 */
@Composable
fun NotesHtmlPreviewPane(
    isLoading: Boolean,
    content: String?,
    errorMessage: String?,
    modifier: Modifier = Modifier,
    fontSizeSp: Int = NotesViewerPreferences.DEFAULT_PREVIEW_FONT_SIZE_SP,
    treeUri: Uri? = null,
    folderPath: List<NotesPathSegment> = emptyList(),
) {
    val context = LocalContext.current
    val resolver = remember(context) { context.applicationContext.contentResolver }

    Box(modifier = modifier.fillMaxSize().clipToBounds()) {
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null && content == null -> {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(24.dp),
                )
            }

            else -> {
                val html =
                    remember(content, fontSizeSp) {
                        buildPreviewHtml(content.orEmpty(), fontSizeSp)
                    }
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            createPreviewWebView(ctx, resolver, treeUri, folderPath)
                        },
                        update = { webView ->
                            val holder = webView.tag as? PreviewWebViewTag
                            val next =
                                PreviewWebViewTag(
                                    html = html,
                                    treeUri = treeUri,
                                    folderPath = folderPath,
                                )
                            if (holder?.html != html ||
                                holder.treeUri != treeUri ||
                                holder.folderPath != folderPath
                            ) {
                                webView.tag = next
                                (webView.webViewClient as? PreviewWebViewClient)?.updateTarget(
                                    treeUri,
                                    folderPath,
                                )
                                webView.loadDataWithBaseURL(
                                    "https://notes.preview.local/",
                                    html,
                                    "text/html",
                                    Charsets.UTF_8.name(),
                                    null,
                                )
                            }
                        },
                        modifier =
                        Modifier
                            .width(maxWidth)
                            .height(maxHeight),
                    )
                }
            }
        }
    }
}

private data class PreviewWebViewTag(
    val html: String,
    val treeUri: Uri?,
    val folderPath: List<NotesPathSegment>,
)

private fun createPreviewWebView(
    context: Context,
    resolver: ContentResolver,
    treeUri: Uri?,
    folderPath: List<NotesPathSegment>,
): WebView {
    val client = PreviewWebViewClient(resolver, treeUri, folderPath)
    return WebView(context).apply {
        settings.javaScriptEnabled = false
        settings.domStorageEnabled = false
        settings.loadsImagesAutomatically = true
        settings.blockNetworkImage = false
        isVerticalScrollBarEnabled = true
        setBackgroundColor(Color.White.toArgb())
        webViewClient = client
        tag =
            PreviewWebViewTag(
                html = "",
                treeUri = treeUri,
                folderPath = folderPath,
            )
    }
}

private class PreviewWebViewClient(
    private val resolver: ContentResolver,
    private var treeUri: Uri?,
    private var folderPath: List<NotesPathSegment>,
) : WebViewClient() {
    fun updateTarget(
        treeUri: Uri?,
        folderPath: List<NotesPathSegment>,
    ) {
        this.treeUri = treeUri
        this.folderPath = folderPath
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        val url = request.url
        if (url.scheme != SimpleMarkdownToHtml.LOCAL_IMAGE_SCHEME) {
            return null
        }
        val tree = treeUri ?: return notFound()
        val relative =
            url.path
                ?.trimStart('/')
                ?.let { Uri.decode(it) }
                ?.replace('\\', '/')
                ?: return notFound()
        val docUri =
            NotesRelativeDocuments.resolve(resolver, tree, folderPath, relative)
                ?: return notFound()
        val bytes = NotesRelativeDocuments.readBytes(resolver, docUri) ?: return notFound()
        val mime = guessImageMime(relative)
        return WebResourceResponse(
            mime,
            null,
            ByteArrayInputStream(bytes),
        )
    }

    private fun notFound(): WebResourceResponse = WebResourceResponse(
        "text/plain",
        "UTF-8",
        404,
        "Not Found",
        emptyMap(),
        ByteArrayInputStream(ByteArray(0)),
    )
}

private fun guessImageMime(path: String): String {
    val lower = path.lowercase(Locale.ROOT)
    return when {
        lower.endsWith(".png") -> "image/png"
        lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
        lower.endsWith(".gif") -> "image/gif"
        lower.endsWith(".webp") -> "image/webp"
        lower.endsWith(".svg") -> "image/svg+xml"
        lower.endsWith(".avif") -> "image/avif"
        lower.endsWith(".ico") -> "image/x-icon"
        else -> "application/octet-stream"
    }
}

/**
 * Builds a full HTML document from Markdown [source] with a white page background.
 */
private fun buildPreviewHtml(
    source: String,
    fontSizeSp: Int,
): String {
    val body = SimpleMarkdownToHtml.convert(source)
    val size =
        fontSizeSp.coerceIn(AppPreferences.MIN_FONT_SIZE_SP, AppPreferences.MAX_FONT_SIZE_SP)
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
            background: #ffffff;
            color: #1a1a1a;
            height: 100%;
          }
          body {
            padding: 16px;
            font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
            font-size: ${size}px;
            line-height: 1.5;
            word-wrap: break-word;
            overflow-wrap: break-word;
          }
          h1, h2, h3, h4, h5, h6 {
            line-height: 1.25;
            margin: 1.1em 0 0.5em;
          }
          h1 { font-size: 1.6em; }
          h2 { font-size: 1.35em; }
          h3 { font-size: 1.2em; }
          p, ul, ol, blockquote, pre, details {
            margin: 0 0 0.85em;
          }
          ul, ol { padding-left: 1.4em; }
          blockquote {
            border-left: 3px solid #cccccc;
            padding-left: 0.8em;
            color: #444444;
          }
          code {
            font-family: ui-monospace, "Cascadia Code", Consolas, monospace;
            font-size: 0.92em;
            background: #f3f3f3;
            padding: 0.1em 0.35em;
            border-radius: 3px;
          }
          pre {
            background: #f3f3f3;
            padding: 12px;
            border-radius: 6px;
            overflow-x: auto;
          }
          pre code {
            background: transparent;
            padding: 0;
          }
          img {
            max-width: 100%;
            height: auto;
            display: block;
            margin: 0.6em 0;
          }
          a { color: #0b57d0; }
          hr {
            border: none;
            border-top: 1px solid #dddddd;
            margin: 1.2em 0;
          }
          details.frontmatter {
            background: #f7f7f7;
            border: 1px solid #e4e4e4;
            border-radius: 6px;
            padding: 8px 12px;
            color: #555555;
          }
          details.frontmatter summary {
            cursor: pointer;
            font-weight: 600;
          }
          details.frontmatter pre {
            margin: 8px 0 0;
            white-space: pre-wrap;
            font-size: 0.85em;
            background: transparent;
            padding: 0;
          }
        </style>
        </head>
        <body>$body</body>
        </html>
    """.trimIndent()
}

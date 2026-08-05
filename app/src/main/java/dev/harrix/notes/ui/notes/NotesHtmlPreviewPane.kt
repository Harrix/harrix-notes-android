package dev.harrix.notes.ui.notes

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.Locale
import kotlin.coroutines.coroutineContext

private const val PREVIEW_BASE_URL = "https://appassets.androidplatform.net/"
private const val PREVIEW_HOST = "appassets.androidplatform.net"

/**
 * Above this source length, skip image embedding (UTF-16 chars ≈ file bytes for ASCII).
 * A ~7 MB combined notes file would otherwise OOM while base64-embedding images into HTML.
 */
private const val PREVIEW_FULL_MAX_SOURCE_CHARS = 2_000_000

/** Wall-clock budget for full preview (markdown + image embed); then simplified mode. */
private const val PREVIEW_BUILD_TIMEOUT_MS = 7_000L

/** Cap on total raw image bytes embedded as `data:` URIs. */
private const val PREVIEW_EMBED_MAX_TOTAL_BYTES = 3L * 1024 * 1024

/** Skip a single image larger than this. */
private const val PREVIEW_EMBED_MAX_SINGLE_BYTES = 1_500_000

/** Keep at least this much free heap before embedding another image. */
private const val PREVIEW_MIN_FREE_HEAP_BYTES = 24L * 1024 * 1024

/**
 * Note **preview** mode: simple custom Markdown → HTML in a WebView.
 *
 * White page background; YAML front matter is collapsed in `<details>`.
 * Local images are embedded as `data:` URIs (SAF cannot be loaded by WebView
 * as plain file/content URLs). Huge notes fall back to simplified preview
 * without images to avoid OOM.
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
    noteDocumentId: String? = null,
) {
    val context = LocalContext.current
    val resolver = remember(context) { context.applicationContext.contentResolver }
    var html by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(content, fontSizeSp, treeUri, folderPath, noteDocumentId) {
        // Do not build from null content: that left a non-null empty HTML and hid
        // the spinner when the real note arrived (first open from the browser).
        if (content == null) {
            html = null
            return@LaunchedEffect
        }
        html = null
        html =
            withContext(Dispatchers.IO) {
                buildPreviewHtml(
                    source = content,
                    fontSizeSp = fontSizeSp,
                    resolver = resolver,
                    treeUri = treeUri,
                    folderPath = folderPath,
                    noteDocumentId = noteDocumentId,
                )
            }
    }

    Box(modifier = modifier.fillMaxSize().clipToBounds()) {
        when {
            isLoading || (content != null && html == null) -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    NotesLoadingIndicator()
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
                val document = html.orEmpty()
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx -> createPreviewWebView(ctx) },
                        update = { webView ->
                            val tag = webView.tag as? String
                            if (tag != document) {
                                webView.tag = document
                                webView.loadDataWithBaseURL(
                                    PREVIEW_BASE_URL,
                                    document,
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

@SuppressLint("SetJavaScriptEnabled")
private fun createPreviewWebView(context: Context): WebView = WebView(context).apply {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = false
    settings.loadsImagesAutomatically = true
    settings.blockNetworkImage = false
    isVerticalScrollBarEnabled = true
    setBackgroundColor(Color.White.toArgb())
    webViewClient = PreviewWebViewClient()
    tag = ""
}

private class PreviewWebViewClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean {
        val url = request.url
        val fragment = url.fragment
        val isPreviewHost =
            url.scheme.equals("https", ignoreCase = true) &&
                url.host.equals(PREVIEW_HOST, ignoreCase = true)
        if (isPreviewHost && !fragment.isNullOrEmpty()) {
            scrollToAnchor(view, fragment)
            return true
        }
        if (url.scheme.equals("http", ignoreCase = true) ||
            url.scheme.equals("https", ignoreCase = true)
        ) {
            return false
        }
        return true
    }

    private fun scrollToAnchor(
        view: WebView,
        rawFragment: String,
    ) {
        val id = SimpleMarkdownToHtml.slugify(Uri.decode(rawFragment))
        val safeId =
            id
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "")
                .replace("\r", "")
        view.evaluateJavascript(
            """
            (function() {
              var el = document.getElementById('$safeId');
              if (el) { el.scrollIntoView({block:'start'}); }
            })();
            """.trimIndent(),
            null,
        )
    }
}

private suspend fun buildPreviewHtml(
    source: String,
    fontSizeSp: Int,
    resolver: ContentResolver,
    treeUri: Uri?,
    folderPath: List<NotesPathSegment>,
    noteDocumentId: String?,
): String {
    val size =
        fontSizeSp.coerceIn(AppPreferences.MIN_FONT_SIZE_SP, AppPreferences.MAX_FONT_SIZE_SP)
    val startedAt = SystemClock.elapsedRealtime()
    return try {
        val bodyBeforeEmbed = markdownBodyWithoutEmbeddedImages(source)
        coroutineContext.ensureActive()
        val elapsedMs = SystemClock.elapsedRealtime() - startedAt
        val forceSimplified =
            source.length >= PREVIEW_FULL_MAX_SOURCE_CHARS ||
                elapsedMs >= PREVIEW_BUILD_TIMEOUT_MS
        val body =
            when {
                forceSimplified ->
                    SimpleMarkdownToHtml.replaceImagesWithPlaceholder(bodyBeforeEmbed)

                treeUri == null ->
                    SimpleMarkdownToHtml.replaceLocalImagesWithPlaceholder(bodyBeforeEmbed)

                else ->
                    embedImagesWithLimits(
                        bodyBeforeEmbed = bodyBeforeEmbed,
                        resolver = resolver,
                        treeUri = treeUri,
                        folderPath = folderPath,
                        noteDocumentId = noteDocumentId,
                        timeoutMs =
                        (PREVIEW_BUILD_TIMEOUT_MS - elapsedMs).coerceAtLeast(1L),
                    )
            }
        wrapPreviewHtml(body, size)
    } catch (_: OutOfMemoryError) {
        System.gc()
        val snippet = source.take(200_000)
        val body =
            runCatching {
                SimpleMarkdownToHtml.replaceImagesWithPlaceholder(
                    markdownBodyWithoutEmbeddedImages(snippet),
                )
            }.getOrElse {
                "<p>${SimpleMarkdownToHtml.escapeHtml(snippet)}</p>"
            }
        wrapPreviewHtml(body, size)
    }
}

private fun markdownBodyWithoutEmbeddedImages(source: String): String {
    var body = SimpleMarkdownToHtml.convert(source)
    body = SimpleMarkdownToHtml.rewriteHtmlImageSources(body)
    return body
}

private suspend fun embedImagesWithLimits(
    bodyBeforeEmbed: String,
    resolver: ContentResolver,
    treeUri: Uri,
    folderPath: List<NotesPathSegment>,
    noteDocumentId: String?,
    timeoutMs: Long,
): String {
    val deadlineMs = SystemClock.elapsedRealtime() + timeoutMs
    val budget =
        PreviewEmbedBudget(
            deadlineMs = deadlineMs,
            maxTotalBytes = PREVIEW_EMBED_MAX_TOTAL_BYTES,
            maxSingleBytes = PREVIEW_EMBED_MAX_SINGLE_BYTES,
            minFreeHeapBytes = PREVIEW_MIN_FREE_HEAP_BYTES,
        )
    return try {
        withTimeout(timeoutMs) {
            var hitLimit = false
            val embedded =
                SimpleMarkdownToHtml.embedLocalImages(bodyBeforeEmbed) { relativePath ->
                    ensureActive()
                    if (!budget.canEmbedMore()) {
                        hitLimit = true
                        return@embedLocalImages null
                    }
                    val loaded =
                        loadLocalImage(
                            resolver,
                            treeUri,
                            folderPath,
                            noteDocumentId,
                            relativePath,
                        )
                    if (loaded == null) {
                        return@embedLocalImages null
                    }
                    if (!budget.accept(loaded.second.size)) {
                        hitLimit = true
                        return@embedLocalImages null
                    }
                    loaded
                }
            when {
                hitLimit ->
                    SimpleMarkdownToHtml.replaceImagesWithPlaceholder(bodyBeforeEmbed)

                embedded.contains(SimpleMarkdownToHtml.LOCAL_IMAGE_PATH_PREFIX) ->
                    SimpleMarkdownToHtml.replaceLocalImagesWithPlaceholder(embedded)

                else -> embedded
            }
        }
    } catch (_: TimeoutCancellationException) {
        SimpleMarkdownToHtml.replaceImagesWithPlaceholder(bodyBeforeEmbed)
    } catch (_: OutOfMemoryError) {
        System.gc()
        SimpleMarkdownToHtml.replaceImagesWithPlaceholder(bodyBeforeEmbed)
    }
}

private class PreviewEmbedBudget(
    private val deadlineMs: Long,
    private val maxTotalBytes: Long,
    private val maxSingleBytes: Int,
    private val minFreeHeapBytes: Long,
) {
    private var usedBytes = 0L

    fun canEmbedMore(): Boolean {
        if (SystemClock.elapsedRealtime() >= deadlineMs) {
            return false
        }
        if (usedBytes >= maxTotalBytes) {
            return false
        }
        return freeHeapBytes() >= minFreeHeapBytes
    }

    fun accept(byteCount: Int): Boolean {
        if (byteCount > maxSingleBytes) {
            return false
        }
        if (usedBytes + byteCount > maxTotalBytes) {
            return false
        }
        if (freeHeapBytes() < minFreeHeapBytes + byteCount) {
            return false
        }
        if (SystemClock.elapsedRealtime() >= deadlineMs) {
            return false
        }
        usedBytes += byteCount
        return true
    }

    private fun freeHeapBytes(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
    }
}

private fun wrapPreviewHtml(
    body: String,
    fontSizePx: Int,
): String = """
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
        font-size: ${fontSizePx}px;
        line-height: 1.5;
        word-wrap: break-word;
        overflow-wrap: break-word;
      }
      h1, h2, h3, h4, h5, h6 {
        line-height: 1.25;
        margin: 1.1em 0 0.5em;
        scroll-margin-top: 8px;
      }
      h1 { font-size: 1.6em; }
      h2 { font-size: 1.35em; }
      h3 { font-size: 1.2em; }
      p, ul, ol, blockquote, pre, details, .table-wrap {
        margin: 0 0 0.85em;
      }
      ul, ol { padding-left: 1.4em; }
      blockquote {
        border-left: 3px solid #cccccc;
        padding-left: 0.8em;
        color: #444444;
      }
      .table-wrap {
        overflow-x: auto;
        -webkit-overflow-scrolling: touch;
      }
      table {
        border-collapse: collapse;
        width: 100%;
        font-size: 0.95em;
      }
      th, td {
        border: 1px solid #dddddd;
        padding: 6px 10px;
        vertical-align: top;
      }
      th {
        background: #f3f3f3;
        font-weight: 600;
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
        white-space: pre-wrap;
        word-break: break-word;
      }
      pre code {
        background: transparent;
        padding: 0;
        font-size: 0.92em;
        white-space: inherit;
      }
      img {
        max-width: 100%;
        height: auto;
        display: block;
        margin: 0.6em 0;
      }
      .img-placeholder {
        background: #e0e0e0;
        color: #555555;
        padding: 12px 16px;
        margin: 0.6em 0;
        border-radius: 4px;
        font-size: 0.9em;
        text-align: center;
      }
      a { color: #0b57d0; }
      hr {
        border: none;
        border-top: 1px solid #dddddd;
        margin: 1.2em 0;
      }
      details {
        background: #f7f7f7;
        border: 1px solid #e4e4e4;
        border-radius: 6px;
        padding: 8px 12px;
      }
      details.frontmatter {
        color: #555555;
      }
      details summary {
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

private fun loadLocalImage(
    resolver: ContentResolver,
    treeUri: Uri,
    folderPath: List<NotesPathSegment>,
    noteDocumentId: String?,
    relativePath: String,
): Pair<String, ByteArray>? {
    val docUri =
        NotesRelativeDocuments.resolve(
            resolver = resolver,
            treeUri = treeUri,
            folderPath = folderPath,
            relativePath = relativePath,
            noteDocumentId = noteDocumentId,
        ) ?: return null
    val bytes = NotesRelativeDocuments.readBytes(resolver, docUri) ?: return null
    if (bytes.isEmpty()) {
        return null
    }
    return guessImageMime(relativePath) to bytes
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
        else -> "image/*"
    }
}

package dev.harrix.notes.ui.notes

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import dev.harrix.notes.AppPreferences
import dev.harrix.notes.NotesContentFont
import dev.harrix.notes.NotesContentFontCss
import dev.harrix.notes.NotesPathSegment
import dev.harrix.notes.NotesRelativeDocuments
import dev.harrix.notes.NotesViewerPreferences
import dev.harrix.notes.R
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
 * Colors follow the app light/dark theme. YAML front matter is collapsed in
 * `<details>`. Local images are embedded as `data:` URIs (SAF cannot be loaded
 * by WebView as plain file/content URLs). Huge notes fall back to simplified
 * preview without images to avoid OOM.
 */
@Immutable
private data class NotesPreviewPalette(
    val dark: Boolean,
    val background: Color,
    val foreground: Color,
    val muted: Color,
    val border: Color,
    val codeBackground: Color,
    val quoteBorder: Color,
    val link: Color,
    val placeholderBackground: Color,
    val placeholderForeground: Color,
    val detailsBackground: Color,
)

@Composable
private fun rememberNotesPreviewPalette(): NotesPreviewPalette {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    return remember(dark, scheme.surface, scheme.onSurface, scheme.primary) {
        NotesPreviewPalette(
            dark = dark,
            background = scheme.surface,
            foreground = scheme.onSurface,
            muted = scheme.onSurfaceVariant,
            border = scheme.outlineVariant,
            codeBackground = scheme.surfaceVariant,
            quoteBorder = scheme.outline,
            link = scheme.primary,
            placeholderBackground = scheme.surfaceVariant,
            placeholderForeground = scheme.onSurfaceVariant,
            detailsBackground = scheme.surfaceVariant.copy(alpha = if (dark) 0.55f else 0.65f),
        )
    }
}

@Composable
fun NotesHtmlPreviewPane(
    isLoading: Boolean,
    content: String?,
    errorMessage: String?,
    modifier: Modifier = Modifier,
    fontSizeSp: Int = NotesViewerPreferences.DEFAULT_PREVIEW_FONT_SIZE_SP,
    font: NotesContentFont = NotesContentFont.Default,
    treeUri: Uri? = null,
    folderPath: List<NotesPathSegment> = emptyList(),
    noteDocumentId: String? = null,
) {
    val context = LocalContext.current
    val resolver = remember(context) { context.applicationContext.contentResolver }
    val palette = rememberNotesPreviewPalette()
    var html by remember { mutableStateOf<String?>(null) }
    var scrollMetrics by remember { mutableStateOf(NotesScrollMetrics()) }
    var previewWebView by remember { mutableStateOf<NotesPreviewWebView?>(null) }
    var simplifiedNotifiedFor by remember { mutableStateOf<String?>(null) }
    val onScrollMetrics by rememberUpdatedState<(NotesScrollMetrics) -> Unit> { metrics ->
        scrollMetrics = metrics
    }
    val scrollMetricsSink =
        remember {
            object {
                var emit: (NotesScrollMetrics) -> Unit = {}
            }
        }
    scrollMetricsSink.emit = onScrollMetrics

    LaunchedEffect(content, fontSizeSp, font, treeUri, folderPath, noteDocumentId, palette) {
        // Do not build from null content: that left a non-null empty HTML and hid
        // the spinner when the real note arrived (first open from the browser).
        if (content == null) {
            html = null
            scrollMetrics = NotesScrollMetrics()
            simplifiedNotifiedFor = null
            return@LaunchedEffect
        }
        val showSpinner = html == null
        if (showSpinner) {
            scrollMetrics = NotesScrollMetrics()
        }
        val built =
            withContext(Dispatchers.IO) {
                buildPreviewHtml(
                    source = content,
                    fontSizeSp = fontSizeSp,
                    font = font,
                    resolver = resolver,
                    treeUri = treeUri,
                    folderPath = folderPath,
                    noteDocumentId = noteDocumentId,
                    palette = palette,
                )
            }
        html = built.html
        if (built.simplified) {
            val notifyKey = noteDocumentId ?: content
            if (simplifiedNotifiedFor != notifyKey) {
                simplifiedNotifiedFor = notifyKey
                Toast
                    .makeText(
                        context,
                        R.string.markdown_notes_preview_simplified,
                        Toast.LENGTH_LONG,
                    ).show()
            }
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
                        factory = { ctx ->
                            createPreviewWebView(
                                context = ctx,
                                background = palette.background,
                                onScrollMetrics = { metrics ->
                                    scrollMetricsSink.emit(metrics)
                                },
                            ).also { previewWebView = it }
                        },
                        update = { webView ->
                            previewWebView = webView as NotesPreviewWebView
                            webView.setBackgroundColor(palette.background.toArgb())
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
                        onRelease = { webView ->
                            if (previewWebView === webView) {
                                previewWebView = null
                            }
                            webView.destroy()
                        },
                        modifier =
                        Modifier
                            .width(maxWidth)
                            .height(maxHeight),
                    )
                    if (scrollMetrics.canScroll) {
                        NotesFingerScrollbar(
                            scrollOffset = scrollMetrics.scrollOffset,
                            maxScrollOffset = scrollMetrics.maxScrollOffset,
                            viewportSize = scrollMetrics.viewportSize,
                            contentSize = scrollMetrics.contentSize,
                            onScrollOffsetChange = { offset ->
                                previewWebView?.scrollTo(0, offset.toScrollPxInt())
                            },
                            modifier =
                            Modifier
                                .align(Alignment.CenterEnd)
                                .padding(vertical = 8.dp, horizontal = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createPreviewWebView(
    context: Context,
    background: Color,
    onScrollMetrics: (NotesScrollMetrics) -> Unit,
): NotesPreviewWebView {
    val assetLoader =
        WebViewAssetLoader
            .Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
    return NotesPreviewWebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = false
        settings.loadsImagesAutomatically = true
        settings.blockNetworkImage = false
        settings.textZoom = 100
        // Native bar is too thin for fingers; Compose overlay handles scrubbing.
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        setBackgroundColor(background.toArgb())
        webViewClient =
            PreviewWebViewClient(
                assetLoader = assetLoader,
                onContentReady = { view ->
                    view.post { onScrollMetrics(view.notesScrollMetrics()) }
                },
            )
        setOnScrollChangeListener { view, _, _, _, _ ->
            onScrollMetrics((view as NotesPreviewWebView).notesScrollMetrics())
        }
        tag = ""
    }
}

/** Exposes protected scroll-range APIs for the Compose finger scrollbar. */
private class NotesPreviewWebView(
    context: Context,
) : WebView(context) {
    fun notesScrollMetrics(): NotesScrollMetrics = NotesScrollMetrics.fromWebView(
        scrollY = scrollY,
        computeVerticalScrollRange = computeVerticalScrollRange(),
        computeVerticalScrollExtent = computeVerticalScrollExtent(),
    )
}

private class PreviewWebViewClient(
    private val assetLoader: WebViewAssetLoader,
    private val onContentReady: (NotesPreviewWebView) -> Unit,
) : WebViewClient() {
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ) = assetLoader.shouldInterceptRequest(request.url)

    override fun onPageFinished(
        view: WebView?,
        url: String?,
    ) {
        val preview = view as? NotesPreviewWebView ?: return
        onContentReady(preview)
        // Images / layout can change scroll range after the first paint.
        preview.postDelayed({ onContentReady(preview) }, 250)
        preview.postDelayed({ onContentReady(preview) }, 1_000)
    }

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean {
        val url = request.url
        val fragment = url.fragment
        val isPreviewHost =
            url.scheme.equals("https", ignoreCase = true) &&
                url.host.equals(PREVIEW_HOST, ignoreCase = true)
        if (isPreviewHost) {
            if (!fragment.isNullOrEmpty()) {
                scrollToAnchor(view as NotesPreviewWebView, fragment)
            }
            return true
        }
        if (url.scheme.equals("http", ignoreCase = true) ||
            url.scheme.equals("https", ignoreCase = true)
        ) {
            openInBrowser(view.context, url)
            return true
        }
        return true
    }

    private fun openInBrowser(
        context: Context,
        url: Uri,
    ) {
        runCatching {
            val intent =
                Intent(Intent.ACTION_VIEW, url).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
        }
    }

    private fun scrollToAnchor(
        view: NotesPreviewWebView,
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
        ) {
            view.post { onContentReady(view) }
        }
    }
}

private data class PreviewHtmlBuild(
    val html: String,
    val simplified: Boolean,
)

private suspend fun buildPreviewHtml(
    source: String,
    fontSizeSp: Int,
    font: NotesContentFont,
    resolver: ContentResolver,
    treeUri: Uri?,
    folderPath: List<NotesPathSegment>,
    noteDocumentId: String?,
    palette: NotesPreviewPalette,
): PreviewHtmlBuild {
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
        val embedded =
            when {
                forceSimplified ->
                    PreviewBody(
                        html = SimpleMarkdownToHtml.replaceImagesWithPlaceholder(bodyBeforeEmbed),
                        simplified = true,
                    )

                treeUri == null ->
                    PreviewBody(
                        html = SimpleMarkdownToHtml.replaceLocalImagesWithPlaceholder(bodyBeforeEmbed),
                        simplified = false,
                    )

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
        PreviewHtmlBuild(
            html = wrapPreviewHtml(embedded.html, size, font, palette),
            simplified = embedded.simplified,
        )
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
        PreviewHtmlBuild(
            html = wrapPreviewHtml(body, size, font, palette),
            simplified = true,
        )
    }
}

private data class PreviewBody(
    val html: String,
    val simplified: Boolean,
)

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
): PreviewBody {
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
                    PreviewBody(
                        html = SimpleMarkdownToHtml.replaceImagesWithPlaceholder(bodyBeforeEmbed),
                        simplified = true,
                    )

                embedded.contains(SimpleMarkdownToHtml.LOCAL_IMAGE_PATH_PREFIX) ->
                    PreviewBody(
                        html = SimpleMarkdownToHtml.replaceLocalImagesWithPlaceholder(embedded),
                        simplified = false,
                    )

                else -> PreviewBody(html = embedded, simplified = false)
            }
        }
    } catch (_: TimeoutCancellationException) {
        PreviewBody(
            html = SimpleMarkdownToHtml.replaceImagesWithPlaceholder(bodyBeforeEmbed),
            simplified = true,
        )
    } catch (_: OutOfMemoryError) {
        System.gc()
        PreviewBody(
            html = SimpleMarkdownToHtml.replaceImagesWithPlaceholder(bodyBeforeEmbed),
            simplified = true,
        )
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
    font: NotesContentFont,
    palette: NotesPreviewPalette,
): String {
    val bg = palette.background.toCssHex()
    val fg = palette.foreground.toCssHex()
    val muted = palette.muted.toCssHex()
    val border = palette.border.toCssHex()
    val codeBg = palette.codeBackground.toCssHex()
    val quoteBorder = palette.quoteBorder.toCssHex()
    val link = palette.link.toCssHex()
    val placeholderBg = palette.placeholderBackground.toCssHex()
    val placeholderFg = palette.placeholderForeground.toCssHex()
    val detailsBg = palette.detailsBackground.toCssHex()
    val colorScheme = if (palette.dark) "dark" else "light"
    val fontFaces = NotesContentFontCss.fontFaceRules()
    val fontFamily = font.cssFontFamily
    return """
    <!DOCTYPE html>
    <html>
    <head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <meta name="color-scheme" content="$colorScheme"/>
    <style>
      $fontFaces
      html, body {
        margin: 0;
        padding: 0;
        background: $bg;
        color: $fg;
        height: 100%;
        color-scheme: $colorScheme;
      }
      body {
        padding: 16px;
        font-family: $fontFamily;
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
        border-left: 3px solid $quoteBorder;
        padding-left: 0.8em;
        color: $muted;
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
        border: 1px solid $border;
        padding: 6px 10px;
        vertical-align: top;
      }
      th {
        background: $codeBg;
        font-weight: 600;
      }
      code {
        font-family: ui-monospace, "Cascadia Code", Consolas, monospace;
        font-size: 0.92em;
        background: $codeBg;
        padding: 0.1em 0.35em;
        border-radius: 3px;
      }
      pre {
        background: $codeBg;
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
        background: $bg;
      }
      .img-placeholder {
        background: $placeholderBg;
        color: $placeholderFg;
        padding: 12px 16px;
        margin: 0.6em 0;
        border-radius: 4px;
        font-size: 0.9em;
        text-align: center;
      }
      a { color: $link; }
      hr {
        border: none;
        border-top: 1px solid $border;
        margin: 1.2em 0;
      }
      details {
        background: $detailsBg;
        border: 1px solid $border;
        border-radius: 6px;
        padding: 8px 12px;
      }
      details.frontmatter {
        color: $muted;
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
}

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

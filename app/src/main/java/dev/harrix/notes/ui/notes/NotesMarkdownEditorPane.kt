package dev.harrix.notes.ui.notes

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import dev.harrix.notes.AppPreferences
import dev.harrix.notes.NotesContentFont
import dev.harrix.notes.NotesContentFontCss
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.Locale

private const val BRIDGE_NAME = "NotesEditorHost"
private const val ASSET_BASE_URL = "https://appassets.androidplatform.net/assets/editor/"
private const val FLUSH_TIMEOUT_MS = 2_000L
private const val BOOT_TIMEOUT_MS = 8_000L
private const val SELECTION_ALPHA = 0.3f
private const val LOG_TAG = "NotesEditor"

/** Minimal shell; `editor.js` / fonts are served by [WebViewAssetLoader]. */
private val EDITOR_SHELL_HTML =
    """
    <!DOCTYPE html>
    <html>
      <head>
        <meta charset="utf-8"/>
        <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no"/>
        <style>
          ${NotesContentFontCss.fontFaceRules()}
          html, body { margin:0; padding:0; width:100%; height:100%; overflow:hidden; background:transparent; }
          .cm-editor { height:100%; width:100%; }
        </style>
      </head>
      <body>
        <script>
          window.onerror = function (message, source, line) {
            try {
              if (window.NotesEditorHost) {
                window.NotesEditorHost.onError(String(message) + ' @' + String(source) + ':' + String(line));
              }
            } catch (e) {}
            return false;
          };
        </script>
        <script src="editor.js"></script>
      </body>
    </html>
    """.trimIndent()

/**
 * Owns the WebView that hosts the CodeMirror editor and moves text between it
 * and Compose.
 *
 * The document is never passed as a single string: it is pulled by JavaScript
 * through [textChunk] and pushed back through [appendText]. Numeric bridge
 * arguments travel as strings because older System WebView builds mishandle
 * primitive ints. Boot is started from Kotlin only after the WebView has a
 * non-zero size — creating CodeMirror at 0×0 leaves a permanently blank pane.
 */
class NotesMarkdownEditorController {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val incoming = StringBuilder()
    private val bootTimeout =
        Runnable {
            if (!ready) {
                errorListener("Editor failed to start (timeout)")
            }
        }

    private var webView: WebView? = null
    private var flushSignal: CompletableDeferred<Unit>? = null
    private var pageFinished = false
    private var scriptLoaded = false
    private var bootRequested = false

    @Volatile
    private var stagedText: String = ""

    @Volatile
    private var stagedConfig: String = "{}"

    @Volatile
    private var ready: Boolean = false

    internal var textListener: (String) -> Unit = {}
    internal var readyListener: () -> Unit = {}
    internal var errorListener: (String) -> Unit = {}
    internal var scrollListener: (NotesScrollMetrics) -> Unit = {}

    /**
     * Sends the document to Compose immediately instead of waiting for the
     * editor debounce, and returns once the text has arrived.
     */
    suspend fun flush() {
        val target = webView ?: return
        if (!ready) return
        val signal = CompletableDeferred<Unit>()
        flushSignal = signal
        target.evaluateJavascript("window.notesEditor && window.notesEditor.flush();", null)
        withTimeoutOrNull(FLUSH_TIMEOUT_MS) { signal.await() }
        flushSignal = null
    }

    internal fun attach(target: WebView) {
        webView = target
    }

    internal fun detach(target: WebView) {
        if (webView === target) {
            webView = null
        }
        cancelBootTimeout()
        ready = false
        pageFinished = false
        scriptLoaded = false
        bootRequested = false
        flushSignal?.complete(Unit)
        flushSignal = null
    }

    /** Stages the document and settings picked up by the next editor load. */
    internal fun stage(
        text: String,
        config: String,
    ) {
        stagedText = text
        stagedConfig = config
        ready = false
        pageFinished = false
        scriptLoaded = false
        bootRequested = false
        scheduleBootTimeout()
    }

    internal fun stageText(text: String) {
        stagedText = text
    }

    internal fun applyConfig(config: String) {
        stagedConfig = config
        val target = webView ?: return
        if (!ready) return
        val literal = JSONObject.quote(config)
        target.evaluateJavascript("window.notesEditor && window.notesEditor.applyConfig($literal);", null)
    }

    fun scrollTo(offsetPx: Int) {
        val target = webView ?: return
        if (!ready) return
        target.evaluateJavascript(
            "window.notesEditor && window.notesEditor.scrollTo(${offsetPx.coerceAtLeast(0)});",
            null,
        )
    }

    fun requestScrollReport() {
        val target = webView ?: return
        if (!ready) return
        target.evaluateJavascript("window.notesEditor && window.notesEditor.reportScroll();", null)
    }

    /**
     * Syncs the CodeMirror shell to the WebView height after IME adjustResize
     * and scrolls the caret back into view when it left the visible area.
     */
    fun onViewportHeightChanged(heightCssPx: Float) {
        val target = webView ?: return
        if (!ready || heightCssPx <= 0f) return
        target.evaluateJavascript(
            "window.notesEditor && window.notesEditor.onViewportResize($heightCssPx);",
            null,
        )
    }

    internal fun onPageFinished() {
        pageFinished = true
        tryBoot()
    }

    internal fun syncVisibility(ready: Boolean) {
        webView?.apply {
            // Keep the view laid out while hidden. CodeMirror sees a zero-sized
            // viewport when Android visibility is INVISIBLE.
            visibility = View.VISIBLE
            alpha = if (ready) 1f else 0f
        }
    }

    private fun scheduleBootTimeout() {
        cancelBootTimeout()
        mainHandler.postDelayed(bootTimeout, BOOT_TIMEOUT_MS)
    }

    private fun cancelBootTimeout() {
        mainHandler.removeCallbacks(bootTimeout)
    }

    private fun tryBoot() {
        val target = webView ?: return
        if (ready || bootRequested) {
            return
        }
        if (!pageFinished || !scriptLoaded) {
            return
        }
        target.post {
            if (target.width <= 0 || target.height <= 0) {
                target.post { tryBoot() }
                return@post
            }
            if (bootRequested || ready) {
                return@post
            }
            bootRequested = true
            target.visibility = View.VISIBLE
            target.alpha = 0f
            val viewportHeight = target.height / target.resources.displayMetrics.density
            Log.d(LOG_TAG, "Booting editor ${target.width}x${target.height}")
            target.evaluateJavascript(
                "window.notesEditorBoot && window.notesEditorBoot($viewportHeight);",
                null,
            )
        }
    }

    @JavascriptInterface
    fun configJson(): String = stagedConfig

    @JavascriptInterface
    fun textLength(): String = stagedText.length.toString()

    @JavascriptInterface
    fun textChunk(
        fromStr: String,
        toStr: String,
    ): String {
        val text = stagedText
        val from = fromStr.toIntOrNull() ?: 0
        val to = toStr.toIntOrNull() ?: 0
        val start = from.coerceIn(0, text.length)
        var end = to.coerceIn(start, text.length)
        // Shorten rather than split a surrogate pair: a lone half would not
        // survive the bridge. JavaScript advances by the returned length.
        if (end in (start + 1) until text.length && text[end - 1].isHighSurrogate()) {
            end--
        }
        return text.substring(start, end)
    }

    @JavascriptInterface
    fun beginText(lengthStr: String) {
        incoming.setLength(0)
        val length = lengthStr.toIntOrNull() ?: 0
        if (length > 0) {
            incoming.ensureCapacity(length)
        }
    }

    @JavascriptInterface
    fun appendText(chunk: String) {
        incoming.append(chunk)
    }

    @JavascriptInterface
    fun commitText() {
        val text = incoming.toString()
        incoming.setLength(0)
        mainHandler.post {
            textListener(text)
            flushSignal?.complete(Unit)
        }
    }

    @JavascriptInterface
    fun onScriptLoaded() {
        mainHandler.post {
            Log.d(LOG_TAG, "Editor script loaded")
            scriptLoaded = true
            tryBoot()
        }
    }

    @JavascriptInterface
    fun onReady() {
        ready = true
        mainHandler.post {
            cancelBootTimeout()
            readyListener()
            requestScrollReport()
        }
    }

    @JavascriptInterface
    fun onScroll(
        scrollTopStr: String,
        scrollHeightStr: String,
        clientHeightStr: String,
    ) {
        val metrics =
            NotesScrollMetrics.fromScroller(
                scrollTop = scrollTopStr.toIntOrNull() ?: 0,
                scrollHeight = scrollHeightStr.toIntOrNull() ?: 0,
                clientHeight = clientHeightStr.toIntOrNull() ?: 0,
            )
        mainHandler.post { scrollListener(metrics) }
    }

    @JavascriptInterface
    fun onError(message: String) {
        Log.e(LOG_TAG, "Editor JS error: $message")
        mainHandler.post {
            cancelBootTimeout()
            errorListener(message)
        }
    }
}

/** VS Code (Default Light / Dark Modern) Markdown token colors. */
@Immutable
data class MarkdownTokenColors(
    val heading: Color,
    val quote: Color,
    val listMarker: Color,
    val emphasis: Color,
    val inlineCode: Color,
    val codeBlock: Color,
    val linkText: Color,
    val linkUrl: Color,
    val separator: Color,
    val strikethrough: Color,
) {
    companion object {
        val Light =
            MarkdownTokenColors(
                heading = Color(0xFF3861B2),
                quote = Color(0xFF1CB978),
                listMarker = Color(0xFFD07826),
                emphasis = Color(0xFF1F2939),
                inlineCode = Color(0xFFF13D3D),
                codeBlock = Color(0xFF1CB978),
                linkText = Color(0xFF4C43C2),
                linkUrl = Color(0xFF1CB978),
                separator = Color(0xFF3861B2),
                strikethrough = Color(0xFFAD1C48),
            )

        val Dark =
            MarkdownTokenColors(
                heading = Color(0xFF7BA3E8),
                quote = Color(0xFF3DDB9A),
                listMarker = Color(0xFFE09A50),
                emphasis = Color(0xFFE0E4DB),
                inlineCode = Color(0xFFFF6B6B),
                codeBlock = Color(0xFF3DDB9A),
                linkText = Color(0xFF9B93E8),
                linkUrl = Color(0xFF3DDB9A),
                separator = Color(0xFF7BA3E8),
                strikethrough = Color(0xFFFF8A9A),
            )
    }
}

/** Editor chrome colors plus the Markdown token palette for the current theme. */
@Immutable
data class NotesEditorPalette(
    val dark: Boolean,
    val background: Color,
    val foreground: Color,
    val caret: Color,
    val selection: Color,
    val tokens: MarkdownTokenColors,
)

@Composable
fun rememberNotesEditorPalette(): NotesEditorPalette {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val background = scheme.surface
    val foreground = scheme.onSurface
    val caret = scheme.primary
    val selection = scheme.primary.copy(alpha = SELECTION_ALPHA)
    return remember(dark, background, foreground, caret, selection) {
        NotesEditorPalette(
            dark = dark,
            background = background,
            foreground = foreground,
            caret = caret,
            selection = selection,
            tokens = if (dark) MarkdownTokenColors.Dark else MarkdownTokenColors.Light,
        )
    }
}

/**
 * Note **editor** mode: CodeMirror 6 inside a WebView.
 *
 * Compose cannot lay out a multi-megabyte `TextField` without blocking the main
 * thread, so editing runs in the WebView renderer instead — the same reason
 * [NotesHtmlPreviewPane] opens huge notes quickly.
 *
 * The WebView stays transparent until CodeMirror reports ready. It must remain
 * [View.VISIBLE] during boot so the browser gives CodeMirror a real viewport.
 */
@Composable
fun NotesMarkdownEditorPane(
    isLoading: Boolean,
    docKey: String,
    text: String,
    errorMessage: String?,
    hasContent: Boolean,
    fontSizeSp: Int,
    highlightMaxChars: Int,
    controller: NotesMarkdownEditorController,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    font: NotesContentFont = NotesContentFont.Default,
) {
    val palette = rememberNotesEditorPalette()
    // 0 disables highlighting for every note; larger notes stay plain text.
    val highlight = highlightMaxChars > 0 && text.length <= highlightMaxChars
    val config =
        remember(palette, fontSizeSp, font, highlight, text.length) {
            buildEditorConfig(palette, fontSizeSp, font, highlight, text.length)
        }
    var editorReady by remember { mutableStateOf(false) }
    var editorError by remember { mutableStateOf<String?>(null) }
    var scrollMetrics by remember { mutableStateOf(NotesScrollMetrics()) }
    val latestText by rememberUpdatedState(text)
    val latestOnTextChange by rememberUpdatedState(onTextChange)

    DisposableEffect(controller) {
        controller.textListener = { changed -> latestOnTextChange(changed) }
        controller.readyListener = {
            editorError = null
            editorReady = true
            controller.syncVisibility(true)
        }
        controller.errorListener = { message ->
            editorError = message
            editorReady = false
            scrollMetrics = NotesScrollMetrics()
            controller.syncVisibility(false)
        }
        controller.scrollListener = { metrics ->
            scrollMetrics = metrics
        }
        onDispose {
            controller.textListener = {}
            controller.readyListener = {}
            controller.errorListener = {}
            controller.scrollListener = {}
        }
    }

    // clipToBounds: a WebView can paint outside its Compose bounds while loading.
    Box(modifier = modifier.fillMaxSize().clipToBounds()) {
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    NotesLoadingIndicator()
                }
            }

            errorMessage != null && !hasContent -> {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(24.dp),
                )
            }

            else -> {
                val loaded = remember { EditorLoadState() }
                // Pin the WebView to the Compose slot size — otherwise it measures
                // by document height and overlays the tabs and breadcrumbs.
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { context ->
                            createEditorWebView(context, controller, palette).also { webView ->
                                loaded.docKey = docKey
                                loaded.config = config
                                editorReady = false
                                editorError = null
                                scrollMetrics = NotesScrollMetrics()
                                webView.visibility = View.VISIBLE
                                webView.alpha = 0f
                                controller.stage(latestText, config)
                                loadEditorShell(webView)
                            }
                        },
                        update = { webView ->
                            webView.setBackgroundColor(palette.background.toArgb())
                            webView.visibility = View.VISIBLE
                            webView.alpha = if (editorReady && editorError == null) 1f else 0f
                            controller.stageText(latestText)
                            when {
                                loaded.docKey != docKey -> {
                                    loaded.docKey = docKey
                                    loaded.config = config
                                    editorReady = false
                                    editorError = null
                                    scrollMetrics = NotesScrollMetrics()
                                    webView.alpha = 0f
                                    controller.stage(latestText, config)
                                    loadEditorShell(webView)
                                }

                                loaded.config != config -> {
                                    loaded.config = config
                                    controller.applyConfig(config)
                                }
                            }
                        },
                        onRelease = { webView ->
                            controller.detach(webView)
                            webView.destroy()
                        },
                        modifier =
                        Modifier
                            .width(maxWidth)
                            .height(maxHeight),
                    )
                    if (editorReady && editorError == null && scrollMetrics.canScroll) {
                        NotesFingerScrollbar(
                            scrollOffset = scrollMetrics.scrollOffset,
                            maxScrollOffset = scrollMetrics.maxScrollOffset,
                            viewportSize = scrollMetrics.viewportSize,
                            contentSize = scrollMetrics.contentSize,
                            onScrollOffsetChange = { offset ->
                                controller.scrollTo(offset.toScrollPxInt())
                            },
                            modifier =
                            Modifier
                                .align(Alignment.CenterEnd)
                                .padding(vertical = 8.dp, horizontal = 2.dp),
                        )
                    }
                }
                when {
                    editorError != null -> {
                        Box(
                            modifier =
                            Modifier
                                .fillMaxSize()
                                .background(palette.background)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = editorError.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    !editorReady -> {
                        Box(
                            modifier =
                            Modifier
                                .fillMaxSize()
                                .background(palette.background),
                            contentAlignment = Alignment.Center,
                        ) {
                            NotesLoadingIndicator()
                        }
                    }
                }
            }
        }
    }
}

/** Tracks what the WebView currently shows, without triggering recomposition. */
private class EditorLoadState {
    var docKey: String? = null
    var config: String? = null
}

private fun loadEditorShell(webView: WebView) {
    // Prefer loadDataWithBaseURL so the HTML shell never depends on intercepting
    // the document request itself; only editor.js is fetched via the asset loader.
    webView.loadDataWithBaseURL(
        ASSET_BASE_URL,
        EDITOR_SHELL_HTML,
        "text/html",
        Charsets.UTF_8.name(),
        null,
    )
}

@SuppressLint("SetJavaScriptEnabled")
private fun createEditorWebView(
    context: Context,
    controller: NotesMarkdownEditorController,
    palette: NotesEditorPalette,
): WebView {
    val assetLoader =
        WebViewAssetLoader
            .Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
    return WebView(context).apply {
        // Scripts are required: the editor itself is the bundled CodeMirror asset.
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = false
        // Font size comes from app settings, so ignore the system text scale.
        settings.textZoom = 100
        isFocusableInTouchMode = true
        isVerticalScrollBarEnabled = false
        visibility = View.VISIBLE
        alpha = 0f
        setBackgroundColor(palette.background.toArgb())
        webViewClient =
            object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ) = assetLoader.shouldInterceptRequest(request.url)

                override fun onPageFinished(
                    view: WebView,
                    url: String?,
                ) {
                    Log.d(LOG_TAG, "Page finished: $url")
                    controller.onPageFinished()
                }
            }
        webChromeClient =
            object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    val level =
                        when (consoleMessage.messageLevel()) {
                            ConsoleMessage.MessageLevel.ERROR -> Log.ERROR
                            ConsoleMessage.MessageLevel.WARNING -> Log.WARN
                            else -> Log.DEBUG
                        }
                    Log.println(
                        level,
                        LOG_TAG,
                        "${consoleMessage.sourceId()}:${consoleMessage.lineNumber()} ${consoleMessage.message()}",
                    )
                    return true
                }
            }
        addJavascriptInterface(controller, BRIDGE_NAME)
        controller.attach(this)
        addOnLayoutChangeListener { view, _, top, _, bottom, _, oldTop, _, oldBottom ->
            val heightPx = bottom - top
            val oldHeightPx = oldBottom - oldTop
            if (heightPx <= 0 || heightPx == oldHeightPx) {
                return@addOnLayoutChangeListener
            }
            val heightCssPx = heightPx / view.resources.displayMetrics.density
            controller.onViewportHeightChanged(heightCssPx)
        }
    }
}

private fun buildEditorConfig(
    palette: NotesEditorPalette,
    fontSizeSp: Int,
    font: NotesContentFont,
    highlight: Boolean,
    expectedLength: Int,
): String {
    val tokens = palette.tokens
    val tokenJson =
        JSONObject()
            .put("heading", tokens.heading.toCssHex())
            .put("quote", tokens.quote.toCssHex())
            .put("listMarker", tokens.listMarker.toCssHex())
            .put("emphasis", tokens.emphasis.toCssHex())
            .put("inlineCode", tokens.inlineCode.toCssHex())
            .put("codeBlock", tokens.codeBlock.toCssHex())
            .put("linkText", tokens.linkText.toCssHex())
            .put("linkUrl", tokens.linkUrl.toCssHex())
            .put("separator", tokens.separator.toCssHex())
            .put("strikethrough", tokens.strikethrough.toCssHex())
    return JSONObject()
        .put(
            "fontSize",
            fontSizeSp.coerceIn(AppPreferences.MIN_FONT_SIZE_SP, AppPreferences.MAX_FONT_SIZE_SP),
        ).put("fontFamily", font.cssFontFamily)
        .put("dark", palette.dark)
        .put("highlight", highlight)
        .put("expectedLength", expectedLength)
        .put("background", palette.background.toCssHex())
        .put("foreground", palette.foreground.toCssHex())
        .put("caret", palette.caret.toCssHex())
        .put("selection", palette.selection.toCssRgba())
        .put("tokens", tokenJson)
        .toString()
}

internal fun Color.toCssHex(): String {
    val argb = toArgb()
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return "#%02X%02X%02X".format(r, g, b)
}

private fun Color.toCssRgba(): String {
    val argb = toArgb()
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    val a = ((argb shr 24) and 0xFF) / 255f
    // Locale.ROOT: CSS needs a dot as the decimal separator.
    return String.format(Locale.ROOT, "rgba(%d, %d, %d, %.2f)", r, g, b, a)
}

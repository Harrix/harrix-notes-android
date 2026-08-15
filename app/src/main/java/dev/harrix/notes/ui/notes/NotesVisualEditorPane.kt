package dev.harrix.notes.ui.notes

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import dev.harrix.notes.NoteInsertedImage
import dev.harrix.notes.NotesPathSegment
import dev.harrix.notes.NotesRelativeDocuments
import dev.harrix.notes.NotesTreeRepository
import dev.harrix.notes.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

private const val BRIDGE_NAME = "NotesVisualEditorHost"
private const val ASSET_BASE_URL = "https://appassets.androidplatform.net/assets/visual-editor/"
private const val FLUSH_TIMEOUT_MS = 2_000L
private const val LOG_TAG = "NotesVisualEditor"

private val MD_IMAGE_REGEX = Regex("""!\[[^\]]*]\(\s*<?([^>\s)]+)>?""")
private val HTML_IMAGE_REGEX =
    Regex("""<img\b[^>]*\bsrc\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)

/**
 * Owns the WebView for the visual Markdown editor (`@hsk-sync:visual-markdown`).
 */
class NotesVisualEditorController {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var flushSignal: CompletableDeferred<Unit>? = null

    @Volatile
    var ready: Boolean = false
        private set

    fun markNotReady() {
        ready = false
    }

    var textListener: (String) -> Unit = {}
    var readyListener: () -> Unit = {}
    var promptLinkListener: () -> Unit = {}
    var pickImagesListener: () -> Unit = {}
    var dropFilesListener: (List<VisualDroppedFile>) -> Unit = {}

    suspend fun flush() {
        val target = webView ?: return
        if (!ready) {
            return
        }
        val signal = CompletableDeferred<Unit>()
        flushSignal = signal
        target.evaluateJavascript("window.notesVisualEditor && window.notesVisualEditor.flush();", null)
        withTimeoutOrNull(FLUSH_TIMEOUT_MS) { signal.await() }
        flushSignal = null
    }

    fun attach(target: WebView) {
        webView = target
    }

    fun detach(target: WebView) {
        if (webView === target) {
            webView = null
        }
        ready = false
        flushSignal?.complete(Unit)
        flushSignal = null
    }

    fun setDocument(
        text: String,
        imageUrisJson: String,
    ) {
        val target = webView ?: return
        if (!ready) {
            return
        }
        val textLiteral = JSONObject.quote(text)
        val urisLiteral = JSONObject.quote(imageUrisJson)
        target.evaluateJavascript(
            "window.notesVisualEditor && window.notesVisualEditor.setDocument($textLiteral, $urisLiteral);",
            null,
        )
    }

    fun insertLink(url: String) {
        val target = webView ?: return
        if (!ready) {
            return
        }
        val literal = JSONObject.quote(url)
        target.evaluateJavascript(
            "window.notesVisualEditor && window.notesVisualEditor.insertLink($literal);",
            null,
        )
    }

    fun insertHtml(html: String) {
        val target = webView ?: return
        if (!ready) {
            return
        }
        val literal = JSONObject.quote(html)
        target.evaluateJavascript(
            "window.notesVisualEditor && window.notesVisualEditor.insertHtml($literal);",
            null,
        )
    }

    fun applyTheme(varsJson: String) {
        val target = webView ?: return
        if (!ready) {
            return
        }
        val literal = JSONObject.quote(varsJson)
        target.evaluateJavascript(
            "window.notesVisualEditor && window.notesVisualEditor.applyTheme($literal);",
            null,
        )
    }

    @JavascriptInterface
    fun onReady() {
        ready = true
        mainHandler.post { readyListener() }
    }

    @JavascriptInterface
    fun onEdit(text: String) {
        mainHandler.post { textListener(text) }
    }

    @JavascriptInterface
    fun onFlushed() {
        mainHandler.post { flushSignal?.complete(Unit) }
    }

    @JavascriptInterface
    fun onPromptLink() {
        mainHandler.post { promptLinkListener() }
    }

    @JavascriptInterface
    fun onPickImages() {
        mainHandler.post { pickImagesListener() }
    }

    @JavascriptInterface
    fun onDropFiles(json: String) {
        val files = parseDroppedFiles(json)
        mainHandler.post { dropFilesListener(files) }
    }

    @JavascriptInterface
    fun onError(message: String) {
        Log.e(LOG_TAG, message)
    }
}

class VisualDroppedFile(
    val name: String,
    val mime: String,
    val bytes: ByteArray,
)

private fun parseDroppedFiles(json: String): List<VisualDroppedFile> {
    val array = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
    return (0 until array.length()).mapNotNull { index ->
        val obj = array.optJSONObject(index) ?: return@mapNotNull null
        val raw = obj.optString("base64")
        if (raw.isBlank()) {
            return@mapNotNull null
        }
        val bytes = runCatching { Base64.decode(raw, Base64.DEFAULT) }.getOrNull()
            ?: return@mapNotNull null
        VisualDroppedFile(
            name = obj.optString("name").ifBlank { "image.png" },
            mime = obj.optString("mime").ifBlank { "image/png" },
            bytes = bytes,
        )
    }
}

/**
 * Visual Markdown editor (`@hsk-sync:visual-markdown`) — same commands as the VS Code custom editor.
 */
@Composable
fun NotesVisualEditorPane(
    isLoading: Boolean,
    docKey: String,
    text: String,
    errorMessage: String?,
    hasContent: Boolean,
    treeUri: Uri?,
    folderPath: List<NotesPathSegment>,
    noteDocumentId: String,
    repository: NotesTreeRepository,
    controller: NotesVisualEditorController,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    findController: NotesPreviewFindController? = null,
) {
    val context = LocalContext.current
    val resolver = remember(context) { context.applicationContext.contentResolver }
    val scope = rememberCoroutineScope()
    val palette = rememberNotesEditorPalette()
    val themeJson = remember(palette) { visualEditorThemeJson(palette) }
    var editorReady by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkUrl by remember { mutableStateOf("") }
    val latestText by rememberUpdatedState(text)
    val latestOnTextChange by rememberUpdatedState(onTextChange)
    val latestTreeUri by rememberUpdatedState(treeUri)
    val latestFolderPath by rememberUpdatedState(folderPath)
    val latestNoteDocumentId by rememberUpdatedState(noteDocumentId)

    val imagePicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments(),
        ) { uris ->
            if (uris.isEmpty()) {
                return@rememberLauncherForActivityResult
            }
            scope.launch {
                insertPickedImages(
                    resolver = resolver,
                    repository = repository,
                    treeUri = latestTreeUri,
                    folderPath = latestFolderPath,
                    noteDocumentId = latestNoteDocumentId,
                    uris = uris,
                    controller = controller,
                )
            }
        }

    DisposableEffect(controller) {
        controller.textListener = { changed -> latestOnTextChange(changed) }
        controller.readyListener = { editorReady = true }
        controller.promptLinkListener = {
            linkUrl = ""
            showLinkDialog = true
        }
        controller.pickImagesListener = {
            imagePicker.launch(arrayOf("image/*"))
        }
        controller.dropFilesListener = { files ->
            scope.launch {
                insertDroppedImages(
                    repository = repository,
                    treeUri = latestTreeUri,
                    folderPath = latestFolderPath,
                    noteDocumentId = latestNoteDocumentId,
                    files = files,
                    controller = controller,
                )
            }
        }
        onDispose {
            controller.textListener = {}
            controller.readyListener = {}
            controller.promptLinkListener = {}
            controller.pickImagesListener = {}
            controller.dropFilesListener = {}
        }
    }

    LaunchedEffect(editorReady, docKey, treeUri, folderPath, noteDocumentId) {
        if (!editorReady) {
            return@LaunchedEffect
        }
        controller.applyTheme(themeJson)
        val imageUris =
            withContext(Dispatchers.IO) {
                collectImageDataUris(
                    resolver = resolver,
                    treeUri = treeUri,
                    folderPath = folderPath,
                    noteDocumentId = noteDocumentId,
                    markdown = latestText,
                )
            }
        controller.setDocument(latestText, imageUris)
    }

    LaunchedEffect(editorReady, themeJson) {
        if (editorReady) {
            controller.applyTheme(themeJson)
        }
    }

    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text(stringResource(R.string.markdown_notes_visual_link_title)) },
            text = {
                OutlinedTextField(
                    value = linkUrl,
                    onValueChange = { linkUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.markdown_notes_visual_link_hint)) },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val url = linkUrl.trim()
                        showLinkDialog = false
                        if (url.isNotEmpty()) {
                            controller.insertLink(url)
                        }
                    },
                ) {
                    Text(stringResource(R.string.markdown_notes_visual_link_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) {
                    Text(stringResource(R.string.markdown_notes_visual_link_cancel))
                }
            },
        )
    }

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
                val loaded = remember { VisualEditorLoadState() }
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            createVisualEditorWebView(ctx, controller, palette, findController).also {
                                loaded.docKey = docKey
                                editorReady = false
                            }
                        },
                        update = { webView ->
                            webView.setBackgroundColor(palette.background.toArgb())
                            if (loaded.docKey != docKey) {
                                loaded.docKey = docKey
                                editorReady = false
                                controller.markNotReady()
                                webView.loadUrl(ASSET_BASE_URL + "visual-editor.html")
                            }
                        },
                        onRelease = { webView ->
                            findController?.detach(webView)
                            controller.detach(webView)
                            webView.destroy()
                        },
                        modifier =
                        Modifier
                            .width(maxWidth)
                            .height(maxHeight),
                    )
                    if (!editorReady) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
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

private class VisualEditorLoadState {
    var docKey: String? = null
}

@SuppressLint("SetJavaScriptEnabled")
private fun createVisualEditorWebView(
    context: Context,
    controller: NotesVisualEditorController,
    palette: NotesEditorPalette,
    findController: NotesPreviewFindController?,
): WebView {
    val assetLoader =
        WebViewAssetLoader
            .Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
    return WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = false
        settings.textZoom = 100
        isFocusableInTouchMode = true
        setBackgroundColor(palette.background.toArgb())
        webViewClient =
            object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ) = assetLoader.shouldInterceptRequest(request.url)
            }
        addJavascriptInterface(controller, BRIDGE_NAME)
        controller.attach(this)
        findController?.attach(this)
        loadUrl(ASSET_BASE_URL + "visual-editor.html")
    }
}

private fun visualEditorThemeJson(palette: NotesEditorPalette): String {
    val dark = palette.dark
    val obj = JSONObject()
    obj.put("--ve-fg", colorCss(palette.foreground.toArgb()))
    obj.put("--ve-bg", colorCss(palette.background.toArgb()))
    obj.put("--ve-muted", if (dark) "#9aa0a6" else "#5f6368")
    obj.put("--ve-border", if (dark) "rgba(255,255,255,0.18)" else "rgba(128,128,128,0.35)")
    obj.put("--ve-btn-bg", if (dark) "#3c4043" else "#e8eaed")
    obj.put("--ve-btn-fg", colorCss(palette.foreground.toArgb()))
    obj.put("--ve-btn-hover", if (dark) "#5f6368" else "#dadce0")
    obj.put("--ve-focus", colorCss(palette.caret.toArgb()))
    obj.put("--ve-input-bg", colorCss(palette.background.toArgb()))
    obj.put("--ve-input-fg", colorCss(palette.foreground.toArgb()))
    obj.put("--ve-link", colorCss(palette.tokens.linkText.toArgb()))
    obj.put("--ve-code-bg", if (dark) "rgba(255,255,255,0.08)" else "rgba(127,127,127,0.12)")
    return obj.toString()
}

private fun colorCss(argb: Int): String = String.format(Locale.US, "#%06X", 0xFFFFFF and argb)

private fun collectImageDataUris(
    resolver: ContentResolver,
    treeUri: Uri?,
    folderPath: List<NotesPathSegment>,
    noteDocumentId: String,
    markdown: String,
): String {
    if (treeUri == null) {
        return "{}"
    }
    val paths = linkedSetOf<String>()
    MD_IMAGE_REGEX.findAll(markdown).forEach { paths.add(it.groupValues[1].trim()) }
    HTML_IMAGE_REGEX.findAll(markdown).forEach { paths.add(it.groupValues[1].trim()) }
    val obj = JSONObject()
    paths
        .filter { path ->
            path.isNotEmpty() &&
                !path.startsWith("http", ignoreCase = true) &&
                !path.startsWith("data:")
        }.forEach { path ->
            val loaded =
                loadLocalImageBytes(resolver, treeUri, folderPath, noteDocumentId, path)
                    ?: return@forEach
            val (mime, bytes) = loaded
            obj.put(
                path,
                "data:$mime;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}",
            )
        }
    return obj.toString()
}

private fun loadLocalImageBytes(
    resolver: ContentResolver,
    treeUri: Uri,
    folderPath: List<NotesPathSegment>,
    noteDocumentId: String,
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
        else -> "image/*"
    }
}

private suspend fun insertPickedImages(
    resolver: ContentResolver,
    repository: NotesTreeRepository,
    treeUri: Uri?,
    folderPath: List<NotesPathSegment>,
    noteDocumentId: String,
    uris: List<Uri>,
    controller: NotesVisualEditorController,
) {
    if (treeUri == null) {
        return
    }
    val inserted =
        withContext(Dispatchers.IO) {
            uris.mapNotNull { uri ->
                val bytes = NotesRelativeDocuments.readBytes(resolver, uri) ?: return@mapNotNull null
                val name = queryDisplayName(resolver, uri) ?: "image.png"
                val mime = resolver.getType(uri) ?: guessImageMime(name)
                runCatching {
                    repository.insertImageIntoNoteFolder(
                        treeUri = treeUri,
                        folderPath = folderPath,
                        noteDocumentId = noteDocumentId,
                        fileName = name,
                        bytes = bytes,
                        mime = mime,
                    )
                }.getOrNull()
            }
        }
    insertImagesHtml(inserted, controller)
}

private suspend fun insertDroppedImages(
    repository: NotesTreeRepository,
    treeUri: Uri?,
    folderPath: List<NotesPathSegment>,
    noteDocumentId: String,
    files: List<VisualDroppedFile>,
    controller: NotesVisualEditorController,
) {
    if (treeUri == null) {
        return
    }
    val inserted =
        withContext(Dispatchers.IO) {
            files.mapNotNull { file ->
                runCatching {
                    repository.insertImageIntoNoteFolder(
                        treeUri = treeUri,
                        folderPath = folderPath,
                        noteDocumentId = noteDocumentId,
                        fileName = file.name,
                        bytes = file.bytes,
                        mime = file.mime,
                    )
                }.getOrNull()
            }
        }
    insertImagesHtml(inserted, controller)
}

private fun insertImagesHtml(
    inserted: List<NoteInsertedImage>,
    controller: NotesVisualEditorController,
) {
    if (inserted.isEmpty()) {
        return
    }
    val html =
        inserted.joinToString("") { image ->
            val src = escapeHtmlAttr(image.dataUri)
            val md = escapeHtmlAttr(image.relativePath)
            """<img src="$src" data-md-src="$md" alt="" />"""
        }
    controller.insertHtml(html)
}

private fun queryDisplayName(
    resolver: ContentResolver,
    uri: Uri,
): String? {
    val cursor =
        runCatching {
            resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
        }.getOrNull() ?: return null
    return cursor.use {
        if (!it.moveToFirst()) {
            return@use null
        }
        val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (index < 0) {
            null
        } else {
            it.getString(index)
        }
    }
}

private fun escapeHtmlAttr(value: String): String = value
    .replace("&", "&amp;")
    .replace("\"", "&quot;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")

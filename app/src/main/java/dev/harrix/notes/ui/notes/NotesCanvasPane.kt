package dev.harrix.notes.ui.notes

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixOff
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.harrix.notes.CanvasPageRef
import dev.harrix.notes.CanvasPages
import dev.harrix.notes.CanvasPaperMode
import dev.harrix.notes.NotesPathSegment
import dev.harrix.notes.NotesRelativeDocuments
import dev.harrix.notes.NotesViewerPreferences
import dev.harrix.notes.R
import dev.harrix.notes.ui.theme.DarkColorScheme
import dev.harrix.notes.ui.theme.LightColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.min
import kotlin.math.roundToInt
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Brush as ComposeBrush

private const val CanvasAutosaveDelayMs = 800L

/** Brand drawing swatches (see project color guide). */
private val PenColors =
    listOf(
        AndroidColor.parseColor("#121e28"), // almost black
        AndroidColor.parseColor("#ffffff"), // white
        AndroidColor.parseColor("#de2b26"), // logo / red
        AndroidColor.parseColor("#cc584c"), // red
        AndroidColor.parseColor("#2e86b7"), // blue
        AndroidColor.parseColor("#79b1d1"), // cyan
        AndroidColor.parseColor("#038387"), // turquoise
        AndroidColor.parseColor("#4caf50"), // green
        AndroidColor.parseColor("#35965f"), // green № 2
        AndroidColor.parseColor("#ffa000"), // orange
        AndroidColor.parseColor("#eec646"), // yellow
    )

private data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float,
)

private data class CanvasStroke(
    val points: List<StrokePoint>,
    val color: Int,
    val baseWidth: Float,
    val isEraser: Boolean,
)

private enum class CanvasTool {
    Pen,
    Eraser,
}

@Composable
private fun rememberCanvasPaperColor(mode: CanvasPaperMode): Color {
    val themeSurface = MaterialTheme.colorScheme.surface
    return when (mode) {
        CanvasPaperMode.Light -> LightColorScheme.surface
        CanvasPaperMode.Dark -> DarkColorScheme.surface
        CanvasPaperMode.FollowTheme -> themeSurface
    }
}

private class CanvasSession {
    var imageUri: Uri? = null
    var sourceBitmap: Bitmap? = null
    var displayBitmap: Bitmap? = null
    val strokes = mutableListOf<CanvasStroke>()
    val redoStack = mutableListOf<CanvasStroke>()
    var dirty: Boolean = false
}

/**
 * Full-screen drawing surface for notes with YAML `type: canvas`.
 * Pages are `img/canvas_01.png`, `img/canvas_02.png`, … (legacy `canvas.png` supported).
 */
@Composable
fun NotesCanvasPane(
    isLoading: Boolean,
    treeUri: Uri?,
    folderPath: List<NotesPathSegment>,
    noteDocumentId: String,
    noteUri: Uri,
    noteMarkdown: String,
    contentResolver: ContentResolver,
    preferences: NotesViewerPreferences,
    modifier: Modifier = Modifier,
    onStatusMessage: (String?) -> Unit = {},
    onNoteMarkdownChange: (String) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val session = remember { CanvasSession() }
    var paperMode by remember { mutableStateOf(preferences.loadCanvasPaperMode()) }
    val paperColor = rememberCanvasPaperColor(paperMode)
    val fallbackPenColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val initialPenColor = preferences.loadCanvasPenColorArgb() ?: fallbackPenColor
    val initialPenWidth = preferences.loadCanvasPenWidth()
    var loadError by remember { mutableStateOf<String?>(null) }
    var hasImage by remember { mutableStateOf(false) }
    var pages by remember { mutableStateOf<List<CanvasPageRef>>(emptyList()) }
    var pageIndex by remember { mutableIntStateOf(0) }
    var showDeletePageDialog by remember { mutableStateOf(false) }
    var showClearPageDialog by remember { mutableStateOf(false) }
    var tool by remember { mutableStateOf(CanvasTool.Pen) }
    var drawingEnabled by remember { mutableStateOf(true) }
    var penColor by remember { mutableIntStateOf(initialPenColor) }
    var baseWidth by remember { mutableFloatStateOf(initialPenWidth) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var currentStroke by remember { mutableStateOf<CanvasStroke?>(null) }
    var canUndo by remember { mutableStateOf(false) }
    var canRedo by remember { mutableStateOf(false) }
    var displayRevision by remember { mutableIntStateOf(0) }
    var autosaveJob by remember { mutableStateOf<Job?>(null) }
    val loadFailedMessage = stringResource(R.string.markdown_notes_canvas_load_failed)
    val saveFailedMessage = stringResource(R.string.markdown_notes_canvas_save_failed)
    val pageFailedMessage = stringResource(R.string.markdown_notes_canvas_page_failed)
    val deleteLastMessage = stringResource(R.string.markdown_notes_canvas_page_delete_last)
    val latestScale by rememberUpdatedState(scale)
    val latestOffset by rememberUpdatedState(offset)
    val latestTool by rememberUpdatedState(tool)
    val latestDrawingEnabled by rememberUpdatedState(drawingEnabled)
    val latestPenColor by rememberUpdatedState(penColor)
    val latestBaseWidth by rememberUpdatedState(baseWidth)
    val latestOnStatusMessage by rememberUpdatedState(onStatusMessage)
    val latestNoteMarkdown by rememberUpdatedState(noteMarkdown)
    val latestOnNoteMarkdownChange by rememberUpdatedState(onNoteMarkdownChange)

    fun syncUndoFlags() {
        canUndo = session.strokes.isNotEmpty()
        canRedo = session.redoStack.isNotEmpty()
    }

    fun rebuildDisplay(active: CanvasStroke? = null) {
        val source = session.sourceBitmap ?: return
        session.displayBitmap?.recycle()
        val display = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = AndroidCanvas(display)
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
        for (stroke in session.strokes) {
            drawStrokeOnAndroid(canvas, stroke, paint)
        }
        if (active != null) {
            drawStrokeOnAndroid(canvas, active, paint)
        }
        session.displayBitmap = display
        displayRevision += 1
    }

    suspend fun flushCurrentPage(): Boolean {
        autosaveJob?.cancel()
        autosaveJob = null
        if (!session.dirty) {
            return true
        }
        val uri = session.imageUri ?: return false
        val display = session.displayBitmap ?: return false
        val ok =
            withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = pngBytes(display)
                    contentResolver.openOutputStream(uri, "w")?.use { output ->
                        output.write(bytes)
                        output.flush()
                    } ?: error("no stream")
                }.isSuccess
            }
        if (ok) {
            session.dirty = false
        }
        return ok
    }

    fun scheduleAutosave() {
        session.dirty = true
        autosaveJob?.cancel()
        autosaveJob =
            scope.launch {
                delay(CanvasAutosaveDelayMs)
                val ok = flushCurrentPage()
                if (ok) {
                    latestOnStatusMessage(null)
                } else {
                    latestOnStatusMessage(saveFailedMessage)
                }
            }
    }

    fun clearSessionBitmaps() {
        session.sourceBitmap?.recycle()
        session.displayBitmap?.recycle()
        session.sourceBitmap = null
        session.displayBitmap = null
        session.imageUri = null
        session.strokes.clear()
        session.redoStack.clear()
        session.dirty = false
        currentStroke = null
        hasImage = false
        syncUndoFlags()
    }

    suspend fun persistMarkdownForPages(updatedPages: List<CanvasPageRef>): Boolean {
        val markdown = CanvasPages.syncMarkdownImageLinks(latestNoteMarkdown, updatedPages)
        val ok =
            withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openOutputStream(noteUri, "wt")?.use { output ->
                        output.write(markdown.toByteArray(Charsets.UTF_8))
                        output.flush()
                    } ?: error("no stream")
                }.isSuccess
            }
        if (ok) {
            latestOnNoteMarkdownChange(markdown)
        }
        return ok
    }

    LaunchedEffect(treeUri, folderPath, noteDocumentId, isLoading) {
        if (isLoading || treeUri == null) {
            return@LaunchedEffect
        }
        loadError = null
        latestOnStatusMessage(null)
        clearSessionBitmaps()
        val listed =
            withContext(Dispatchers.IO) {
                CanvasPages.listPages(
                    resolver = contentResolver,
                    treeUri = treeUri,
                    folderPath = folderPath,
                    noteDocumentId = noteDocumentId,
                )
            }
        pages = listed
        pageIndex = 0
        if (listed.isEmpty()) {
            loadError = loadFailedMessage
            latestOnStatusMessage(loadFailedMessage)
        }
    }

    LaunchedEffect(treeUri, pages, pageIndex, isLoading) {
        if (isLoading || treeUri == null || pages.isEmpty()) {
            return@LaunchedEffect
        }
        val safeIndex = pageIndex.coerceIn(0, pages.lastIndex)
        if (safeIndex != pageIndex) {
            pageIndex = safeIndex
            return@LaunchedEffect
        }
        val page = pages[safeIndex]
        loadError = null
        val loaded =
            withContext(Dispatchers.IO) {
                val bytes = NotesRelativeDocuments.readBytes(contentResolver, page.uri)
                val decoded =
                    bytes
                        ?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                        ?.copy(Bitmap.Config.ARGB_8888, true)
                if (decoded == null) null else page.uri to decoded
            }
        session.sourceBitmap?.recycle()
        session.displayBitmap?.recycle()
        session.sourceBitmap = null
        session.displayBitmap = null
        session.strokes.clear()
        session.redoStack.clear()
        session.dirty = false
        currentStroke = null
        if (loaded == null) {
            loadError = loadFailedMessage
            latestOnStatusMessage(loadFailedMessage)
            session.imageUri = null
            hasImage = false
            syncUndoFlags()
            return@LaunchedEffect
        }
        session.imageUri = loaded.first
        session.sourceBitmap = loaded.second
        rebuildDisplay()
        hasImage = true
        scale = 1f
        offset = Offset.Zero
        syncUndoFlags()
    }

    DisposableEffect(Unit) {
        onDispose {
            autosaveJob?.cancel()
            if (session.dirty) {
                runCatching {
                    val uri = session.imageUri ?: return@runCatching
                    val display = session.displayBitmap ?: return@runCatching
                    val bytes = pngBytes(display)
                    contentResolver.openOutputStream(uri, "w")?.use { output ->
                        output.write(bytes)
                        output.flush()
                    }
                }
            }
            session.sourceBitmap?.recycle()
            session.displayBitmap?.recycle()
            session.sourceBitmap = null
            session.displayBitmap = null
        }
    }

    fun goToPage(targetIndex: Int) {
        if (targetIndex == pageIndex || targetIndex !in pages.indices) {
            return
        }
        scope.launch {
            if (!flushCurrentPage()) {
                latestOnStatusMessage(saveFailedMessage)
                return@launch
            }
            clearSessionBitmaps()
            pageIndex = targetIndex
        }
    }

    fun addPage() {
        val tree = treeUri ?: return
        scope.launch {
            if (!flushCurrentPage()) {
                latestOnStatusMessage(saveFailedMessage)
                return@launch
            }
            val updated =
                withContext(Dispatchers.IO) {
                    runCatching {
                        CanvasPages.addPage(
                            resolver = contentResolver,
                            treeUri = tree,
                            folderPath = folderPath,
                            noteDocumentId = noteDocumentId,
                        )
                    }.getOrNull()
                }
            if (updated == null || updated.isEmpty()) {
                latestOnStatusMessage(pageFailedMessage)
                return@launch
            }
            if (!persistMarkdownForPages(updated)) {
                latestOnStatusMessage(pageFailedMessage)
                return@launch
            }
            clearSessionBitmaps()
            pages = updated
            pageIndex = updated.lastIndex
            latestOnStatusMessage(null)
        }
    }

    fun confirmDeletePage() {
        if (pages.size <= 1) {
            latestOnStatusMessage(deleteLastMessage)
            showDeletePageDialog = false
            return
        }
        val tree = treeUri ?: return
        val page = pages.getOrNull(pageIndex) ?: return
        scope.launch {
            if (!flushCurrentPage()) {
                latestOnStatusMessage(saveFailedMessage)
                showDeletePageDialog = false
                return@launch
            }
            val deleted =
                withContext(Dispatchers.IO) {
                    CanvasPages.deletePage(contentResolver, page)
                }
            if (!deleted) {
                latestOnStatusMessage(pageFailedMessage)
                showDeletePageDialog = false
                return@launch
            }
            val updated =
                withContext(Dispatchers.IO) {
                    CanvasPages.listPages(
                        resolver = contentResolver,
                        treeUri = tree,
                        folderPath = folderPath,
                        noteDocumentId = noteDocumentId,
                    )
                }
            if (updated.isEmpty() || !persistMarkdownForPages(updated)) {
                latestOnStatusMessage(pageFailedMessage)
                showDeletePageDialog = false
                return@launch
            }
            clearSessionBitmaps()
            pages = updated
            pageIndex = pageIndex.coerceAtMost(updated.lastIndex)
            showDeletePageDialog = false
            latestOnStatusMessage(null)
        }
    }

    fun clearCurrentPage() {
        val width = session.sourceBitmap?.width ?: return
        val height = session.sourceBitmap?.height ?: return
        currentStroke = null
        session.strokes.clear()
        session.redoStack.clear()
        session.sourceBitmap?.recycle()
        session.displayBitmap?.recycle()
        val blank = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        blank.eraseColor(AndroidColor.TRANSPARENT)
        session.sourceBitmap = blank
        session.displayBitmap = null
        rebuildDisplay()
        syncUndoFlags()
        scheduleAutosave()
        showClearPageDialog = false
    }

    if (showDeletePageDialog) {
        AlertDialog(
            onDismissRequest = { showDeletePageDialog = false },
            title = { Text(stringResource(R.string.markdown_notes_canvas_page_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.markdown_notes_canvas_page_delete_message,
                        pageIndex + 1,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmDeletePage() }) {
                    Text(stringResource(R.string.markdown_notes_canvas_page_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePageDialog = false }) {
                    Text(stringResource(R.string.markdown_notes_canvas_page_delete_cancel))
                }
            },
        )
    }

    if (showClearPageDialog) {
        AlertDialog(
            onDismissRequest = { showClearPageDialog = false },
            title = { Text(stringResource(R.string.markdown_notes_canvas_clear_title)) },
            text = { Text(stringResource(R.string.markdown_notes_canvas_clear_message)) },
            confirmButton = {
                TextButton(onClick = { clearCurrentPage() }) {
                    Text(stringResource(R.string.markdown_notes_canvas_clear_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearPageDialog = false }) {
                    Text(stringResource(R.string.markdown_notes_canvas_clear_cancel))
                }
            },
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        CanvasToolbar(
            tool = tool,
            drawingEnabled = drawingEnabled,
            penColor = penColor,
            paperMode = paperMode,
            baseWidth = baseWidth,
            canUndo = canUndo,
            canRedo = canRedo,
            canClear = hasImage,
            onToolChange = { next ->
                tool = next
                drawingEnabled = true
            },
            onDrawingEnabledChange = { drawingEnabled = it },
            onColorChange = { color ->
                penColor = color
                drawingEnabled = true
                preferences.saveCanvasPenColorArgb(color)
            },
            onPaperModeChange = { mode ->
                paperMode = mode
                preferences.saveCanvasPaperMode(mode)
            },
            onWidthChange = { width ->
                baseWidth = width
                preferences.saveCanvasPenWidth(width)
            },
            onUndo = {
                if (session.strokes.isEmpty()) {
                    return@CanvasToolbar
                }
                session.redoStack += session.strokes.removeAt(session.strokes.lastIndex)
                rebuildDisplay()
                syncUndoFlags()
                scheduleAutosave()
            },
            onRedo = {
                if (session.redoStack.isEmpty()) {
                    return@CanvasToolbar
                }
                session.strokes += session.redoStack.removeAt(session.redoStack.lastIndex)
                rebuildDisplay()
                syncUndoFlags()
                scheduleAutosave()
            },
            onClear = { showClearPageDialog = true },
        )
        CanvasPageBar(
            pageIndex = pageIndex,
            pageCount = pages.size,
            onPrevious = { goToPage(pageIndex - 1) },
            onNext = { goToPage(pageIndex + 1) },
            onAdd = { addPage() },
            onDelete = {
                if (pages.size <= 1) {
                    latestOnStatusMessage(deleteLastMessage)
                } else {
                    showDeletePageDialog = true
                }
            },
        )
        Box(
            modifier =
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .onSizeChanged { viewportSize = it },
            contentAlignment = Alignment.Center,
        ) {
            when {
                isLoading -> NotesLoadingIndicator(modifier = Modifier.align(Alignment.Center))

                loadError != null ->
                    Text(
                        text = loadError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(24.dp),
                    )

                hasImage && session.displayBitmap != null && session.sourceBitmap != null -> {
                    val source = session.sourceBitmap!!
                    val display = session.displayBitmap!!
                    val fitScale =
                        if (viewportSize.width > 0 && viewportSize.height > 0) {
                            min(
                                viewportSize.width.toFloat() / source.width,
                                viewportSize.height.toFloat() / source.height,
                            )
                        } else {
                            1f
                        }
                    val imageBitmap = remember(display, displayRevision) { display.asImageBitmap() }

                    Canvas(
                        modifier =
                        Modifier
                            .fillMaxSize()
                            .pointerInput(fitScale, source.width, source.height) {
                                fun toBitmap(change: PointerInputChange): StrokePoint {
                                    val totalScale = fitScale * latestScale
                                    val viewCenter = Offset(size.width / 2f, size.height / 2f)
                                    val bitmapCenter = Offset(source.width / 2f, source.height / 2f)
                                    val unscaled =
                                        (change.position - viewCenter - latestOffset) / totalScale +
                                            bitmapCenter
                                    val pressure =
                                        if (change.type == PointerType.Stylus) {
                                            change.pressure.coerceIn(0.05f, 1f)
                                        } else {
                                            1f
                                        }
                                    return StrokePoint(
                                        x = unscaled.x.coerceIn(0f, source.width.toFloat()),
                                        y = unscaled.y.coerceIn(0f, source.height.toFloat()),
                                        pressure = pressure,
                                    )
                                }

                                awaitEachGesture {
                                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                                    var event = currentEvent
                                    val panOnly =
                                        !latestDrawingEnabled ||
                                            event.changes.count { it.pressed } >= 2
                                    if (panOnly) {
                                        do {
                                            event = awaitPointerEvent(PointerEventPass.Main)
                                            if (event.changes.count { it.pressed } >= 2) {
                                                scale =
                                                    (latestScale * event.calculateZoom())
                                                        .coerceIn(0.4f, 5f)
                                            }
                                            offset = latestOffset + event.calculatePan()
                                            event.changes.forEach { it.consume() }
                                        } while (event.changes.any { it.pressed })
                                        return@awaitEachGesture
                                    }

                                    val strokePoints = mutableListOf(toBitmap(firstDown))
                                    var active =
                                        CanvasStroke(
                                            points = strokePoints.toList(),
                                            color = latestPenColor,
                                            baseWidth = latestBaseWidth,
                                            isEraser = latestTool == CanvasTool.Eraser,
                                        )
                                    currentStroke = active

                                    var strokeFinished = false
                                    var cancelled = false
                                    while (!strokeFinished) {
                                        event = awaitPointerEvent(PointerEventPass.Main)
                                        val multiTouch = event.changes.count { it.pressed } >= 2
                                        val change =
                                            event.changes.firstOrNull { it.id == firstDown.id }
                                        when {
                                            multiTouch -> {
                                                cancelled = true
                                                currentStroke = null
                                                do {
                                                    event = awaitPointerEvent(PointerEventPass.Main)
                                                    scale =
                                                        (latestScale * event.calculateZoom())
                                                            .coerceIn(0.4f, 5f)
                                                    offset = latestOffset + event.calculatePan()
                                                    event.changes.forEach { it.consume() }
                                                } while (event.changes.any { it.pressed })
                                                strokeFinished = true
                                            }

                                            change == null || !change.pressed -> {
                                                change?.consume()
                                                strokeFinished = true
                                            }

                                            change.positionChanged() -> {
                                                strokePoints += toBitmap(change)
                                                active =
                                                    CanvasStroke(
                                                        points = strokePoints.toList(),
                                                        color = latestPenColor,
                                                        baseWidth = latestBaseWidth,
                                                        isEraser = latestTool == CanvasTool.Eraser,
                                                    )
                                                currentStroke = active
                                                change.consume()
                                            }
                                        }
                                    }

                                    currentStroke = null
                                    if (!cancelled && active.points.isNotEmpty()) {
                                        session.strokes += active
                                        session.redoStack.clear()
                                        rebuildDisplay()
                                        syncUndoFlags()
                                        if (!active.isEraser) {
                                            preferences.saveCanvasPenColorArgb(active.color)
                                            preferences.saveCanvasPenWidth(active.baseWidth)
                                        }
                                        scheduleAutosave()
                                    }
                                }
                            },
                    ) {
                        val totalScale = fitScale * scale
                        val dstSize =
                            IntSize(
                                (source.width * totalScale).roundToInt().coerceAtLeast(1),
                                (source.height * totalScale).roundToInt().coerceAtLeast(1),
                            )
                        val dstOffset =
                            IntOffset(
                                ((size.width - dstSize.width) / 2f + offset.x).roundToInt(),
                                ((size.height - dstSize.height) / 2f + offset.y).roundToInt(),
                            )
                        drawRect(
                            color = paperColor,
                            topLeft = Offset(dstOffset.x.toFloat(), dstOffset.y.toFloat()),
                            size = Size(dstSize.width.toFloat(), dstSize.height.toFloat()),
                        )
                        drawImage(
                            image = imageBitmap,
                            dstOffset = dstOffset,
                            dstSize = dstSize,
                        )
                        val live = currentStroke
                        if (live != null) {
                            drawLiveStroke(
                                stroke = live,
                                paperColor = paperColor,
                                totalScale = totalScale,
                                dstOffset = dstOffset,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CanvasPageBar(
    pageIndex: Int,
    pageCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onAdd: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(tonalElevation = 1.dp) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onPrevious,
                    enabled = pageIndex > 0 && pageCount > 0,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.markdown_notes_canvas_page_prev),
                    )
                }
                Text(
                    text =
                    stringResource(
                        R.string.markdown_notes_canvas_page_indicator,
                        if (pageCount == 0) 0 else pageIndex + 1,
                        pageCount,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                )
                IconButton(
                    onClick = onNext,
                    enabled = pageIndex < pageCount - 1,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.markdown_notes_canvas_page_next),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onAdd) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.markdown_notes_canvas_page_add),
                    )
                }
                IconButton(
                    onClick = onDelete,
                    enabled = pageCount > 1,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.markdown_notes_canvas_page_delete),
                    )
                }
            }
        }
    }
}

@Composable
private fun CanvasToolbar(
    tool: CanvasTool,
    drawingEnabled: Boolean,
    penColor: Int,
    paperMode: CanvasPaperMode,
    baseWidth: Float,
    canUndo: Boolean,
    canRedo: Boolean,
    canClear: Boolean,
    onToolChange: (CanvasTool) -> Unit,
    onDrawingEnabledChange: (Boolean) -> Unit,
    onColorChange: (Int) -> Unit,
    onPaperModeChange: (CanvasPaperMode) -> Unit,
    onWidthChange: (Float) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
) {
    var showCustomColorDialog by remember { mutableStateOf(false) }
    val isCustomColor = penColor !in PenColors
    if (showCustomColorDialog) {
        CanvasCustomColorDialog(
            initialColor = penColor,
            onDismiss = { showCustomColorDialog = false },
            onConfirm = { color ->
                onToolChange(CanvasTool.Pen)
                onColorChange(color)
                showCustomColorDialog = false
            },
        )
    }
    Surface(tonalElevation = 2.dp) {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(
                    onClick = {
                        if (drawingEnabled) {
                            onDrawingEnabledChange(false)
                        } else {
                            onDrawingEnabledChange(true)
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.PanTool,
                        contentDescription =
                        if (drawingEnabled) {
                            stringResource(R.string.markdown_notes_canvas_pan)
                        } else {
                            stringResource(R.string.markdown_notes_canvas_draw)
                        },
                        tint =
                        if (!drawingEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                IconButton(onClick = { onToolChange(CanvasTool.Pen) }) {
                    Icon(
                        imageVector = Icons.Filled.Brush,
                        contentDescription = stringResource(R.string.markdown_notes_canvas_pen),
                        tint =
                        if (drawingEnabled && tool == CanvasTool.Pen) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                IconButton(onClick = { onToolChange(CanvasTool.Eraser) }) {
                    Icon(
                        imageVector = Icons.Filled.AutoFixOff,
                        contentDescription = stringResource(R.string.markdown_notes_canvas_eraser),
                        tint =
                        if (drawingEnabled && tool == CanvasTool.Eraser) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                IconButton(onClick = onUndo, enabled = canUndo) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = stringResource(R.string.markdown_notes_canvas_undo),
                    )
                }
                IconButton(onClick = onRedo, enabled = canRedo) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = stringResource(R.string.markdown_notes_canvas_redo),
                    )
                }
                IconButton(onClick = onClear, enabled = canClear) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = stringResource(R.string.markdown_notes_canvas_clear),
                    )
                }
                VerticalDivider(modifier = Modifier.height(28.dp))
                IconButton(onClick = { onPaperModeChange(CanvasPaperMode.Light) }) {
                    Icon(
                        imageVector = Icons.Filled.LightMode,
                        contentDescription = stringResource(R.string.markdown_notes_canvas_paper_light),
                        tint =
                        if (paperMode == CanvasPaperMode.Light) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                IconButton(onClick = { onPaperModeChange(CanvasPaperMode.Dark) }) {
                    Icon(
                        imageVector = Icons.Filled.DarkMode,
                        contentDescription = stringResource(R.string.markdown_notes_canvas_paper_dark),
                        tint =
                        if (paperMode == CanvasPaperMode.Dark) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                IconButton(onClick = { onPaperModeChange(CanvasPaperMode.FollowTheme) }) {
                    Icon(
                        imageVector = Icons.Filled.BrightnessAuto,
                        contentDescription = stringResource(R.string.markdown_notes_canvas_paper_theme),
                        tint =
                        if (paperMode == CanvasPaperMode.FollowTheme) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                PenColors.forEach { color ->
                    val selected = drawingEnabled && color == penColor && tool == CanvasTool.Pen
                    ColorSwatch(
                        color = Color(color),
                        selected = selected,
                        onClick = {
                            onToolChange(CanvasTool.Pen)
                            onColorChange(color)
                        },
                    )
                }
                val customSelected = drawingEnabled && isCustomColor && tool == CanvasTool.Pen
                Box(
                    modifier =
                    Modifier
                        .size(28.dp)
                        .padding(2.dp)
                        .background(
                            brush =
                            ComposeBrush.sweepGradient(
                                listOf(
                                    Color(0xFFE53935),
                                    Color(0xFFFFB300),
                                    Color(0xFF43A047),
                                    Color(0xFF1E88E5),
                                    Color(0xFF8E24AA),
                                    Color(0xFFE53935),
                                ),
                            ),
                            shape = CircleShape,
                        )
                        .then(
                            if (isCustomColor) {
                                Modifier.background(Color(penColor), CircleShape)
                            } else {
                                Modifier
                            },
                        )
                        .border(
                            width = if (customSelected) 2.dp else 1.dp,
                            color =
                            if (customSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = CircleShape,
                        )
                        .clickable { showCustomColorDialog = true },
                    contentAlignment = Alignment.Center,
                ) {
                    if (!isCustomColor) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = stringResource(R.string.markdown_notes_canvas_custom_color),
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
            Slider(
                value = baseWidth,
                onValueChange = onWidthChange,
                valueRange =
                NotesViewerPreferences.MIN_CANVAS_PEN_WIDTH..NotesViewerPreferences.MAX_CANVAS_PEN_WIDTH,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val swatchOutline =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    Box(
        modifier =
        Modifier
            .size(28.dp)
            .padding(2.dp)
            .background(color, CircleShape)
            .border(1.dp, swatchOutline, CircleShape)
            .then(
                if (selected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(
                onClick = onClick,
            ),
    )
}

@Composable
private fun CanvasCustomColorDialog(
    initialColor: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val initialHsv =
        remember(initialColor) {
            FloatArray(3).also { AndroidColor.colorToHSV(initialColor, it) }
        }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2].coerceAtLeast(0.05f)) }
    val previewArgb =
        remember(hue, saturation, value) {
            AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.markdown_notes_canvas_custom_color_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color(previewArgb), RoundedCornerShape(8.dp))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(8.dp),
                        ),
                )
                Text(
                    text = stringResource(R.string.markdown_notes_canvas_custom_color_hue),
                    style = MaterialTheme.typography.labelMedium,
                )
                Slider(
                    value = hue,
                    onValueChange = { hue = it },
                    valueRange = 0f..360f,
                )
                Text(
                    text = stringResource(R.string.markdown_notes_canvas_custom_color_saturation),
                    style = MaterialTheme.typography.labelMedium,
                )
                Slider(
                    value = saturation,
                    onValueChange = { saturation = it },
                    valueRange = 0f..1f,
                )
                Text(
                    text = stringResource(R.string.markdown_notes_canvas_custom_color_value),
                    style = MaterialTheme.typography.labelMedium,
                )
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 0f..1f,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(previewArgb) }) {
                Text(stringResource(R.string.markdown_notes_canvas_custom_color_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.markdown_notes_canvas_custom_color_cancel))
            }
        },
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLiveStroke(
    stroke: CanvasStroke,
    paperColor: Color,
    totalScale: Float,
    dstOffset: IntOffset,
) {
    if (stroke.points.isEmpty()) {
        return
    }
    // Eraser preview paints paper color over ink; commit uses CLEAR on the alpha bitmap.
    val color = if (stroke.isEraser) paperColor else Color(stroke.color)
    if (stroke.points.size == 1) {
        val p = stroke.points.first()
        drawCircle(
            color = color,
            radius = stroke.baseWidth * pressureFactor(p.pressure) * totalScale / 2f,
            center =
            Offset(
                dstOffset.x + p.x * totalScale,
                dstOffset.y + p.y * totalScale,
            ),
        )
        return
    }
    for (i in 1 until stroke.points.size) {
        val a = stroke.points[i - 1]
        val b = stroke.points[i]
        drawLine(
            color = color,
            start =
            Offset(
                dstOffset.x + a.x * totalScale,
                dstOffset.y + a.y * totalScale,
            ),
            end =
            Offset(
                dstOffset.x + b.x * totalScale,
                dstOffset.y + b.y * totalScale,
            ),
            strokeWidth = stroke.baseWidth * pressureFactor(b.pressure) * totalScale,
            cap = StrokeCap.Round,
        )
    }
}

private fun drawStrokeOnAndroid(
    canvas: AndroidCanvas,
    stroke: CanvasStroke,
    paint: Paint,
) {
    if (stroke.points.isEmpty()) {
        return
    }
    if (stroke.isEraser) {
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        paint.color = AndroidColor.TRANSPARENT
    } else {
        paint.xfermode = null
        paint.color = stroke.color
    }
    if (stroke.points.size == 1) {
        val p = stroke.points.first()
        paint.style = Paint.Style.FILL
        val radius = stroke.baseWidth * pressureFactor(p.pressure) / 2f
        canvas.drawCircle(p.x, p.y, radius, paint)
        paint.style = Paint.Style.STROKE
        paint.xfermode = null
        return
    }
    paint.style = Paint.Style.STROKE
    for (i in 1 until stroke.points.size) {
        val a = stroke.points[i - 1]
        val b = stroke.points[i]
        paint.strokeWidth = stroke.baseWidth * pressureFactor(b.pressure)
        canvas.drawLine(a.x, a.y, b.x, b.y, paint)
    }
    paint.xfermode = null
}

private fun pressureFactor(pressure: Float): Float = 0.25f + 0.75f * pressure.coerceIn(0.05f, 1f)

private fun pngBytes(bitmap: Bitmap): ByteArray = ByteArrayOutputStream().use { stream ->
    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
        error("compress failed")
    }
    stream.toByteArray()
}

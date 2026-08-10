package dev.harrix.notes.ui.notes

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixOff
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
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
import dev.harrix.notes.CanvasNoteDefaults
import dev.harrix.notes.NotesPathSegment
import dev.harrix.notes.NotesRelativeDocuments
import dev.harrix.notes.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

private const val CanvasAutosaveDelayMs = 800L
private val PenColors =
    listOf(
        AndroidColor.BLACK,
        AndroidColor.RED,
        AndroidColor.BLUE,
        AndroidColor.GREEN,
        AndroidColor.rgb(0xFF, 0x98, 0x00),
        AndroidColor.MAGENTA,
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
 * Stylus pressure modulates stroke width; content is saved to `img/canvas.png`.
 */
@Composable
fun NotesCanvasPane(
    isLoading: Boolean,
    treeUri: Uri?,
    folderPath: List<NotesPathSegment>,
    noteDocumentId: String,
    contentResolver: ContentResolver,
    modifier: Modifier = Modifier,
    onStatusMessage: (String?) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val session = remember { CanvasSession() }
    var loadError by remember { mutableStateOf<String?>(null) }
    var hasImage by remember { mutableStateOf(false) }
    var tool by remember { mutableStateOf(CanvasTool.Pen) }
    var penColor by remember { mutableIntStateOf(PenColors.first()) }
    var baseWidth by remember { mutableFloatStateOf(6f) }
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
    val latestScale by rememberUpdatedState(scale)
    val latestOffset by rememberUpdatedState(offset)
    val latestTool by rememberUpdatedState(tool)
    val latestPenColor by rememberUpdatedState(penColor)
    val latestBaseWidth by rememberUpdatedState(baseWidth)
    val latestOnStatusMessage by rememberUpdatedState(onStatusMessage)

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

    fun scheduleAutosave() {
        session.dirty = true
        autosaveJob?.cancel()
        autosaveJob =
            scope.launch {
                delay(CanvasAutosaveDelayMs)
                val ok =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            val uri = session.imageUri ?: error("no uri")
                            val display = session.displayBitmap ?: error("no display")
                            val bytes = pngBytes(display)
                            contentResolver.openOutputStream(uri, "w")?.use { output ->
                                output.write(bytes)
                                output.flush()
                            } ?: error("no stream")
                        }.isSuccess
                    }
                if (ok) {
                    session.dirty = false
                    latestOnStatusMessage(null)
                } else {
                    latestOnStatusMessage(saveFailedMessage)
                }
            }
    }

    LaunchedEffect(treeUri, folderPath, noteDocumentId, isLoading) {
        if (isLoading || treeUri == null) {
            return@LaunchedEffect
        }
        loadError = null
        latestOnStatusMessage(null)
        hasImage = false
        val loaded =
            withContext(Dispatchers.IO) {
                val uri =
                    NotesRelativeDocuments.resolve(
                        resolver = contentResolver,
                        treeUri = treeUri,
                        folderPath = folderPath,
                        relativePath = CanvasNoteDefaults.IMAGE_RELATIVE_PATH,
                        noteDocumentId = noteDocumentId,
                    )
                val bytes = uri?.let { NotesRelativeDocuments.readBytes(contentResolver, it) }
                if (uri == null || bytes == null) {
                    null
                } else {
                    val decoded =
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            ?.copy(Bitmap.Config.ARGB_8888, true)
                    if (decoded == null) null else uri to decoded
                }
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

    Column(modifier = modifier.fillMaxSize()) {
        CanvasToolbar(
            tool = tool,
            penColor = penColor,
            baseWidth = baseWidth,
            canUndo = canUndo,
            canRedo = canRedo,
            onToolChange = { tool = it },
            onColorChange = { penColor = it },
            onWidthChange = { baseWidth = it },
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
                                    if (event.changes.count { it.pressed } >= 2) {
                                        do {
                                            event = awaitPointerEvent(PointerEventPass.Main)
                                            scale =
                                                (latestScale * event.calculateZoom()).coerceIn(0.4f, 5f)
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
                        drawImage(
                            image = imageBitmap,
                            dstOffset = dstOffset,
                            dstSize = dstSize,
                        )
                        val live = currentStroke
                        if (live != null) {
                            drawLiveStroke(
                                stroke = live,
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
private fun CanvasToolbar(
    tool: CanvasTool,
    penColor: Int,
    baseWidth: Float,
    canUndo: Boolean,
    canRedo: Boolean,
    onToolChange: (CanvasTool) -> Unit,
    onColorChange: (Int) -> Unit,
    onWidthChange: (Float) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
) {
    Surface(tonalElevation = 2.dp) {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(onClick = { onToolChange(CanvasTool.Pen) }) {
                    Icon(
                        imageVector = Icons.Filled.Brush,
                        contentDescription = stringResource(R.string.markdown_notes_canvas_pen),
                        tint =
                        if (tool == CanvasTool.Pen) {
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
                        if (tool == CanvasTool.Eraser) {
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
                PenColors.forEach { color ->
                    val selected = color == penColor && tool == CanvasTool.Pen
                    Box(
                        modifier =
                        Modifier
                            .size(28.dp)
                            .padding(2.dp)
                            .background(Color(color), CircleShape)
                            .then(
                                if (selected) {
                                    Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape,
                                    )
                                } else {
                                    Modifier
                                },
                            )
                            .clickable {
                                onToolChange(CanvasTool.Pen)
                                onColorChange(color)
                            },
                    )
                }
            }
            Slider(
                value = baseWidth,
                onValueChange = onWidthChange,
                valueRange = 2f..28f,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLiveStroke(
    stroke: CanvasStroke,
    totalScale: Float,
    dstOffset: IntOffset,
) {
    if (stroke.points.isEmpty()) {
        return
    }
    val color = if (stroke.isEraser) Color.White else Color(stroke.color)
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
    paint.color = if (stroke.isEraser) AndroidColor.WHITE else stroke.color
    if (stroke.points.size == 1) {
        val p = stroke.points.first()
        paint.style = Paint.Style.FILL
        val radius = stroke.baseWidth * pressureFactor(p.pressure) / 2f
        canvas.drawCircle(p.x, p.y, radius, paint)
        paint.style = Paint.Style.STROKE
        return
    }
    paint.style = Paint.Style.STROKE
    for (i in 1 until stroke.points.size) {
        val a = stroke.points[i - 1]
        val b = stroke.points[i]
        paint.strokeWidth = stroke.baseWidth * pressureFactor(b.pressure)
        canvas.drawLine(a.x, a.y, b.x, b.y, paint)
    }
}

private fun pressureFactor(pressure: Float): Float = 0.25f + 0.75f * pressure.coerceIn(0.05f, 1f)

private fun pngBytes(bitmap: Bitmap): ByteArray = ByteArrayOutputStream().use { stream ->
    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
        error("compress failed")
    }
    stream.toByteArray()
}

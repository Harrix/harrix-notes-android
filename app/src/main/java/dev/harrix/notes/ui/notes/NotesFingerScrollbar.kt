package dev.harrix.notes.ui.notes

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/** Idle / “normal” thin scrollbar (matches system-style bars). */
private val NotesScrollbarCompactTrackWidth = 4.dp
private val NotesScrollbarCompactThumbWidth = 3.dp

/** Finger-friendly size while the user is scrolling or scrubbing. */
private val NotesScrollbarExpandedTrackWidth = 28.dp
private val NotesScrollbarExpandedThumbWidth = 10.dp

private val NotesScrollbarMinThumbHeight = 48.dp
private val NotesScrollbarThumbCorner = 5.dp
private val NotesScrollbarExpandAnimMs = 160
private const val NotesScrollbarCollapseDelayMs = 1_200L

/**
 * Vertical scrollbar that stays thin at rest and expands to a wide, draggable
 * control while the content is scrolling (or while the thumb is dragged).
 * After scrolling stops it collapses again after a short delay.
 */
@Composable
fun NotesFingerScrollbar(
    scrollOffset: Float,
    maxScrollOffset: Float,
    viewportSize: Float,
    contentSize: Float,
    onScrollOffsetChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    thumbColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
    trackColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
) {
    if (contentSize <= viewportSize || maxScrollOffset <= 0f || viewportSize <= 0f) {
        return
    }

    var expanded by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var previousScrollOffset by remember { mutableFloatStateOf(Float.NaN) }

    LaunchedEffect(scrollOffset, dragging) {
        if (dragging) {
            expanded = true
            return@LaunchedEffect
        }
        val previous = previousScrollOffset
        previousScrollOffset = scrollOffset
        if (!previous.isNaN() && abs(previous - scrollOffset) > 0.5f) {
            expanded = true
        }
        if (!expanded) {
            return@LaunchedEffect
        }
        delay(NotesScrollbarCollapseDelayMs)
        expanded = false
    }

    val trackWidth by animateDpAsState(
        targetValue =
        if (expanded) {
            NotesScrollbarExpandedTrackWidth
        } else {
            NotesScrollbarCompactTrackWidth
        },
        animationSpec = tween(NotesScrollbarExpandAnimMs),
        label = "notesScrollbarTrackWidth",
    )
    val thumbWidth by animateDpAsState(
        targetValue =
        if (expanded) {
            NotesScrollbarExpandedThumbWidth
        } else {
            NotesScrollbarCompactThumbWidth
        },
        animationSpec = tween(NotesScrollbarExpandAnimMs),
        label = "notesScrollbarThumbWidth",
    )

    val density = LocalDensity.current
    BoxWithConstraints(
        modifier =
        modifier
            .width(trackWidth)
            .fillMaxHeight(),
    ) {
        val trackHeightPx = with(density) { maxHeight.toPx() }
        val minThumbPx =
            with(density) {
                if (expanded) {
                    NotesScrollbarMinThumbHeight.toPx()
                } else {
                    24.dp.toPx()
                }
            }
        val thumbHeightPx =
            (trackHeightPx * (viewportSize / contentSize))
                .coerceIn(minThumbPx, trackHeightPx)
        val travelPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(0f)
        val fraction = (scrollOffset / maxScrollOffset).coerceIn(0f, 1f)
        val thumbOffsetPx = travelPx * fraction

        fun seekToTrackY(y: Float) {
            if (travelPx <= 0f) {
                return
            }
            val centerAdjusted = (y - thumbHeightPx / 2f).coerceIn(0f, travelPx)
            onScrollOffsetChange((centerAdjusted / travelPx) * maxScrollOffset)
        }

        Box(
            modifier =
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clip(RoundedCornerShape(trackWidth / 2))
                .background(
                    if (expanded) {
                        trackColor
                    } else {
                        Color.Transparent
                    },
                )
                .then(
                    if (expanded) {
                        Modifier
                            .pointerInput(maxScrollOffset, travelPx, thumbHeightPx) {
                                detectTapGestures { offset ->
                                    seekToTrackY(offset.y)
                                }
                            }
                            .pointerInput(maxScrollOffset, travelPx, thumbHeightPx) {
                                var dragThumbTop = thumbOffsetPx
                                detectVerticalDragGestures(
                                    onDragStart = { offset ->
                                        dragging = true
                                        val onThumb =
                                            offset.y in
                                                thumbOffsetPx..(thumbOffsetPx + thumbHeightPx)
                                        dragThumbTop =
                                            if (onThumb) {
                                                thumbOffsetPx
                                            } else {
                                                seekToTrackY(offset.y)
                                                (offset.y - thumbHeightPx / 2f)
                                                    .coerceIn(0f, travelPx)
                                            }
                                    },
                                    onDragEnd = { dragging = false },
                                    onDragCancel = { dragging = false },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        if (travelPx <= 0f) {
                                            return@detectVerticalDragGestures
                                        }
                                        dragThumbTop =
                                            (dragThumbTop + dragAmount).coerceIn(0f, travelPx)
                                        onScrollOffsetChange(
                                            (dragThumbTop / travelPx) * maxScrollOffset,
                                        )
                                    },
                                )
                            }
                    } else {
                        Modifier
                    },
                ),
        ) {
            Box(
                modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .width(thumbWidth)
                    .height(with(density) { thumbHeightPx.toDp() })
                    .offset(y = with(density) { thumbOffsetPx.toDp() })
                    .background(
                        color =
                        if (expanded) {
                            thumbColor
                        } else {
                            thumbColor.copy(alpha = thumbColor.alpha * 0.85f)
                        },
                        shape = RoundedCornerShape(NotesScrollbarThumbCorner),
                    ),
            )
        }
    }
}

/** Snapshot of a scrollable surface for [NotesFingerScrollbar]. */
data class NotesScrollMetrics(
    val scrollOffset: Float = 0f,
    val maxScrollOffset: Float = 0f,
    val viewportSize: Float = 0f,
    val contentSize: Float = 0f,
) {
    val canScroll: Boolean
        get() = contentSize > viewportSize && maxScrollOffset > 0f

    companion object {
        fun fromWebView(
            scrollY: Int,
            computeVerticalScrollRange: Int,
            computeVerticalScrollExtent: Int,
        ): NotesScrollMetrics {
            val viewport = computeVerticalScrollExtent.toFloat().coerceAtLeast(0f)
            val content = computeVerticalScrollRange.toFloat().coerceAtLeast(viewport)
            val max = (content - viewport).coerceAtLeast(0f)
            return NotesScrollMetrics(
                scrollOffset = scrollY.toFloat().coerceIn(0f, max),
                maxScrollOffset = max,
                viewportSize = viewport,
                contentSize = content,
            )
        }

        fun fromScroller(
            scrollTop: Int,
            scrollHeight: Int,
            clientHeight: Int,
        ): NotesScrollMetrics {
            val viewport = clientHeight.toFloat().coerceAtLeast(0f)
            val content = scrollHeight.toFloat().coerceAtLeast(viewport)
            val max = (content - viewport).coerceAtLeast(0f)
            return NotesScrollMetrics(
                scrollOffset = scrollTop.toFloat().coerceIn(0f, max),
                maxScrollOffset = max,
                viewportSize = viewport,
                contentSize = content,
            )
        }
    }
}

fun Float.toScrollPxInt(): Int = roundToInt().coerceAtLeast(0)

package dev.harrix.notes.ui.notes

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Wide touch target so the scrollbar is usable with a finger on a phone. */
private val NotesFingerScrollbarTrackWidth = 28.dp
private val NotesFingerScrollbarThumbWidth = 10.dp
private val NotesFingerScrollbarMinThumbHeight = 48.dp
private val NotesFingerScrollbarThumbCorner = 5.dp

/**
 * Always-visible vertical scrollbar with a large drag hit area.
 *
 * [scrollOffset] / [maxScrollOffset] are in the same units as the underlying
 * scroller (pixels). [viewportSize] and [contentSize] size the thumb.
 */
@Composable
fun NotesFingerScrollbar(
    scrollOffset: Float,
    maxScrollOffset: Float,
    viewportSize: Float,
    contentSize: Float,
    onScrollOffsetChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    trackWidth: Dp = NotesFingerScrollbarTrackWidth,
    thumbWidth: Dp = NotesFingerScrollbarThumbWidth,
    thumbColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
    trackColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
) {
    if (contentSize <= viewportSize || maxScrollOffset <= 0f || viewportSize <= 0f) {
        return
    }
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier =
        modifier
            .width(trackWidth)
            .fillMaxHeight(),
    ) {
        val trackHeightPx = with(density) { maxHeight.toPx() }
        val minThumbPx = with(density) { NotesFingerScrollbarMinThumbHeight.toPx() }
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
                .background(trackColor)
                .pointerInput(maxScrollOffset, travelPx, thumbHeightPx) {
                    detectTapGestures { offset ->
                        seekToTrackY(offset.y)
                    }
                }
                .pointerInput(maxScrollOffset, travelPx, thumbHeightPx) {
                    var dragThumbTop = thumbOffsetPx
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            val onThumb =
                                offset.y in thumbOffsetPx..(thumbOffsetPx + thumbHeightPx)
                            dragThumbTop =
                                if (onThumb) {
                                    thumbOffsetPx
                                } else {
                                    seekToTrackY(offset.y)
                                    (offset.y - thumbHeightPx / 2f).coerceIn(0f, travelPx)
                                }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            if (travelPx <= 0f) {
                                return@detectVerticalDragGestures
                            }
                            dragThumbTop = (dragThumbTop + dragAmount).coerceIn(0f, travelPx)
                            onScrollOffsetChange((dragThumbTop / travelPx) * maxScrollOffset)
                        },
                    )
                },
        ) {
            Box(
                modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .width(thumbWidth)
                    .height(with(density) { thumbHeightPx.toDp() })
                    .offset(y = with(density) { thumbOffsetPx.toDp() })
                    .background(
                        color = thumbColor,
                        shape = RoundedCornerShape(NotesFingerScrollbarThumbCorner),
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

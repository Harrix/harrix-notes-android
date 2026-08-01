package dev.harrix.notes.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/** Compact phone width where dense chrome and labels tend to overflow. */
const val CompactScreenWidthDp = 400

private const val MediumScreenWidthDp = 600

private const val ExpandedScreenWidthDp = 840

private val AdaptiveContentMaxWidth = 840.dp

private val DrawerMaxWidth = 320.dp

@Composable
fun isCompactWidth(): Boolean = LocalConfiguration.current.screenWidthDp < CompactScreenWidthDp

@Composable
fun isCompactHeight(): Boolean = LocalConfiguration.current.screenHeightDp < CompactScreenWidthDp

@Composable
fun screenWidthDp(): Int = LocalConfiguration.current.screenWidthDp

/** Icons browse grid: fewer columns on landscape phones, more on tablets. */
@Composable
fun notesIconsGridColumnCount(): Int {
    val configuration = LocalConfiguration.current
    val width = configuration.screenWidthDp
    val height = configuration.screenHeightDp
    return when {
        width >= ExpandedScreenWidthDp -> 6
        height < CompactScreenWidthDp && width >= MediumScreenWidthDp -> 4
        width >= MediumScreenWidthDp -> 5
        else -> 3
    }
}

/** Limits settings / welcome content width on tablets and centers it. */
@Composable
fun Modifier.adaptiveContentWidth(): Modifier = this
    .fillMaxWidth()
    .wrapContentWidth(Alignment.CenterHorizontally)
    .widthIn(max = AdaptiveContentMaxWidth)

/** Caps navigation drawer width on wide screens. */
@Composable
fun Modifier.adaptiveDrawerWidth(): Modifier = this.widthIn(max = DrawerMaxWidth)

package dev.harrix.notes.ui.notes

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val NotesMenuItemHorizontalPadding = 12.dp
private val NotesMenuItemVerticalPadding = 4.dp
private val NotesMenuItemMinHeight = 36.dp
private val NotesMenuItemMaxHeight = 40.dp
private val NotesMenuCheckboxSize = 24.dp

/** Dense dropdown rows; Material defaults leave too much vertical gap. */
val NotesMenuItemContentPadding =
    PaddingValues(
        horizontal = NotesMenuItemHorizontalPadding,
        vertical = NotesMenuItemVerticalPadding,
    )

@Composable
fun NotesDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        DropdownMenuItem(
            text = text,
            onClick = onClick,
            modifier =
            modifier.heightIn(
                min = NotesMenuItemMinHeight,
                max = NotesMenuItemMaxHeight,
            ),
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            enabled = enabled,
            colors = MenuDefaults.itemColors(),
            contentPadding = NotesMenuItemContentPadding,
        )
    }
}

/** Compact checkbox for dropdown rows (still larger than the ultra-dense variant). */
@Composable
fun NotesMenuCheckbox(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            modifier = modifier.size(NotesMenuCheckboxSize),
        )
    }
}

package dev.harrix.notes.ui.notes

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val NotesMenuItemHorizontalPadding = 12.dp
private val NotesMenuItemVerticalPadding = 0.dp
private val NotesMenuItemMinHeight = 32.dp

/** Tighter than Material3 defaults so overflow menus feel denser. */
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
    DropdownMenuItem(
        text = text,
        onClick = onClick,
        modifier = modifier.heightIn(min = NotesMenuItemMinHeight),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        colors = MenuDefaults.itemColors(),
        contentPadding = NotesMenuItemContentPadding,
    )
}

/** Compact checkbox for dense dropdown rows (`onCheckedChange = null` skips 48.dp target). */
@Composable
fun NotesMenuCheckbox(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    Checkbox(
        checked = checked,
        onCheckedChange = null,
        modifier = modifier,
    )
}

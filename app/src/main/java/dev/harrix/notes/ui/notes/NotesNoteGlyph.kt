package dev.harrix.notes.ui.notes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import dev.harrix.notes.NotesIconStyle
import dev.harrix.notes.R

/** Current folder/file icon set; provided by the notes viewer and settings. */
val LocalNotesIconStyle = compositionLocalOf { NotesIconStyle.Default }

/** Folder icon according to [LocalNotesIconStyle]. */
@Composable
fun NotesFolderGlyph(
    size: Dp,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    when (LocalNotesIconStyle.current) {
        NotesIconStyle.Harrix -> {
            Icon(
                painter = painterResource(R.drawable.ic_harrix_folder),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = modifier.size(size),
            )
        }

        NotesIconStyle.Material -> {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = tint,
                modifier = modifier.size(size),
            )
        }
    }
}

/** File icon, or YAML `icon:` emoji when [icon] is non-empty. */
@Composable
fun NotesNoteGlyph(
    icon: String,
    size: Dp,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    if (icon.isNotEmpty()) {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = icon,
                fontSize = (size.value * 0.85f).sp,
                lineHeight = size.value.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    } else {
        when (LocalNotesIconStyle.current) {
            NotesIconStyle.Harrix -> {
                Icon(
                    painter = painterResource(R.drawable.ic_harrix_file_text),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = modifier.size(size),
                )
            }

            NotesIconStyle.Material -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = tint,
                    modifier = modifier.size(size),
                )
            }
        }
    }
}

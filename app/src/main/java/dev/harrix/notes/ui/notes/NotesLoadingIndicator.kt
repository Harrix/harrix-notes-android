package dev.harrix.notes.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.harrix.notes.R
import kotlinx.coroutines.delay

private const val ELAPSED_TIME_VISIBLE_AFTER_SECONDS = 2

/** Spinner that shows elapsed wait time after [ELAPSED_TIME_VISIBLE_AFTER_SECONDS] seconds. */
@Composable
fun NotesLoadingIndicator(
    modifier: Modifier = Modifier,
    indicatorSize: Dp = 40.dp,
) {
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        elapsedSeconds = 0
        while (true) {
            delay(1_000)
            elapsedSeconds += 1
        }
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(indicatorSize))
        if (elapsedSeconds >= ELAPSED_TIME_VISIBLE_AFTER_SECONDS) {
            Text(
                text = stringResource(R.string.loading_elapsed_seconds, elapsedSeconds),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

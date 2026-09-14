package com.routewatcher.app.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.routewatcher.app.R

// Saved quick pick time & routes current time
data class FavoriteTime(val hour: Int, val minute: Int)

// AM/PM format
//private fun FavoriteTime.label(): String {
//    val period = if (hour >= 12) "PM" else "AM"
//    val h12 = if (hour % 12 == 0) 12 else hour % 12
//    return "$h12:${minute.toString().padStart(2, '0')} $period"
//}

private fun FavoriteTime.label(): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

// Row of time tiles.
// Tap selects a time, long press marks a tile.
// Action button shows "+" normally, switches to delete icon while any tile is marked.
@Composable
fun TimeTileRow(
    currentHour: Int,
    currentMinute: Int,
    favorites: List<FavoriteTime>,
    onSelectTime: (FavoriteTime) -> Unit,
    onAddTimeRequested: () -> Unit,
    onDeleteTimes: (Set<FavoriteTime>) -> Boolean, // returns true if the current time was blocked
    modifier: Modifier = Modifier,
) {
    var marked by remember { mutableStateOf(setOf<FavoriteTime>()) }
    var showBlockedNotice by remember { mutableStateOf(false) }

    val current = FavoriteTime(currentHour, currentMinute)
    val tiles = (favorites + current).distinct().sortedBy { it.hour * 60 + it.minute }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LazyRow(modifier = Modifier.weight(1f)) {
                items(tiles, key = { "${it.hour}:${it.minute}" }) { tile ->
                    val isMarked = tile in marked
                    val isCurrent = tile == current
                    Surface(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .combinedClickable(
                                onClick = {
                                    showBlockedNotice = false
                                    if (marked.isNotEmpty()) {
                                        marked = if (isMarked) marked - tile else marked + tile
                                    } else {
                                        onSelectTime(tile)
                                    }
                                },
                                onLongClick = {
                                    showBlockedNotice = false
                                    marked = if (isMarked) marked - tile else marked + tile
                                },
                            ),
                        shape = MaterialTheme.shapes.large,
                        color = when {
                            isMarked -> MaterialTheme.colorScheme.errorContainer
                            isCurrent -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (isMarked) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                tile.label(),
                                color = when {
                                    isMarked -> MaterialTheme.colorScheme.onErrorContainer
                                    isCurrent -> MaterialTheme.colorScheme.onPrimary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }
            IconButton(
                onClick = {
                    if (marked.isNotEmpty()) {
                        showBlockedNotice = onDeleteTimes(marked)
                        marked = emptySet()
                    } else {
                        onAddTimeRequested()
                    }
                },
            ) {
                if (marked.isNotEmpty()) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                } else {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.save_as_quick_pick))
                }
            }
        }
        Text(
            text = if (showBlockedNotice) {
                stringResource(R.string.time_tile_delete_current_blocked)
            } else {
                stringResource(R.string.time_tile_hint)
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (showBlockedNotice) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Standard Material3 clock dialog & checkbox for saving the picked time as a quick pick
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartureTimeDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int, saveAsFavorite: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    var saveAsFavorite by remember { mutableStateOf(false) }

    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = {},
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute, saveAsFavorite) }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TimePicker(state = state)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = saveAsFavorite, onCheckedChange = { saveAsFavorite = it })
                Text(stringResource(R.string.save_as_quick_pick))
            }
        }
    }
}
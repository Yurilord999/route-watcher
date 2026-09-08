package com.routewatcher.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.routewatcher.app.R
import com.routewatcher.app.data.RouteEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRouteScreen(
    name: String,
    onNameChange: (String) -> Unit,
    origin: String,
    onOriginChange: (String) -> Unit,
    destination: String,
    onDestinationChange: (String) -> Unit,
    hour: String,
    onHourChange: (String) -> Unit,
    minute: String,
    onMinuteChange: (String) -> Unit,
    activeDays: Int,
    onActiveDaysChange: (Int) -> Unit,
    offsets: String,
    onOffsetsChange: (String) -> Unit,
    threshold: String,
    onThresholdChange: (String) -> Unit,
    lockedRouteSummary: String?,
    isNewRoute: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onPickRoad: () -> Unit,
    onCancel: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.add_route)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ---- basic route info: name, origin, destination ----
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.route_name)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = origin,
                onValueChange = onOriginChange,
                label = { Text(stringResource(R.string.origin_address)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = destination,
                onValueChange = onDestinationChange,
                label = { Text(stringResource(R.string.destination_address)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))

            // ---- departure time ----
            Row {
                OutlinedTextField(
                    value = hour,
                    onValueChange = { onHourChange(it.filter { c -> c.isDigit() }) },
                    label = { Text(stringResource(R.string.departure_hour)) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = minute,
                    onValueChange = { onMinuteChange(it.filter { c -> c.isDigit() }) },
                    label = { Text(stringResource(R.string.departure_minute)) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))

            // ---- active days: toggles + week/weekdays/weekend presets ----
            Text(stringResource(R.string.route_active_days), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                daysForDisplay().forEach { (label, bit) ->
                    DayCircle(
                        label = label,
                        active = (activeDays and bit) != 0,
                        onClick = { onActiveDaysChange(activeDays xor bit) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = activeDays == RouteEntity.ALL_DAYS,
                    onClick = { onActiveDaysChange(RouteEntity.ALL_DAYS) },
                    label = { Text(stringResource(R.string.preset_full_week)) },
                )
                FilterChip(
                    selected = activeDays == RouteEntity.WEEKDAYS,
                    onClick = { onActiveDaysChange(RouteEntity.WEEKDAYS) },
                    label = { Text(stringResource(R.string.preset_weekdays)) },
                )
                FilterChip(
                    selected = activeDays == RouteEntity.WEEKEND,
                    onClick = { onActiveDaysChange(RouteEntity.WEEKEND) },
                    label = { Text(stringResource(R.string.preset_weekend)) },
                )
            }
            Spacer(Modifier.height(24.dp))

            // ---- check timing & alert threshold ----
            OutlinedTextField(
                value = offsets,
                onValueChange = onOffsetsChange,
                label = { Text(stringResource(R.string.check_offsets)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = threshold,
                onValueChange = { onThresholdChange(it.filter { c -> c.isDigit() }) },
                label = { Text(stringResource(R.string.delay_threshold)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))

            // ---- picked road summary ----
            Text(
                if (lockedRouteSummary != null) {
                    stringResource(R.string.picked_road_summary, lockedRouteSummary)
                } else {
                    stringResource(R.string.no_road_picked)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onPickRoad,
                modifier = Modifier.fillMaxWidth(),
                ) {
                Text(stringResource(if (lockedRouteSummary != null) R.string.change_picked_road else R.string.pick_road_on_map))
                }
            Spacer(Modifier.height(8.dp))

            // ---- save / cancel / delete ----
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save_route))
            }
            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.cancel))
            }

            if (!isNewRoute) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(R.string.delete_route),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    // ---- delete confirmation dialog ----
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    stringResource(R.string.delete_route_confirm_title),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            showDeleteConfirm = false
                            onDelete()
                        },
                    ) {
                        Text(stringResource(R.string.delete_route), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
        )
    }
}
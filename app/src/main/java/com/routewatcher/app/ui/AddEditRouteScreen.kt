package com.routewatcher.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.routewatcher.app.R
import com.routewatcher.app.data.RouteEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRouteScreen(
    existing: RouteEntity?,
    onSave: (RouteEntity) -> Unit,
    onDelete: (() -> Unit)?,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var origin by remember { mutableStateOf(existing?.originAddress ?: "") }
    var destination by remember { mutableStateOf(existing?.destinationAddress ?: "") }
    var hour by remember { mutableStateOf((existing?.departureHour ?: 8).toString()) }
    var minute by remember { mutableStateOf((existing?.departureMinute ?: 0).toString()) }
    var offsets by remember { mutableStateOf(existing?.checkOffsetsMinutes ?: "30") }
    var threshold by remember { mutableStateOf((existing?.delayThresholdMinutes ?: 10).toString()) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.add_route)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.route_name)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = origin,
                onValueChange = { origin = it },
                label = { Text(stringResource(R.string.origin_address)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text(stringResource(R.string.destination_address)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))

            Row {
                OutlinedTextField(
                    value = hour,
                    onValueChange = { hour = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.departure_hour)) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = minute,
                    onValueChange = { minute = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.departure_minute)) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = offsets,
                onValueChange = { offsets = it },
                label = { Text(stringResource(R.string.check_offsets)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = threshold,
                onValueChange = { threshold = it.filter { c -> c.isDigit() } },
                label = { Text(stringResource(R.string.delay_threshold)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    onSave(
                        RouteEntity(
                            id = existing?.id ?: 0,
                            name = name.ifBlank { "Route" },
                            originAddress = origin,
                            destinationAddress = destination,
                            departureHour = hour.toIntOrNull()?.coerceIn(0, 23) ?: 8,
                            departureMinute = minute.toIntOrNull()?.coerceIn(0, 59) ?: 0,
                            checkOffsetsMinutes = offsets.ifBlank { "30" },
                            delayThresholdMinutes = threshold.toIntOrNull() ?: 10,
                            activeDays = existing?.activeDays ?: 0b1111100,
                            enabled = existing?.enabled ?: false,
                            ),
                        )
                    },
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

            if (onDelete != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onDelete,
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
}
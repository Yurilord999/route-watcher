package com.routewatcher.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.CameraPositionState
import com.routewatcher.app.R
import com.routewatcher.app.network.RouteOption
import com.routewatcher.app.network.AddressPrediction
import com.routewatcher.app.network.RouteAlternative
import com.routewatcher.app.data.RouteEntity

private val DRESDEN_HAUPTBAHNHOF = LatLng(51.0405, 13.7325)

// ---- fullscreen add/edit route form: search fields + map ----
// Bare state: just the map. Full state: includes draggable bottom sheet.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteFormScreen(
    origin: String,
    onOriginChange: (String) -> Unit,
    destination: String,
    onDestinationChange: (String) -> Unit,
    originPredictions: List<AddressPrediction>,
    destinationPredictions: List<AddressPrediction>,
    onOriginPredictionSelected: (AddressPrediction) -> Unit,
    onDestinationPredictionSelected: (AddressPrediction) -> Unit,
    onSwap: () -> Unit,
    onMoreOptions: () -> Unit,
    alternatives: List<RouteAlternative>,
    selectedAlternative: Int?,
    onAlternativeSelected: (Int) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
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
    stopsCount: Int,
    onAddStops: () -> Unit,
    isNewRoute: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    val hasResolvedRoute = alternatives.isNotEmpty()
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false,
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)

    LaunchedEffect(hasResolvedRoute) {
        if (hasResolvedRoute) {
            sheetState.partialExpand()
        } else {
            sheetState.hide()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 120.dp,
        sheetContent = {
            if (hasResolvedRoute) {
                val selected = selectedAlternative?.let { alternatives.getOrNull(it) }
                RouteFormSheetContent(
                    routeSummary = selected?.let {
                        stringResource(R.string.route_form_summary, it.option.durationMinutes, it.option.distanceText)
                    },
                    stopsCount = stopsCount,
                    onAddStops = onAddStops,
                    name = name,
                    onNameChange = onNameChange,
                    hour = hour,
                    onHourChange = onHourChange,
                    minute = minute,
                    onMinuteChange = onMinuteChange,
                    activeDays = activeDays,
                    onActiveDaysChange = onActiveDaysChange,
                    offsets = offsets,
                    onOffsetsChange = onOffsetsChange,
                    threshold = threshold,
                    onThresholdChange = onThresholdChange,
                    isNewRoute = isNewRoute,
                    onSave = onSave,
                    onDelete = onDelete,
                    onCancel = onCancel,
                )
            }
        },
    ) {
        RouteFormMapArea(
            origin = origin,
            onOriginChange = onOriginChange,
            destination = destination,
            onDestinationChange = onDestinationChange,
            originPredictions = originPredictions,
            destinationPredictions = destinationPredictions,
            onOriginPredictionSelected = onOriginPredictionSelected,
            onDestinationPredictionSelected = onDestinationPredictionSelected,
            onSwap = onSwap,
            onMoreOptions = onMoreOptions,
            alternatives = alternatives,
            selectedAlternative = selectedAlternative,
            onAlternativeSelected = onAlternativeSelected,
            onCancel = onCancel,
        )
    }
}

// All add/edit form settings (full state)
@Composable
private fun RouteFormSheetContent(
    routeSummary: String?,
    stopsCount: Int,
    onAddStops: () -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
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
    isNewRoute: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        // ---- route summary + stops, side by side to stay compact ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(routeSummary ?: "", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = onAddStops) {
                Text(
                    if (stopsCount > 0) {
                        pluralStringResource(R.plurals.route_form_stops_count, stopsCount, stopsCount)
                    } else {
                        stringResource(R.string.route_form_add_stops)
                    },
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        // ---- route name ----
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.route_name)) },
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

// Map, address fields and everything floating on top is shared by bare & full state
@Composable
private fun RouteFormMapArea(
    origin: String,
    onOriginChange: (String) -> Unit,
    destination: String,
    onDestinationChange: (String) -> Unit,
    originPredictions: List<AddressPrediction>,
    destinationPredictions: List<AddressPrediction>,
    onOriginPredictionSelected: (AddressPrediction) -> Unit,
    onDestinationPredictionSelected: (AddressPrediction) -> Unit,
    onSwap: () -> Unit,
    onMoreOptions: () -> Unit,
    alternatives: List<RouteAlternative>,
    selectedAlternative: Int?,
    onAlternativeSelected: (Int) -> Unit,
    onCancel: () -> Unit,
) {

    var originFocused by remember { mutableStateOf(false) }
    var destinationFocused by remember { mutableStateOf(false) }
    var trafficEnabled by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Box(Modifier.fillMaxSize()) {
        // ---- map (background, edge-to-edge) ----
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(DRESDEN_HAUPTBAHNHOF, 13f)
        }
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isTrafficEnabled = trafficEnabled),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = true,
                compassEnabled = true,
            ),
            contentPadding = PaddingValues(bottom = 48.dp),
            onMapClick = { focusManager.clearFocus() },
        ) {
            alternatives.forEachIndexed { index, alt ->
                Polyline(
                    points = alt.points,
                    color = if (index == selectedAlternative) {
                        Color(0xFF1A73E8)
                    } else {
                        Color(0xFF8AB4F8)
                    },
                    width = if (index == selectedAlternative) 14f else 8f,
                    clickable = true,
                    onClick = { onAlternativeSelected(index) },
                )
            }
        }

        // ---- route duration bubble ----
        alternatives.forEachIndexed { index, alt ->
            val anchor = alt.points[alt.points.size / 2]
            RouteTimeBubble(
                text = stringResource(R.string.route_form_bubble_minutes, alt.option.durationMinutes),
                selected = index == selectedAlternative,
                modifier = Modifier
                    .offset { latLngToScreenOffset(cameraPositionState, anchor) }
                    .offset(x = (-18).dp, y = (-32).dp)
                    .clickable { onAlternativeSelected(index) },
            )
        }

        // ---- traffic toggle (reused from the minimap) ----
        TrafficToggleButton(
            enabled = trafficEnabled,
            onToggle = { trafficEnabled = !trafficEnabled },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 200.dp, end = 8.dp),
        )

        // ---- back / cancel ----
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onCancel),
            contentAlignment = Alignment.Center,
        ) {
            Text("\u2190", style = MaterialTheme.typography.titleMedium)
        }

        // ---- origin / destination fields ----
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp, start = 12.dp, end = 12.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                OutlinedTextField(
                    value = origin,
                    onValueChange = onOriginChange,
                    placeholder = { Text(stringResource(R.string.origin_address)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { originFocused = it.isFocused },
                )
                Text(
                    "\u22EE",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .clickable(onClick = onMoreOptions)
                        .padding(8.dp),
                )
            }
            HorizontalDivider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                OutlinedTextField(
                    value = destination,
                    onValueChange = onDestinationChange,
                    placeholder = { Text(stringResource(R.string.destination_address)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { destinationFocused = it.isFocused },
                )
                Text(
                    "\u21C5",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .clickable(onClick = onSwap)
                        .padding(8.dp),
                )
            }

            if (originFocused && originPredictions.isNotEmpty()) {
                PredictionsList(originPredictions, onClick = {
                    onOriginPredictionSelected(it)
                    focusManager.clearFocus()
                })
            } else if (destinationFocused && destinationPredictions.isNotEmpty()) {
                PredictionsList(destinationPredictions, onClick = {
                    onDestinationPredictionSelected(it)
                    focusManager.clearFocus()
                })
            }
        }
    }
}

// Converts a map coordinate into its current on-screen pixel position
// Duration bubbles will remain attached to the route this way
private fun latLngToScreenOffset(cameraPositionState: CameraPositionState, latLng: LatLng): IntOffset {
    cameraPositionState.position
    return cameraPositionState.projection
        ?.toScreenLocation(latLng)
        ?.let { IntOffset(it.x, it.y) }
        ?: IntOffset.Zero
}

// displays route durations
@Composable
private fun RouteTimeBubble(text: String, selected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PredictionsList(predictions: List<AddressPrediction>, onClick: (AddressPrediction) -> Unit) {
    LazyColumn {
        items(predictions) { prediction ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(prediction) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(prediction.primaryText, style = MaterialTheme.typography.bodyMedium)
                Text(
                    prediction.secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
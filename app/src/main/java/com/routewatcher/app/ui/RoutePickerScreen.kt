package com.routewatcher.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment


import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberMarkerState
import com.routewatcher.app.network.RouteOption

// Lets the user pick their route of choice (similar to Google Maps)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePickerScreen(
    routeOptions: List<RouteOption>,
    isLoading: Boolean,
    initiallySelectedPolyline: String?,
    onConfirm: (RouteOption, Boolean) -> Unit,
    onCancel: () -> Unit,
    isCustomizing: Boolean,
    stops: List<Pair<Double, Double>>,
    onModeChange: (Boolean) -> Unit,
    onAddStop: (Double, Double) -> Unit,
    onMoveStop: (Int, Double, Double) -> Unit,
    onRemoveStop: (Int) -> Unit,
    customRoute: RouteOption?,
    isRecomputing: Boolean,
) {
    // Match against whatever is already picked/saved
    // Reopening the picker shows the current choice instead of defaulting to the first option
    // Falls back to 0 if there is nothing to match / no match found
    // TODO: bandaid, matching by polyline is not a real identity check. Live traffic routing could return a different polyline
    var selectedIndex by remember(routeOptions) {
        val matchedIndex = routeOptions.indexOfFirst { it.encodedPolyline == initiallySelectedPolyline }
        mutableIntStateOf(if (matchedIndex >= 0) matchedIndex else 0)
        }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Pick your road") }) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    CircularProgressIndicator(Modifier.padding(32.dp))
                }
            } else if (routeOptions.isEmpty()) {
                Text(
                    "Couldn't find any routes between those addresses.",
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    SegmentedButton(
                        selected = !isCustomizing,
                        onClick = { onModeChange(false) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) { Text("Browse") }
                    SegmentedButton(
                        selected = isCustomizing,
                        onClick = { onModeChange(true) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) { Text("Customize") }
                }

                val selected = routeOptions[selectedIndex]
                val allPoints = routeOptions.flatMap { decodeForDisplay(it.encodedPolyline) }
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(
                        allPoints.firstOrNull() ?: LatLng(0.0, 0.0),
                        12f,
                    )
                }

                GoogleMap(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    cameraPositionState = cameraPositionState,
                    onMapClick = { latLng ->
                        if (isCustomizing) onAddStop(latLng.latitude, latLng.longitude)
                    },
                ) {
                    routeOptions.forEachIndexed { index, option ->
                        if (isCustomizing && index != selectedIndex) return@forEachIndexed
                        val polylineToShow = if (isCustomizing && customRoute != null) {
                                customRoute.encodedPolyline
                            } else {
                                option.encodedPolyline
                            }
                        val points = decodeForDisplay(polylineToShow)
                        Polyline(
                            points = points,
                            color = if (index == selectedIndex) Color(0xFF1E88E5) else Color(0xFFB0BEC5),
                            width = if (index == selectedIndex) 12f else 8f,
                            clickable = !isCustomizing,
                            onClick = { selectedIndex = index },
                        )
                    }

                    if (isCustomizing) {
                        stops.forEachIndexed { index, stop ->
                            key(index) {
                                val markerState = rememberMarkerState(position = LatLng(stop.first, stop.second))
                                LaunchedEffect(markerState.position) {
                                    val pos = markerState.position
                                    if (pos.latitude != stop.first || pos.longitude != stop.second) {
                                        onMoveStop(index, pos.latitude, pos.longitude)
                                    }
                                }
                                Marker(
                                    state = markerState,
                                    draggable = true,
                                    title = "Stop ${index + 1}",
                                )
                            }
                        }
                    }
                }

                if (!isCustomizing) {
                    LazyColumn(Modifier.fillMaxWidth().weight(0.6f)) {
                        items(routeOptions) { option ->
                            val index = routeOptions.indexOf(option)
                            RouteOptionCard(
                                option = option,
                                isSelected = index == selectedIndex,
                                onClick = { selectedIndex = index },
                            )
                        }
                    }
                } else {
                    Column(Modifier.fillMaxWidth().weight(0.6f).padding(12.dp)) {
                        when {
                            isRecomputing -> Text(
                                "Recalculating route...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            customRoute != null -> Text(
                                "${customRoute.distanceText} - about ${customRoute.durationMinutes} min right now",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            else -> Text(
                                "Tap the road to add a stop, drag a stop to move it. ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        ) {
                            stops.forEachIndexed { index, _ ->
                                if (index > 0) Spacer(Modifier.width(8.dp))
                                Row(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("Stop ${index + 1}", style = MaterialTheme.typography.bodySmall)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "×",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.clickable { onRemoveStop(index) },
                                    )
                                }
                            }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth().padding(16.dp)) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            // User placed stops are better pinning points than polyline derived guesses parseAlternatives would attach
                            // Keep the real ones when confirming customized route
                            val isCustom = isCustomizing && customRoute != null
                            val toConfirm = if (isCustom) {
                                customRoute.copy(
                                    waypoints = stops,
                                    summary = "Custom route (${stops.size} stop${if (stops.size == 1) "" else "s"})",
                                )
                            } else {
                                selected
                        }
                        onConfirm(toConfirm, isCustom) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Use this road")
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteOptionCard(option: RouteOption, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
        onClick = onClick,
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(option.summary, style = MaterialTheme.typography.titleSmall)
            Text(
                "${option.distanceText} - about ${option.durationMinutes} min right now",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

// Decodes a polyline for display on the map
// Same algorithm as the decoder inside RoutesApiClient
private fun decodeForDisplay(encoded: String): List<LatLng> {
    val poly = mutableListOf<LatLng>()
    var index = 0
    var lat = 0
    var lng = 0
    while (index < encoded.length) {
        var shift = 0
        var result = 0
        var b: Int
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1

        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        lng += if (result and 1 != 0) (result shr 1).inv() else result shr 1

        poly.add(LatLng(lat / 1E5, lng / 1E5))
    }
    return poly
}
package com.routewatcher.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.routewatcher.app.network.RouteOption

// Lets the user pick their route of choice (similar to Google Maps)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePickerScreen(
    routeOptions: List<RouteOption>,
    isLoading: Boolean,
    initiallySelectedPolyline: String?,
    onConfirm: (RouteOption) -> Unit,
    onCancel: () -> Unit,
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
                ) {
                    routeOptions.forEachIndexed { index, option ->
                        val points = decodeForDisplay(option.encodedPolyline)
                        Polyline(
                            points = points,
                            color = if (index == selectedIndex) Color(0xFF1E88E5) else Color(0xFFB0BEC5),
                            width = if (index == selectedIndex) 12f else 8f,
                            clickable = true,
                            onClick = { selectedIndex = index },
                        )
                    }
                }

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

                Row(Modifier.fillMaxWidth().padding(16.dp)) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onConfirm(selected) },
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
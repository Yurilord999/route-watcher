package com.routewatcher.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import com.routewatcher.app.data.RouteEntity
import com.routewatcher.app.network.RoutesApiClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapEffect

// A small preview of a routes saved polyline, shown in expanded route list
// Reuses Maps SDK compose library already used in RoutePickerScreen
// No network call of its own (just renders an already decoded polyline locally)
@Composable
fun RouteMapSnapshot(route: RouteEntity, modifier: Modifier = Modifier, onMovingChanged: (Boolean) -> Unit) {
    val polyline = route.lockedRoutePolyline
    if (polyline.isNullOrBlank()) {
        EmptyMapPlaceholder(modifier)
        return
    }

    val points = remember(polyline) {
        RoutesApiClient.decodePolyline(polyline).map { (lat, lng) -> LatLng(lat, lng) }
    }
    if (points.isEmpty()) {
        EmptyMapPlaceholder(modifier)
        return
    }

    val bounds = remember(points) {
        val builder = LatLngBounds.Builder()
        points.forEach { builder.include(it) }
        builder.build()
    }

    val cameraPositionState = rememberCameraPositionState()
    var mapLoaded by remember(polyline) { mutableStateOf(false) }
    var trafficEnabled by remember(polyline) { mutableStateOf(true) }

    // newLatLngBounds needs the map already measured, or it throws / no-ops
    LaunchedEffect(mapLoaded) {
        if (mapLoaded) {
            cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 48))
        }
    }

    Box(
        modifier = modifier.pointerInput(Unit ) {
            // Watches for a finger scroll on just this map
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                onMovingChanged(true)
                waitForUpOrCancellation()
                onMovingChanged(false)
            }
        },
    ) {
        GoogleMap(
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
            cameraPositionState = cameraPositionState,
            onMapLoaded = { mapLoaded = true },
            properties = MapProperties(isTrafficEnabled = true),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                scrollGesturesEnabled = true,
                zoomGesturesEnabled = true,
                tiltGesturesEnabled = false,
                rotationGesturesEnabled = true,
                myLocationButtonEnabled = false,
                compassEnabled = true,
            ),
        ) {
            // MapProperties alone isn't reliable at reapplying isTrafficEnabled
            // This sets it directly on the real native map object instead
            // Warning! May break code in the future?
            MapEffect(trafficEnabled) { map ->
                map.isTrafficEnabled = trafficEnabled
            }

            //Double line for increased visibility
            Polyline(points = points, color = Color.White, width = 14f)
            Polyline(points = points, color = MaterialTheme.colorScheme.primary, width = 7f)
        }
        TrafficToggleButton(
            enabled = trafficEnabled,
            onToggle = { trafficEnabled = !trafficEnabled },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
        )
    }
}

@Composable
private fun TrafficToggleButton(enabled: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (enabled) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.55f))
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Text("\uD83D\uDEA6", fontSize = 18.sp)
    }
}

@Composable
private fun EmptyMapPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
    )
}
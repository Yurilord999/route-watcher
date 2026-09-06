package com.routewatcher.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.routewatcher.app.data.RouteEntity
import com.routewatcher.app.network.RoutesApiClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

// A small preview of a routes saved polyline, shown in expanded route list
// Reuses Maps SDK compose library already used in RoutePickerScreen
// No network call of its own (just renders an already decoded polyline locally)
@Composable
fun RouteMapSnapshot(route: RouteEntity, modifier: Modifier = Modifier) {
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

    // newLatLngBounds needs the map already measured, or it throws / no-ops
    LaunchedEffect(mapLoaded) {
        if (mapLoaded) {
            cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 48))
        }
    }

    GoogleMap(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp)),
        cameraPositionState = cameraPositionState,
        onMapLoaded = { mapLoaded = true },
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
        Polyline(points = points, color = MaterialTheme.colorScheme.primary, width = 6f)
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
package com.routewatcher.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.routewatcher.app.R
import com.routewatcher.app.network.RouteOption
import com.routewatcher.app.network.RoutesApiClient

// ---- stops/waypoints editor ----
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopsEditorScreen(
    baseRoutePolyline: String?,
    stops: List<Pair<Double, Double>>,
    onAddStop: (Double, Double) -> Unit,
    onMoveStop: (Int, Double, Double) -> Unit,
    onRemoveStop: (Int) -> Unit,
    customRoute: RouteOption?,
    isRecomputing: Boolean,
    onConfirm: (RouteOption) -> Unit,
    onCancel: () -> Unit,
) {
    // Shows custom route once stops produce one
    val displayPolyline = customRoute?.encodedPolyline ?: baseRoutePolyline
    val points = displayPolyline?.let {
        RoutesApiClient.decodePolyline(it).map { (lat, lng) -> LatLng(lat, lng) }
    } ?: emptyList()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.route_form_add_stops)) }) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(points.firstOrNull() ?: LatLng(0.0, 0.0), 13f)
            }
            GoogleMap(
                modifier = Modifier.fillMaxWidth().weight(1f),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng -> onAddStop(latLng.latitude, latLng.longitude) },
            ) {
                if (points.isNotEmpty()) {
                    Polyline(points = points, color = Color(0xFF1E88E5), width = 12f)
                }
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
                            title = stringResource(R.string.stop_label, index + 1),
                        )
                    }
                }
            }

            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                when {
                    isRecomputing -> Text(
                        stringResource(R.string.recalculating_route),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    customRoute != null -> Text(
                        stringResource(R.string.route_eta_summary, customRoute.distanceText, customRoute.durationMinutes),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    else -> Text(
                        stringResource(R.string.customize_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    stops.forEachIndexed { index, _ ->
                        if (index > 0) Spacer(Modifier.width(8.dp))
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(R.string.stop_label, index + 1), style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "\u00D7",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.clickable { onRemoveStop(index) },
                            )
                        }
                    }
                }
            }

            val customRouteSummary = pluralStringResource(R.plurals.custom_route_summary, stops.size, stops.size)
            Row(Modifier.fillMaxWidth().padding(16.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        val toConfirm = customRoute?.copy(waypoints = stops, summary = customRouteSummary)
                        if (toConfirm != null) onConfirm(toConfirm)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = customRoute != null,
                ) {
                    Text(stringResource(R.string.use_this_road))
                }
            }
        }
    }
}
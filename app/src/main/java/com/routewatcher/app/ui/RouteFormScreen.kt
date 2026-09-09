package com.routewatcher.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.routewatcher.app.R

private val DRESDEN_HAUPTBAHNHOF = LatLng(51.0405, 13.7325)

// ---- fullscreen add/edit route form: search fields + map ----
@Composable
fun RouteFormScreen(
    origin: String,
    onOriginChange: (String) -> Unit,
    destination: String,
    onDestinationChange: (String) -> Unit,
    onSwap: () -> Unit,
    onMoreOptions: () -> Unit,
    onCancel: () -> Unit,
) {
    var trafficEnabled by remember { mutableStateOf(true) }

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
        )

        // ---- traffic toggle (reused from the minimap) ----
        TrafficToggleButton(
            enabled = trafficEnabled,
            onToggle = { trafficEnabled = !trafficEnabled },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 128.dp, end = 8.dp),
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
                    modifier = Modifier.weight(1f),
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
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "\u21C5",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .clickable(onClick = onSwap)
                        .padding(8.dp),
                )
            }
        }
    }
}
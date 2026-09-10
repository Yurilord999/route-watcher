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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
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

private val DRESDEN_HAUPTBAHNHOF = LatLng(51.0405, 13.7325)

// Autocomplete suggestion row (placeholder shape for now)
data class AddressPrediction(
    val primaryText: String,
    val secondaryText: String,
)

// Route alternative shown on the map
// TODO: fake data for now, no real API call yet
data class FakeRouteAlternative(
    val option: RouteOption,
    val points: List<LatLng>,
)

// ---- fullscreen add/edit route form: search fields + map ----
// Bare state: just the map. Full state: includes draggable bottom sheet.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteFormScreen(
    origin: String,
    onOriginChange: (String) -> Unit,
    destination: String,
    onDestinationChange: (String) -> Unit,
    predictions: List<AddressPrediction>,
    onOriginPredictionSelected: (AddressPrediction) -> Unit,
    onDestinationPredictionSelected: (AddressPrediction) -> Unit,
    onSwap: () -> Unit,
    onMoreOptions: () -> Unit,
    alternatives: List<FakeRouteAlternative>,
    selectedAlternative: Int?,
    onAlternativeSelected: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    val mapArea = @Composable {
        RouteFormMapArea(
            origin = origin,
            onOriginChange = onOriginChange,
            destination = destination,
            onDestinationChange = onDestinationChange,
            predictions = predictions,
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

    if (alternatives.isEmpty()) {
        mapArea()
    } else {
        val sheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
        val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = 120.dp,
            sheetContent = {
                // TODO: Route summary, stops and the rest of the form goes here
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Sheet content coming soon")
                }
            },
        ) {
            mapArea()
        }
    }
}

// Map, address fields and everything floating on top is shared by bare & full state
@Composable
private fun RouteFormMapArea(
    origin: String,
    onOriginChange: (String) -> Unit,
    destination: String,
    onDestinationChange: (String) -> Unit,
    predictions: List<AddressPrediction>,
    onOriginPredictionSelected: (AddressPrediction) -> Unit,
    onDestinationPredictionSelected: (AddressPrediction) -> Unit,
    onSwap: () -> Unit,
    onMoreOptions: () -> Unit,
    alternatives: List<FakeRouteAlternative>,
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
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color(0xFFB0BEC5)
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

            if (originFocused && predictions.isNotEmpty()) {
                PredictionsList(predictions, onClick = {
                    onOriginPredictionSelected(it)
                    focusManager.clearFocus()
                })
            } else if (destinationFocused && predictions.isNotEmpty()) {
                PredictionsList(predictions, onClick = {
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
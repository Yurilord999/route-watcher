package com.routewatcher.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.routewatcher.app.R
import com.routewatcher.app.data.RouteEntity
import com.routewatcher.app.network.TrafficErrorCode
import com.routewatcher.app.network.errorMessageRes
import com.routewatcher.app.viewmodel.RouteCheckStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteListScreen(
    routes: List<RouteEntity>,
    onAddRoute: () -> Unit,
    onEditRoute: (RouteEntity) -> Unit,
    onToggleRoute: (RouteEntity, Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    checkStatuses: Map<Long, RouteCheckStatus>,
    onCheckNow: (RouteEntity) -> Unit,
    onUpdateActiveDays: (RouteEntity, Int) -> Unit,
    onDeleteRoute: (RouteEntity) -> Unit,
    pendingExpandRouteId: Long?,
    onPendingExpandConsumed: () -> Unit,
){

    var expandedRouteIds by remember { mutableStateOf(setOf<Long>()) }
    // Routes which minimap is currently mid-interaction (keeps the route list scrollable)
    var movingMapRouteIds by remember { mutableStateOf(setOf<Long>()) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-expands + scrolls to a route with a delay upon checking
    LaunchedEffect(pendingExpandRouteId) {
        val routeId = pendingExpandRouteId ?: return@LaunchedEffect
        expandedRouteIds = expandedRouteIds + routeId
        scrollToRoute(routes, listState, routeId)
        onPendingExpandConsumed()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name))},
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_title)
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (expandedRouteIds.isEmpty()) {
                FloatingActionButton(onClick = onAddRoute) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add_route),
                    )
                }
            }
        }
    ) { padding ->
        if (routes.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.no_routes_yet), textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                state = listState,
                userScrollEnabled = movingMapRouteIds.isEmpty(),
            ) {
                items(routes, key = { it.id }) { route ->
                    RouteRow(
                        route = route,
                        checkStatus = checkStatuses[route.id],
                        onCheckNow = { onCheckNow(route) },
                        onOpenSettings = onOpenSettings,
                        onEditRoute = onEditRoute,
                        onToggleRoute = onToggleRoute,
                        onUpdateActiveDays = onUpdateActiveDays,
                        onDeleteRoute = {  toDelete ->
                            expandedRouteIds = expandedRouteIds - toDelete.id
                            movingMapRouteIds = movingMapRouteIds - toDelete.id
                            onDeleteRoute(toDelete)
                        },
                        expanded = expandedRouteIds.contains(route.id),
                        onExpandedChanged = { isExpanded ->
                            expandedRouteIds = if (isExpanded) {
                                expandedRouteIds + route.id
                            } else {
                                expandedRouteIds - route.id
                            }
                            if (isExpanded) {
                                coroutineScope.launch { scrollToRoute(routes, listState, route.id) }
                            }
                        },
                        onMapMovingChanged = { isMoving ->
                            movingMapRouteIds = if (isMoving) {
                                movingMapRouteIds + route.id
                            } else {
                                movingMapRouteIds - route.id
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

// Scrolls the list to a routes current position
private suspend fun scrollToRoute(routes: List<RouteEntity>, listState: LazyListState, routeId: Long) {
    val index = routes.indexOfFirst { it.id == routeId }
    if (index >= 0) {
        listState.animateScrollToItem(index)
    }
}

// ---- day of week display order ----
// Bit values must match RouteEntity constants (shared with AlarmScheduler.dayBitFor)
@Composable
private fun daysForDisplay(): List<Pair<String, Int>> = listOf(
    stringResource(R.string.day_abbrev_monday) to RouteEntity.MONDAY,
    stringResource(R.string.day_abbrev_tuesday) to RouteEntity.TUESDAY,
    stringResource(R.string.day_abbrev_wednesday) to RouteEntity.WEDNESDAY,
    stringResource(R.string.day_abbrev_thursday) to RouteEntity.THURSDAY,
    stringResource(R.string.day_abbrev_friday) to RouteEntity.FRIDAY,
    stringResource(R.string.day_abbrev_saturday) to RouteEntity.SATURDAY,
    stringResource(R.string.day_abbrev_sunday) to RouteEntity.SUNDAY,
)

// ---- single expandable route row ----
@Composable
private fun RouteRow(
    route: RouteEntity,
    checkStatus: RouteCheckStatus?,
    onCheckNow: () -> Unit,
    onOpenSettings: () -> Unit,
    onEditRoute: (RouteEntity) -> Unit,
    onToggleRoute: (RouteEntity, Boolean) -> Unit,
    onUpdateActiveDays: (RouteEntity, Int) -> Unit,
    onDeleteRoute: (RouteEntity) -> Unit,
    expanded: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
    onMapMovingChanged: (Boolean) -> Unit,
) {

    var activeDays by remember(route.id) { mutableStateOf(route.activeDays) }
    var confirmingDelete by remember(route.id) { mutableStateOf(false) }
    val context = LocalContext.current

    Column {
        // ---- check-now result banner ----
        AnimatedVisibility(
            visible = checkStatus is RouteCheckStatus.Done,
            enter = expandVertically() + fadeIn(),
        ) {
            val result = (checkStatus as? RouteCheckStatus.Done)?.result
            if (result != null) {
                val isJam = result.success && result.delayMinutes >= route.delayThresholdMinutes
                val isKeyMissing = !result.success && result.errorCode == TrafficErrorCode.NO_API_KEY
                val (bg, fg) = when {
                    !result.success -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                    isJam -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                    else -> Color(0xFF2E7D32) to Color.White
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bg)
                        .then(if (isKeyMissing) Modifier.clickable { onOpenSettings() } else Modifier)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = if (result.success) {
                            if (isJam) {
                                stringResource(R.string.check_result_jam, result.delayMinutes)
                            } else {
                                stringResource(R.string.check_result_clear, result.trafficDurationMinutes)
                            }
                        } else {
                            stringResource(errorMessageRes(result.errorCode))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = fg,
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            // ---- name, addresses, check-now button, enabled switch ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onEditRoute(route) },
                ) {
                    Text(route.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${route.originAddress} -> ${route.destinationAddress}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (checkStatus is RouteCheckStatus.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 12.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    IconButton(onClick = onCheckNow) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.check_now))
                    }
                }
                Switch(
                    checked = route.enabled,
                    onCheckedChange = { onToggleRoute(route, it) },
                )
            }
            Spacer(Modifier.height(6.dp))

            if (!expanded) {
                // ---- collapsed row: small dots + expand arrow ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onExpandedChanged(true)
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        daysForDisplay().forEach { (label, bit) ->
                            DayDot(label = label.take(1), active = (activeDays and bit) != 0)
                        }
                    }
                    Text("\u25BE", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                // ---- expanded panel: large tappable circles, minimap, delete + collapse ----
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    daysForDisplay().forEach { (label, bit) ->
                        DayCircle(
                            label = label,
                            active = (activeDays and bit) != 0,
                            onClick = {
                                activeDays = activeDays xor bit
                                onUpdateActiveDays(route, activeDays)
                            },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                // minimap
                RouteMapSnapshot(
                    route = route,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    onMovingChanged = onMapMovingChanged,
                )
                Spacer(Modifier.height(12.dp))

                if (!confirmingDelete) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { confirmingDelete = true }) {
                                Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.delete_route), color = MaterialTheme.colorScheme.error)
                            }
                            TextButton(onClick = { openInGoogleMaps(context, route) }) {
                                Text(stringResource(R.string.open_in_maps))
                            }
                        }
                        IconButton(onClick = {
                            onExpandedChanged(false)
                        }) {
                            Text("\u25B4", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.delete_route_confirm_title), style = MaterialTheme.typography.bodyMedium)
                        Row {
                            TextButton(onClick = { confirmingDelete = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                            TextButton(onClick = { onDeleteRoute(route) }) {
                                Text(stringResource(R.string.delete_route), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Opens Google Maps for directions between this routes origin & destination
private fun openInGoogleMaps(context: Context, route: RouteEntity) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(route.directionsUrl()))
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Nothing installed that can open a link at all (no browser either) - nothing to do.
    }
}
// ---- day circle styles: small read only dots + large tappable toggle ----

@Composable
private fun DayDot(label: String, active: Boolean) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DayCircle(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .border(
                width = 0.5.dp,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
package com.routewatcher.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.routewatcher.app.alarm.AlarmScheduler
import com.routewatcher.app.data.RouteDao
import com.routewatcher.app.data.RouteEntity
import com.routewatcher.app.data.SettingsStore
import com.routewatcher.app.network.DistanceMatrixClient
import com.routewatcher.app.network.RouteOption
import com.routewatcher.app.network.RoutesApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Screen the app is currently showing + the composable that switches between them
private sealed class Screen {
    data object List : Screen()
    data class AddEdit(val route: RouteEntity?) : Screen()
    data object Settings : Screen()
    data class PickRoad(val origin: String, val destination: String) : Screen()
}

// Top level screen router
@Composable
fun RouteWatcherApp(
    dao: RouteDao,
    settingsStore: SettingsStore,
    onRequestExactAlarmPermission: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf<Screen>(Screen.List) }
    val routes by dao.observeAll().collectAsState(initial = emptyList())
    var apiKey by remember { mutableStateOf(settingsStore.getApiKey()) }
    var testResult by remember { mutableStateOf<String?>(null) }

    when (val currentScreen = screen) {
        is Screen.List -> RouteListScreen(
            routes = routes,
            onAddRoute = { screen = Screen.AddEdit(null) },
            onEditRoute = { screen = Screen.AddEdit(it) },
            onToggleRoute = { route, enabled ->
                scope.launch(Dispatchers.IO) {
                    val updated = route.copy(enabled = enabled)
                    dao.upsert(updated)
                    if (enabled) {
                        withContext(Dispatchers.Main) { onRequestExactAlarmPermission() }
                        AlarmScheduler.scheduleAllForRoute(context, updated)
                    } else {
                        AlarmScheduler.cancelAllForRoute(context, updated.id)
                    }
                }
            },
            onOpenSettings = { screen = Screen.Settings },
        )
        is Screen.AddEdit -> AddEditRouteScreen(
            existing = currentScreen.route,
            onSave = { route ->
                scope.launch(Dispatchers.IO) {
                    val id = dao.upsert(route)
                    val saved = route.copy(id = if (route.id == 0L) id else route.id)
                    AlarmScheduler.scheduleAllForRoute(context, saved)
                }
                screen = Screen.List
            },
            onDelete = currentScreen.route?.let { existing ->
                {
                    AlarmScheduler.cancelAllForRoute(context, existing.id)
                    scope.launch(Dispatchers.IO) { dao.delete(existing) }
                    screen = Screen.List
                }
            },
            onPickRoad = { origin, destination ->
                screen = Screen.PickRoad(origin, destination)
            },
            onCancel = { screen = Screen.List },
        )
        is Screen.Settings -> SettingsScreen(
            currentKey = apiKey,
            onSaveKey = { key ->
                settingsStore.setApiKey(key)
                apiKey = key
            },
            onClearKey = {
                settingsStore.clearApiKey()
                apiKey = null
            },
            onTestKey = {
                val key = settingsStore.getApiKey()
                scope.launch(Dispatchers.IO) {
                    val result = DistanceMatrixClient.checkTraffic(
                        "Dresden Hauptbahnhof, Dresden",
                        "Frauenkirche Dresden, Dresden",
                        key ?: "",
                    )
                    withContext(Dispatchers.Main) {
                        testResult = if (result.success) {
                            "Key works. Test route: ${result.trafficDurationMinutes} min."
                        } else {
                            "Test failed: ${result.errorMessage}"
                        }
                    }
                }
            },
            testResultMessage = testResult,
            onBack = { screen = Screen.List },
        )
        is Screen.PickRoad -> {
            var routeOptions by remember(currentScreen) { mutableStateOf<List<RouteOption>>(emptyList()) }
            var isLoadingRoutes by remember(currentScreen) { mutableStateOf(true) }

            LaunchedEffect(currentScreen) {
                isLoadingRoutes = true
                val key = settingsStore.getApiKey() ?: ""
                routeOptions = withContext(Dispatchers.IO) {
                    RoutesApiClient.fetchRouteAlternatives(
                        currentScreen.origin,
                        currentScreen.destination,
                        key,
                    )
                }
                isLoadingRoutes = false
            }

            RoutePickerScreen(
                routeOptions = routeOptions,
                isLoading = isLoadingRoutes,
                // TODO: persist the picked road - wiring this next, see ROADMAP.md
                onConfirm = { screen = Screen.List },
                onCancel = { screen = Screen.List },
            )
        }
    }
}
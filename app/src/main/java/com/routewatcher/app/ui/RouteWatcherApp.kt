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
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.viewmodel.compose.viewModel
import com.routewatcher.app.alarm.AlarmScheduler
import com.routewatcher.app.data.RouteDao
import com.routewatcher.app.data.RouteEntity
import com.routewatcher.app.data.SettingsStore
import com.routewatcher.app.network.RouteOption
import com.routewatcher.app.network.RoutesApiClient
import com.routewatcher.app.viewmodel.RouteViewModel
import com.routewatcher.app.viewmodel.RouteViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Screen the app is currently showing + the composable that switches between them
private sealed class Screen {
    data object List : Screen()
    data class AddEdit(val route: RouteEntity?) : Screen()
    data object Settings : Screen()
}
// A request to open the road-picker overlay
private data class PickRoadRequest(val origin: String, val destination: String)

// Top level screen router
@Composable
fun RouteWatcherApp(
    dao: RouteDao,
    settingsStore: SettingsStore,
    onRequestExactAlarmPermission: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: RouteViewModel = viewModel(factory = RouteViewModelFactory(dao, settingsStore))

    var screen by remember { mutableStateOf<Screen>(Screen.List) }
    val routes by viewModel.routes.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val testResult by viewModel.testResult.collectAsState()

    when (val currentScreen = screen) {
        is Screen.List -> RouteListScreen(
            routes = routes,
            onAddRoute = { screen = Screen.AddEdit(null) },
            onEditRoute = { screen = Screen.AddEdit(it) },
            onToggleRoute = { route, enabled ->
                if (enabled) onRequestExactAlarmPermission()
                viewModel.toggleRoute(context, route, enabled)
            },
            onOpenSettings = { screen = Screen.Settings },
        )
        is Screen.AddEdit -> {
            // Scoped to this specific edit session
            var pickedRoute by remember(currentScreen) { mutableStateOf<RouteOption?>(null) }
            var pickRoadRequest by remember(currentScreen) { mutableStateOf<PickRoadRequest?>(null) }

            AddEditRouteScreen(
                existing = currentScreen.route,
                pickedRoute = pickedRoute,
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
                    pickRoadRequest = PickRoadRequest(origin, destination)
                },
                onCancel = { screen = Screen.List },
            )

            // Road picker as a full-screen overlay, not a screen swap
            // TODO: bandaid, in need of a ViewModel (see roadmap)
            pickRoadRequest?.let { request ->
                Dialog(
                    onDismissRequest = { pickRoadRequest = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                ) {
                    Box(Modifier.fillMaxSize()) {
                        var routeOptions by remember(request) { mutableStateOf<List<RouteOption>>(emptyList()) }
                        var isLoadingRoutes by remember(request) { mutableStateOf(true) }

                        LaunchedEffect(request) {
                            isLoadingRoutes = true
                            val key = settingsStore.getApiKey() ?: ""
                            routeOptions = withContext(Dispatchers.IO) {
                                RoutesApiClient.fetchRouteAlternatives(request.origin, request.destination, key)
                            }
                            isLoadingRoutes = false
                        }

                        RoutePickerScreen(
                            routeOptions = routeOptions,
                            isLoading = isLoadingRoutes,
                            initiallySelectedPolyline = pickedRoute?.encodedPolyline
                                ?: currentScreen.route?.lockedRoutePolyline,
                            onConfirm = { picked ->
                                pickedRoute = picked
                                pickRoadRequest = null
                            },
                            onCancel = { pickRoadRequest = null },
                        )
                    }
                }
            }
        }
        is Screen.Settings -> SettingsScreen(
            currentKey = apiKey,
            onSaveKey = { key -> viewModel.saveApiKey(key) },
            onClearKey = { viewModel.clearApiKey() },
            onTestKey = { viewModel.testApiKey() },
            testResultMessage = testResult,
            onBack = { screen = Screen.List },
        )
    }
}
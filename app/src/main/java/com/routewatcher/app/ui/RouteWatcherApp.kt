package com.routewatcher.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.routewatcher.app.data.RouteDao
import com.routewatcher.app.data.SettingsStore
import com.routewatcher.app.viewmodel.RouteViewModel
import com.routewatcher.app.viewmodel.RouteViewModelFactory

// Screen the app is currently showing
private sealed class Screen {
    data object List : Screen()
    data object AddEdit : Screen()
    data object Settings : Screen()
    data object PickRoad : Screen()
}

// Top level screen router
@Composable
fun RouteWatcherApp(
    dao: RouteDao,
    settingsStore: SettingsStore,
    onRequestExactAlarmPermission: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: RouteViewModel = viewModel(factory = RouteViewModelFactory(dao, settingsStore))

    var screen by remember { mutableStateOf<Screen>(Screen.List) }
    val routes by viewModel.routes.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val editState by viewModel.editState.collectAsState()
    val pickerState by viewModel.pickerState.collectAsState()

    when (screen) {
        is Screen.List -> RouteListScreen(
            routes = routes,
            onAddRoute = {
                viewModel.startNewRoute()
                screen = Screen.AddEdit },
            onEditRoute = {
                viewModel.startEditRoute(it)
                screen = Screen.AddEdit },
            onToggleRoute = { route, enabled ->
                if (enabled) onRequestExactAlarmPermission()
                viewModel.toggleRoute(context, route, enabled)
            },
            onOpenSettings = { screen = Screen.Settings },
        )
        is Screen.AddEdit -> editState?.let { state ->
            AddEditRouteScreen(
                name = state.name,
                onNameChange = { viewModel.updateName(it) },
                origin = state.origin,
                onOriginChange = { viewModel.updateOrigin(it) },
                destination = state.destination,
                onDestinationChange = { viewModel.updateDestination(it) },
                hour = state.hour,
                onHourChange = { viewModel.updateHour(it) },
                minute = state.minute,
                onMinuteChange = { viewModel.updateMinute(it) },
                offsets = state.offsets,
                onOffsetsChange = { viewModel.updateOffsets(it) },
                threshold = state.threshold,
                onThresholdChange = { viewModel.updateThreshold(it) },
                lockedRouteSummary = state.lockedRouteSummary,
                isNewRoute = state.isNewRoute,
                onSave = {
                    viewModel.saveEditedRoute(context)
                    screen = Screen.List
                },
                onDelete = {
                    viewModel.deleteEditedRoute(context)
                    screen = Screen.List
                },
                onPickRoad = {
                    viewModel.openRoadPicker()
                    screen = Screen.PickRoad
                },
                onCancel = {
                    viewModel.cancelEdit()
                    screen = Screen.List
                },
            )
        }
        is Screen.Settings -> SettingsScreen(
            currentKey = apiKey,
            onSaveKey = { key -> viewModel.saveApiKey(key) },
            onClearKey = { viewModel.clearApiKey() },
            onTestKey = { viewModel.testApiKey() },
            testResultMessage = testResult,
            onBack = { screen = Screen.List },
            onTestRouteThroughStops = { viewModel.testRouteThroughStops() }
        )
        is Screen.PickRoad -> pickerState?.let { state ->
            RoutePickerScreen(
                routeOptions = state.routeOptions,
                isLoading = state.isLoading,
                initiallySelectedPolyline = editState?.lockedRoutePolyline,
                onConfirm = { picked ->
                    viewModel.confirmPickedRoute(picked)
                    screen = Screen.AddEdit
                },
                onCancel = {
                    viewModel.cancelRoadPicker()
                    screen = Screen.AddEdit
                },
                isCustomizing = state.isCustomizing,
                stops = state.stops,
                onModeChange = { viewModel.setPickerCustomizing(it) },
                onAddStop = { lat, lng -> viewModel.addPickerStop(lat, lng) },
                onMoveStop = { index, lat, lng -> viewModel.movePickerStop(index, lat, lng) },
                onRemoveStop = { viewModel.removePickerStop(it) },
                customRoute = state.customRoute,
                isRecomputing = state.isRecomputing,
            )
        }
    }
}
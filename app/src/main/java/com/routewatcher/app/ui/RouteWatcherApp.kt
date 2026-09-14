package com.routewatcher.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.lifecycle.viewmodel.compose.viewModel
import com.routewatcher.app.data.RouteDao
import com.routewatcher.app.data.SettingsStore
import com.routewatcher.app.viewmodel.RouteViewModel
import com.routewatcher.app.viewmodel.RouteViewModelFactory
import com.routewatcher.app.R

// Screen the app is currently showing
private sealed class Screen {
    data object List : Screen()
    data object Settings : Screen()
    data object Onboarding : Screen()
    data object RouteForm : Screen()
    data object StopsEditor : Screen()
}

// Top level screen router
@Composable
fun RouteWatcherApp(
    dao: RouteDao,
    settingsStore: SettingsStore,
    onRequestExactAlarmPermission: () -> Unit,
    openSettingsOnStart: Boolean = false,
    expandRouteIdOnStart: Long? = null,
) {
    val context = LocalContext.current
    val viewModel: RouteViewModel = viewModel(factory = RouteViewModelFactory(dao, settingsStore))

    var screen by remember {
        mutableStateOf<Screen>(
            when {
                openSettingsOnStart -> Screen.Settings
                !settingsStore.hasSeenOnboarding() -> Screen.Onboarding
                else -> Screen.List
            }
        )
    }
    val routes by viewModel.routes.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val editState by viewModel.editState.collectAsState()
    val checkStatuses by viewModel.checkStatuses.collectAsState()
    val stopsEditorState by viewModel.stopsEditorState.collectAsState()

    var pendingExpandRouteId by remember { mutableStateOf(expandRouteIdOnStart) }
    LaunchedEffect(Unit) {
        viewModel.autoExpandRouteId.collect { routeId -> pendingExpandRouteId = routeId }
    }

    var showApiLimitDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.apiLimitReachedEvents.collect { showApiLimitDialog = true }
    }
    val routesApiUsage by viewModel.routesApiUsage.collectAsState()
    val originPredictions by viewModel.originPredictions.collectAsState()
    val destinationPredictions by viewModel.destinationPredictions.collectAsState()

    // Temporary! Prototype only, favorites live in local compose state for now
    var prototypeFavorites by remember { mutableStateOf(listOf(FavoriteTime(7, 0), FavoriteTime(8, 0))) }

    when (screen) {
        is Screen.List -> RouteListScreen(
            routes = routes,
            onAddRoute = {
                viewModel.startNewRoute()
                screen = Screen.RouteForm },
            onEditRoute = {
                viewModel.startEditRoute(it)
                screen = Screen.RouteForm },
            onToggleRoute = { route, enabled ->
                if (enabled) onRequestExactAlarmPermission()
                viewModel.toggleRoute(context, route, enabled)
            },
            onOpenSettings = {
                viewModel.refreshRoutesApiUsage()
                screen = Screen.Settings
            },
            checkStatuses = checkStatuses,
            onCheckNow = { viewModel.checkRouteNow(it) },
            onUpdateActiveDays = { route, activeDays -> viewModel.updateActiveDays(context, route, activeDays) },
            onDeleteRoute = { viewModel.deleteRoute(context, it) },
            pendingExpandRouteId = pendingExpandRouteId,
            onPendingExpandConsumed = { pendingExpandRouteId = null },
        )
        is Screen.Settings -> SettingsScreen(
            currentKey = apiKey,
            onSaveKey = { key -> viewModel.saveApiKey(key) },
            onClearKey = { viewModel.clearApiKey() },
            onTestKey = { viewModel.testApiKey() },
            testResult = testResult,
            routesApiUsage = routesApiUsage,
            onBack = { screen = Screen.List },
        )

        is Screen.RouteForm -> editState?.let { state -> RouteFormScreen(
            origin = state.origin,
            onOriginChange = {
                viewModel.updateOrigin(it)
                viewModel.searchOriginPredictions(context, it)
            },
            destination = state.destination,
            onDestinationChange = {
                viewModel.updateDestination(it)
                viewModel.searchDestinationPredictions(context, it)
            },
            originPredictions = originPredictions,
            destinationPredictions = destinationPredictions,
            onOriginPredictionSelected = { viewModel.originPredictionSelected(context,it) },
            onDestinationPredictionSelected = { viewModel.destinationPredictionSelected(context,it) },
            onOriginFieldBlurred = { viewModel.confirmOriginManually() },
            onDestinationFieldBlurred = { viewModel.confirmDestinationManually() },
            onSwap = { viewModel.swapOriginDestination() },
            onMoreOptions = {},
            alternatives = state.alternatives,
            selectedAlternative = state.selectedAlternativeIndex,
            onAlternativeSelected = { viewModel.selectRouteAlternative(it) },
            name = state.name,
            onNameChange = { viewModel.updateName(it) },
            hour = state.hour,
            minute = state.minute,
            favoriteTimes = prototypeFavorites,
            onSelectTime = { viewModel.setDepartureTime(it.hour, it.minute) },
            onAddFavoriteTime = { time ->
                if (time !in prototypeFavorites) prototypeFavorites = prototypeFavorites + time
            },
            onDeleteFavoriteTimes = { toDelete ->
                val blocked = toDelete.any { it.hour == state.hour && it.minute == state.minute }
                prototypeFavorites = prototypeFavorites.filterNot { it in toDelete && !(it.hour == state.hour && it.minute == state.minute) }
                blocked
            },
            activeDays = state.activeDays,
            onActiveDaysChange = { viewModel.updateActiveDays(it) },
            offsets = state.offsets,
            onOffsetsChange = { viewModel.updateOffsets(it) },
            threshold = state.threshold,
            onThresholdChange = { viewModel.updateThreshold(it) },
            stopsCount = state.stopsCount,
            onAddStops = {
                viewModel.openStopsEditor()
                screen = Screen.StopsEditor
            },
            isNewRoute = state.isNewRoute,
            onSave = {
                viewModel.saveEditedRoute(context)
                screen = Screen.List
            },
            onDelete = {
                viewModel.deleteEditedRoute(context)
                screen = Screen.List
            },
            onCancel = {
                viewModel.cancelEdit()
                screen = Screen.List
            },
        ) }

        is Screen.StopsEditor -> stopsEditorState?.let { state ->
            StopsEditorScreen(
                baseRoutePolyline = editState?.lockedRoutePolyline,
                stops = state.stops,
                onAddStop = { lat, lng -> viewModel.addStopsEditorStop(lat, lng) },
                onMoveStop = { index, lat, lng -> viewModel.moveStopsEditorStop(index, lat, lng) },
                onRemoveStop = { viewModel.removeStopsEditorStop(it) },
                customRoute = state.customRoute,
                isRecomputing = state.isRecomputing,
                onConfirm = { customRoute ->
                    viewModel.confirmStopsEditorRoute(customRoute)
                    screen = Screen.RouteForm
                },
                onCancel = {
                    viewModel.cancelStopsEditor()
                    screen = Screen.RouteForm
                },
            )
        }

        is Screen.Onboarding -> OnboardingScreen(
            onSaveKey = { key -> viewModel.saveApiKey(key) },
            onFinished = {
                settingsStore.setHasSeenOnboarding(true)
                screen = Screen.List
            },
        )
    }
    if (showApiLimitDialog) {
        AlertDialog(
            onDismissRequest = { showApiLimitDialog = false },
            confirmButton = {
                TextButton(onClick = { showApiLimitDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            },
            text = { Text(stringResource(R.string.error_api_limit_reached)) },
        )
    }
}
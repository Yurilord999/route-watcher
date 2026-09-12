package com.routewatcher.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.routewatcher.app.data.RouteDao
import com.routewatcher.app.data.SettingsStore
import com.routewatcher.app.data.RouteEntity
import com.routewatcher.app.viewmodel.RouteViewModel
import com.routewatcher.app.viewmodel.RouteViewModelFactory

// Screen the app is currently showing
private sealed class Screen {
    data object List : Screen()
    data object AddEdit : Screen()
    data object Settings : Screen()
    data object PickRoad : Screen()
    data object Onboarding : Screen()
    //Temporary! (new add/edit route form testing)
    data object RouteForm : Screen()
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
    val pickerState by viewModel.pickerState.collectAsState()
    val checkStatuses by viewModel.checkStatuses.collectAsState()

    var pendingExpandRouteId by remember { mutableStateOf(expandRouteIdOnStart) }
    LaunchedEffect(Unit) {
        viewModel.autoExpandRouteId.collect { routeId -> pendingExpandRouteId = routeId }
    }

    //Temporary! (new add/edit route form testing)
    var routeFormOrigin by remember { mutableStateOf("") }
    var routeFormDestination by remember { mutableStateOf("") }
    var originResolved by remember { mutableStateOf(false) }
    var destinationResolved by remember { mutableStateOf(false) }
    val originPredictions by viewModel.originPredictions.collectAsState()
    val destinationPredictions by viewModel.destinationPredictions.collectAsState()

    var routeFormSelectedAlt by remember { mutableStateOf<Int?>(null) }
    val routeFormAlternatives by viewModel.routeFormAlternatives.collectAsState()
    LaunchedEffect(routeFormAlternatives) {
        routeFormSelectedAlt = if (routeFormAlternatives.isNotEmpty()) {
            routeFormAlternatives.indices.minByOrNull { routeFormAlternatives[it].option.durationMinutes }
        } else {
            null
        }
    }
    LaunchedEffect(originResolved, destinationResolved, routeFormOrigin, routeFormDestination) {
        if (originResolved && destinationResolved) {
            viewModel.fetchRouteFormAlternatives(routeFormOrigin, routeFormDestination)
        } else {
            viewModel.clearRouteFormAlternatives()
        }
    }

    //Temporary! New add/edit form testing
    var routeFormName by remember { mutableStateOf("") }
    var routeFormHour by remember { mutableStateOf("8") }
    var routeFormMinute by remember { mutableStateOf("0") }
    var routeFormActiveDays by remember { mutableStateOf(RouteEntity.dayBitFor()) }
    var routeFormOffsets by remember { mutableStateOf("30") }
    var routeFormThreshold by remember { mutableStateOf("10") }
    var routeFormStopsCount by remember { mutableStateOf(0) }

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
            checkStatuses = checkStatuses,
            onCheckNow = { viewModel.checkRouteNow(it) },
            onUpdateActiveDays = { route, activeDays -> viewModel.updateActiveDays(context, route, activeDays) },
            onDeleteRoute = { viewModel.deleteRoute(context, it) },
            pendingExpandRouteId = pendingExpandRouteId,
            onPendingExpandConsumed = { pendingExpandRouteId = null },
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
                activeDays = state.activeDays,
                onActiveDaysChange = { viewModel.updateActiveDays(it) },
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
            testResult = testResult,
            onBack = { screen = Screen.List },
            onPreviewRouteForm = { screen = Screen.RouteForm },
        )

        //Temporary! (new add/edit route form testing)
        is Screen.RouteForm -> RouteFormScreen(
            origin = routeFormOrigin,
            onOriginChange = {
                originResolved = false
                routeFormOrigin = it
                viewModel.searchOriginPredictions(context, it)
            },
            destination = routeFormDestination,
            onDestinationChange = {
                routeFormDestination = it
                destinationResolved = false
                viewModel.searchDestinationPredictions(context, it)
            },
            originPredictions = originPredictions,
            destinationPredictions = destinationPredictions,
            onOriginPredictionSelected = {
                routeFormOrigin = it.fullText
                originResolved = true
                viewModel.originPredictionSelected()
            },
            onDestinationPredictionSelected = {
                routeFormDestination = it.fullText
                destinationResolved = true
                viewModel.destinationPredictionSelected()
            },
            onSwap = {
                val tmp = routeFormOrigin
                routeFormOrigin = routeFormDestination
                routeFormDestination = tmp
            },
            onMoreOptions = {},
            alternatives = routeFormAlternatives,
            selectedAlternative = routeFormSelectedAlt,
            onAlternativeSelected = { routeFormSelectedAlt = it },
            name = routeFormName,
            onNameChange = { routeFormName = it },
            hour = routeFormHour,
            onHourChange = { routeFormHour = it },
            minute = routeFormMinute,
            onMinuteChange = { routeFormMinute = it },
            activeDays = routeFormActiveDays,
            onActiveDaysChange = { routeFormActiveDays = it },
            offsets = routeFormOffsets,
            onOffsetsChange = { routeFormOffsets = it },
            threshold = routeFormThreshold,
            onThresholdChange = { routeFormThreshold = it },
            stopsCount = routeFormStopsCount,
            onAddStops = {},
            isNewRoute = true,
            onSave = {},
            onDelete = {},
            onCancel = { screen = Screen.Settings },
        )

        is Screen.Onboarding -> OnboardingScreen(
            onSaveKey = { key -> viewModel.saveApiKey(key) },
            onFinished = {
                settingsStore.setHasSeenOnboarding(true)
                screen = Screen.List
            },
        )
        is Screen.PickRoad -> pickerState?.let { state ->
            RoutePickerScreen(
                routeOptions = state.routeOptions,
                isLoading = state.isLoading,
                initiallySelectedPolyline = editState?.lockedRoutePolyline,
                onConfirm = { picked, isCustom ->
                    viewModel.confirmPickedRoute(picked, isCustom)
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
                missingApiKey = state.missingApiKey,
                onGoToSettings = {
                    viewModel.cancelRoadPicker()
                    screen = Screen.Settings
                },
            )
        }
    }
}
package com.routewatcher.app.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routewatcher.app.data.RouteDao
import com.routewatcher.app.data.RouteEntity
import com.routewatcher.app.data.SettingsStore
import com.routewatcher.app.alarm.AlarmScheduler
import com.routewatcher.app.network.RoutesApiClient
import com.routewatcher.app.network.RouteOption
import com.routewatcher.app.network.TrafficErrorCode
import com.routewatcher.app.network.TrafficResult
import com.routewatcher.app.network.AddressPrediction
import com.routewatcher.app.network.RouteAlternative
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.android.gms.tasks.Task
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest


// Data outcome of the "Test key now" button (SettingsScreen builds display text from this)
data class ApiKeyTestResult(
    val success: Boolean,
    val durationMinutes: Int = 0,
    val errorCode: TrafficErrorCode? = null,
)

// Per route "check now" status. Shown inline in RouteListScreen (session only)
sealed class RouteCheckStatus {
    object Loading : RouteCheckStatus()
    data class Done(val result: TrafficResult) : RouteCheckStatus()
}

// Single shared ViewModel for the whole app (screen navigation, list, add/edit, picker state, settings)
class RouteViewModel(
    private val dao: RouteDao,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    val routes: StateFlow<List<RouteEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _apiKey = MutableStateFlow(settingsStore.getApiKey())
    val apiKey: StateFlow<String?> = _apiKey.asStateFlow()

    private val _testResult = MutableStateFlow<ApiKeyTestResult?>(null)
    val testResult: StateFlow<ApiKeyTestResult?> = _testResult.asStateFlow()

    private val _checkStatuses = MutableStateFlow<Map<Long, RouteCheckStatus>>(emptyMap())
    val checkStatuses: StateFlow<Map<Long, RouteCheckStatus>> = _checkStatuses.asStateFlow()

    private val _autoExpandRouteId = MutableSharedFlow<Long>()
    val autoExpandRouteId: SharedFlow<Long> = _autoExpandRouteId.asSharedFlow()

    // Instant check (in app)
    fun checkRouteNow(route: RouteEntity) {
        _checkStatuses.value = _checkStatuses.value + (route.id to RouteCheckStatus.Loading)
        viewModelScope.launch(Dispatchers.IO) {
            val result = RoutesApiClient.checkTrafficOnRoute(
                route.originAddress,
                route.destinationAddress,
                route.lockedWaypointsList(),
                settingsStore.getApiKey() ?: "",
            )
            _checkStatuses.value = _checkStatuses.value + (route.id to RouteCheckStatus.Done(result))
            if (result.success && result.delayMinutes >= route.delayThresholdMinutes) {
                _autoExpandRouteId.emit(route.id)
            }
        }
    }

    // Context is passed in per call, rather than held in the ViewModel
    // This avoids holding a reference which could outlive the activity
    fun toggleRoute(context: Context, route: RouteEntity, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = route.copy(enabled = enabled)
            dao.upsert(updated)
            if (enabled) {
                AlarmScheduler.scheduleAllForRoute(context, updated)
            } else {
                AlarmScheduler.cancelAllForRoute(context, updated.id)
            }
        }
    }

    // scheduleAllForRoute cancels & reschedules internally,
    // no-ops after canceling if the route is disabled
    fun updateActiveDays(context: Context, route: RouteEntity, activeDays: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = route.copy(activeDays = activeDays)
            dao.upsert(updated)
            AlarmScheduler.scheduleAllForRoute(context, updated)
        }
    }

    fun deleteRoute(context: Context, route: RouteEntity) {
        AlarmScheduler.cancelAllForRoute(context, route.id)
        viewModelScope.launch(Dispatchers.IO) { dao.delete(route) }
    }

    fun saveApiKey(key: String) {
        settingsStore.setApiKey(key)
        _apiKey.value = key
    }

    fun clearApiKey() {
        settingsStore.clearApiKey()
        _apiKey.value = null
    }

    fun testApiKey() {
        val key = _apiKey.value
        viewModelScope.launch(Dispatchers.IO) {
            val result = RoutesApiClient.checkTrafficOnRoute(
                "Dresden Hauptbahnhof, Dresden",
                "Frauenkirche Dresden, Dresden",
                emptyList(),
                key ?: "",
            )
            _testResult.value = ApiKeyTestResult(
                success = result.success,
                durationMinutes = result.trafficDurationMinutes,
                errorCode = result.errorCode,
            )
        }
    }

    private val _editState = MutableStateFlow<RouteEditState?>(null)
    val editState: StateFlow<RouteEditState?> = _editState.asStateFlow()

    private val _pickerState = MutableStateFlow<RoutePickerState?>(null)
    val pickerState: StateFlow<RoutePickerState?> = _pickerState.asStateFlow()

    fun startNewRoute() {
        _editState.value = RouteEditState()
    }

    fun startEditRoute(route: RouteEntity) {
        _editState.value = RouteEditState.from(route)
    }

    private fun updateEditState(transform: (RouteEditState) -> RouteEditState) {
        _editState.value = _editState.value?.let(transform)
    }

    fun updateName(value: String) = updateEditState { it.copy(name = value) }
    fun updateOrigin(value: String) = updateEditState { it.copy(origin = value) }
    fun updateDestination(value: String) = updateEditState { it.copy(destination = value) }
    fun updateHour(value: String) = updateEditState { it.copy(hour = value) }
    fun updateMinute(value: String) = updateEditState { it.copy(minute = value) }
    fun updateOffsets(value: String) = updateEditState { it.copy(offsets = value) }
    fun updateThreshold(value: String) = updateEditState { it.copy(threshold = value) }
    fun updateActiveDays(value: Int) = updateEditState { it.copy(activeDays = value) }

    fun cancelEdit() {
        _editState.value = null
        _pickerState.value = null
    }

    fun saveEditedRoute(context: Context) {
        val state = _editState.value ?: return
        val route = RouteEntity(
            id = state.id,
            name = state.name.ifBlank { "Route" },
            originAddress = state.origin,
            destinationAddress = state.destination,
            departureHour = state.hour.toIntOrNull()?.coerceIn(0, 23) ?: 8,
            departureMinute = state.minute.toIntOrNull()?.coerceIn(0, 59) ?: 0,
            checkOffsetsMinutes = state.offsets.ifBlank { "30" },
            delayThresholdMinutes = state.threshold.toIntOrNull() ?: 10,
            activeDays = state.activeDays,
            enabled = state.enabled,
            isCustomRoute = state.isCustomRoute,
            lockedRoutePolyline = state.lockedRoutePolyline,
            lockedRouteSummary = state.lockedRouteSummary,
            lockedRouteWaypoints = state.lockedRouteWaypoints,
        )
        viewModelScope.launch(Dispatchers.IO) {
            val id = dao.upsert(route)
            val saved = route.copy(id = if (route.id == 0L) id else route.id)
            AlarmScheduler.scheduleAllForRoute(context, saved)
        }
        _editState.value = null
    }

    fun deleteEditedRoute(context: Context) {
        val state = _editState.value ?: return
        if (state.isNewRoute) return
        val toDelete = RouteEntity(
            id = state.id,
            name = state.name,
            originAddress = state.origin,
            destinationAddress = state.destination,
        )
        AlarmScheduler.cancelAllForRoute(context, state.id)
        viewModelScope.launch(Dispatchers.IO) { dao.delete(toDelete) }
        _editState.value = null
    }

    fun openRoadPicker() {
        val state = _editState.value ?: return
        val existingStops = if (state.isCustomRoute) decodeWaypoints(state.lockedRouteWaypoints) else emptyList()
        val key = settingsStore.getApiKey()
        val hasKey = !key.isNullOrBlank()
        _pickerState.value = RoutePickerState(
            origin = state.origin,
            destination = state.destination,
            isCustomizing = existingStops.isNotEmpty(),
            stops = existingStops,
            isLoading = hasKey,
            missingApiKey = !hasKey,
            )
        // No point in calling the API without a key
        if (hasKey) {
            viewModelScope.launch(Dispatchers.IO) {
                val options =
                    RoutesApiClient.fetchRouteAlternatives(state.origin, state.destination, key)
                _pickerState.value =
                    _pickerState.value?.copy(routeOptions = options, isLoading = false)
            }
        }
        // Recomputes right away so reopening a customized route shows its real
        if (existingStops.isNotEmpty()) scheduleStopRecompute()
    }

    fun confirmPickedRoute(picked: RouteOption, isCustom: Boolean) {
        updateEditState {
            it.copy(
                lockedRoutePolyline = picked.encodedPolyline,
                lockedRouteSummary = picked.summary,
                lockedRouteWaypoints = encodeWaypoints(picked.waypoints),
                isCustomRoute = isCustom
            )
        }
        _pickerState.value = null
    }

    fun cancelRoadPicker() {
        recomputeJob?.cancel()
        _pickerState.value = null
    }

    fun setPickerCustomizing(customizing: Boolean) {
        _pickerState.value = _pickerState.value?.copy(isCustomizing = customizing)
    }

    private var recomputeJob: Job? = null

    fun addPickerStop(lat: Double, lng: Double) {
        val state = _pickerState.value ?: return
        _pickerState.value = state.copy(stops = state.stops + (lat to lng))
        scheduleStopRecompute()
    }

    fun movePickerStop(index: Int, lat: Double, lng: Double) {
        val state = _pickerState.value ?: return
        if (index !in state.stops.indices) return
        _pickerState.value = state.copy(
            stops = state.stops.toMutableList().also { it[index] = lat to lng },
        )
        scheduleStopRecompute()
    }

    fun removePickerStop(index: Int) {
        val state = _pickerState.value ?: return
        if (index !in state.stops.indices) return
        _pickerState.value = state.copy(
            stops = state.stops.toMutableList().also { it.removeAt(index) },
        )
        scheduleStopRecompute()
    }

    // Fires on each intermediate drag position (not just on release)
    // Cancel & (re)launch means only the last call reaches the network call (when the marker stops moving)
    private fun scheduleStopRecompute() {
        recomputeJob?.cancel() // stop the previous pending recompute, if there is one
        val state = _pickerState.value ?: return
        if (state.stops.isEmpty()) {
            _pickerState.value = state.copy(customRoute = null, isRecomputing = false)
            return
        }
        recomputeJob = viewModelScope.launch { // start a new pending recompute
            _pickerState.value = _pickerState.value?.copy(isRecomputing = true)
            delay(600) //milliseconds
            val current = _pickerState.value ?: return@launch
            val key = settingsStore.getApiKey() ?: ""
            val result = withContext(Dispatchers.IO) {
                RoutesApiClient.fetchRouteThroughStops(current.origin, current.destination, current.stops, key)
            }
            _pickerState.value = _pickerState.value?.copy(customRoute = result, isRecomputing = false)
        }
    }
    // ---- address autocomplete ----
    private class AddressSearchState {
        val predictions = MutableStateFlow<List<AddressPrediction>>(emptyList())
        var sessionToken: AutocompleteSessionToken? = null
        var job: Job? = null
    }

    private val originSearch = AddressSearchState()
    private val destinationSearch = AddressSearchState()
    val originPredictions: StateFlow<List<AddressPrediction>> = originSearch.predictions.asStateFlow()
    val destinationPredictions: StateFlow<List<AddressPrediction>> = destinationSearch.predictions.asStateFlow()

    fun searchOriginPredictions(context: Context, query: String) = searchPredictions(context, query, originSearch)
    fun searchDestinationPredictions(context: Context, query: String) =
        searchPredictions(context, query, destinationSearch)

    // A session ends once a place is picked
    fun originPredictionSelected() {
        originSearch.job?.cancel()
        originSearch.sessionToken = null
        originSearch.predictions.value = emptyList()
    }
    fun destinationPredictionSelected() {
        destinationSearch.job?.cancel()
        destinationSearch.sessionToken = null
        destinationSearch.predictions.value = emptyList()
    }

    private fun searchPredictions(context: Context, query: String, state: AddressSearchState) {
        state.job?.cancel()
        if (query.isBlank()) {
            state.predictions.value = emptyList()
            return
        }
        if (state.sessionToken == null) {
            state.sessionToken = AutocompleteSessionToken.newInstance()
        }
        val token = state.sessionToken!!
        state.job = viewModelScope.launch(Dispatchers.IO) {
            delay(300)
            val results = try {
                val request = FindAutocompletePredictionsRequest.builder()
                    .setQuery(query)
                    .setSessionToken(token)
                    .build()
                Places.createClient(context)
                    .findAutocompletePredictions(request)
                    .awaitTask()
                    .autocompletePredictions
                    .map {
                        AddressPrediction(
                            primaryText = it.getPrimaryText(null).toString(),
                            secondaryText = it.getSecondaryText(null).toString(),
                        )
                    }
            } catch (e: Exception) {
                Log.e("RouteViewModel", "findAutocompletePredictions failed", e)
                emptyList()
            }
            state.predictions.value = results
        }
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
    }

    // ---- route form: real route alternatives, once both fields resolve ----
    private val _routeFormAlternatives = MutableStateFlow<List<RouteAlternative>>(emptyList())
    val routeFormAlternatives: StateFlow<List<RouteAlternative>> = _routeFormAlternatives.asStateFlow()
    private var routeFormAlternativesJob: Job? = null

    fun fetchRouteFormAlternatives(originAddress: String, destinationAddress: String) {
        routeFormAlternativesJob?.cancel()
        routeFormAlternativesJob = viewModelScope.launch(Dispatchers.IO) {
            val key = settingsStore.getApiKey() ?: ""
            val options = RoutesApiClient.fetchRouteAlternatives(originAddress, destinationAddress, key)
            _routeFormAlternatives.value = options.map { option ->
                RouteAlternative(
                    option = option,
                    points = RoutesApiClient.decodePolyline(option.encodedPolyline)
                        .map { (lat, lng) -> LatLng(lat, lng) },
                )
            }
        }
    }

    fun clearRouteFormAlternatives() {
        routeFormAlternativesJob?.cancel()
        _routeFormAlternatives.value = emptyList()
    }
}

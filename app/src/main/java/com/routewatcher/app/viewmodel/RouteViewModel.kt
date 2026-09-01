package com.routewatcher.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routewatcher.app.data.RouteDao
import com.routewatcher.app.data.RouteEntity
import com.routewatcher.app.data.SettingsStore
import com.routewatcher.app.alarm.AlarmScheduler
import com.routewatcher.app.network.RoutesApiClient
import com.routewatcher.app.network.RouteOption
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Single shared ViewModel for the whole app (screen navigation, list, add/edit, picker state, settings)
class RouteViewModel(
    private val dao: RouteDao,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    val routes: StateFlow<List<RouteEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _apiKey = MutableStateFlow(settingsStore.getApiKey())
    val apiKey: StateFlow<String?> = _apiKey.asStateFlow()

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult: StateFlow<String?> = _testResult.asStateFlow()

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
            _testResult.value = if (result.success) {
                "Key works. Test route: ${result.trafficDurationMinutes} min."
            } else {
                "Test failed: ${result.errorMessage}"
            }
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
        _pickerState.value = RoutePickerState(origin = state.origin, destination = state.destination)
        viewModelScope.launch(Dispatchers.IO) {
            val key = settingsStore.getApiKey() ?: ""
            val options = RoutesApiClient.fetchRouteAlternatives(state.origin, state.destination, key)
            _pickerState.value = _pickerState.value?.copy(routeOptions = options, isLoading = false)
        }
    }

    fun confirmPickedRoute(picked: RouteOption) {
        updateEditState {
            it.copy(
                lockedRoutePolyline = picked.encodedPolyline,
                lockedRouteSummary = picked.summary,
                lockedRouteWaypoints = encodeWaypoints(picked.waypoints),
            )
        }
        _pickerState.value = null
    }

    fun cancelRoadPicker() {
        _pickerState.value = null
    }
}
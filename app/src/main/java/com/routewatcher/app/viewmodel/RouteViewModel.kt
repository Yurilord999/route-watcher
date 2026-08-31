package com.routewatcher.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routewatcher.app.data.RouteDao
import com.routewatcher.app.data.RouteEntity
import com.routewatcher.app.data.SettingsStore
import com.routewatcher.app.alarm.AlarmScheduler
import com.routewatcher.app.network.RoutesApiClient
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
}
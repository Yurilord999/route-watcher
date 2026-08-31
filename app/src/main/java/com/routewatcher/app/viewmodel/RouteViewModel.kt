package com.routewatcher.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routewatcher.app.data.RouteDao
import com.routewatcher.app.data.RouteEntity
import com.routewatcher.app.data.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

// Single shared ViewModel for the whole app (screen navigation, list, add/edit, picker state, settings)
class RouteViewModel(
    private val dao: RouteDao,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    val routes: StateFlow<List<RouteEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
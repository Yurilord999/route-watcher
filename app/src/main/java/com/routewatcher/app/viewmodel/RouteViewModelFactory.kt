package com.routewatcher.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.routewatcher.app.data.RouteDao
import com.routewatcher.app.data.SettingsStore

// Manual factory since this app doesn't use a DI library
// RouteViewModel needs constructor args (dao, settingsStore) the default factory cant provide
class RouteViewModelFactory(
    private val dao: RouteDao,
    private val settingsStore: SettingsStore,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RouteViewModel::class.java)) {
            return RouteViewModel(dao, settingsStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}
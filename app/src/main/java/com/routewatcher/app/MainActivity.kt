package com.routewatcher.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.routewatcher.app.data.AppDatabase
import com.routewatcher.app.data.RouteEntity
import com.routewatcher.app.data.SettingsStore
import com.routewatcher.app.ui.AddEditRouteScreen
import com.routewatcher.app.ui.RouteListScreen
import com.routewatcher.app.ui.SettingsScreen
import com.routewatcher.app.ui.theme.RouteWatcherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private sealed class Screen {
    data object List : Screen()
    data class AddEdit(val route: RouteEntity?) : Screen()
    data object Settings : Screen()
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Database instance for whole activity lifetime
        val dao = AppDatabase.get(this).routeDao()
        val settingsStore = SettingsStore(this)

        setContent {
            RouteWatcherTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.List) }
                val routes by dao.observeAll().collectAsState(initial = emptyList())
                var apiKey by remember { mutableStateOf(settingsStore.getApiKey()) }

                when (val currentScreen = screen) {
                    is Screen.List -> RouteListScreen(
                        routes = routes,
                        onAddRoute = { screen = Screen.AddEdit(null) },
                        onEditRoute = { screen = Screen.AddEdit(it) },
                        onOpenSettings = { screen = Screen.Settings },
                    )
                    is Screen.AddEdit -> AddEditRouteScreen(
                        existing = currentScreen.route,
                        onSave = { route ->
                            lifecycleScope.launch(Dispatchers.IO) { dao.upsert(route) }
                            screen = Screen.List
                        },
                        onDelete = currentScreen.route?.let { existing ->
                            {
                                lifecycleScope.launch(Dispatchers.IO) { dao.delete(existing) }
                                screen = Screen.List
                            }
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
                        onBack = { screen = Screen.List },
                    )
                }
            }
        }
    }
}

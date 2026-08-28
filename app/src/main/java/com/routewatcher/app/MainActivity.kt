package com.routewatcher.app

import android.os.Bundle
import android.os.Build
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import com.routewatcher.app.data.AppDatabase
import com.routewatcher.app.data.RouteEntity
import com.routewatcher.app.data.SettingsStore
import com.routewatcher.app.ui.AddEditRouteScreen
import com.routewatcher.app.ui.RouteListScreen
import com.routewatcher.app.ui.SettingsScreen
import com.routewatcher.app.ui.theme.RouteWatcherTheme
import com.routewatcher.app.network.DistanceMatrixClient
import com.routewatcher.app.alarm.AlarmScheduler
import com.routewatcher.app.alarm.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


private sealed class Screen {
    data object List : Screen()
    data class AddEdit(val route: RouteEntity?) : Screen()
    data object Settings : Screen()
}
class MainActivity : ComponentActivity() {

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.ensureChannels(this)
        requestNotificationPermissionIfNeeded()

        // Database instance for whole activity lifetime
        val dao = AppDatabase.get(this).routeDao()
        val settingsStore = SettingsStore(this)

        setContent {
            RouteWatcherTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.List) }
                val routes by dao.observeAll().collectAsState(initial = emptyList())
                var apiKey by remember { mutableStateOf(settingsStore.getApiKey()) }
                var testResult by remember { mutableStateOf<String?>(null) }

                when (val currentScreen = screen) {
                    is Screen.List -> RouteListScreen(
                        routes = routes,
                        onAddRoute = { screen = Screen.AddEdit(null) },
                        onEditRoute = { screen = Screen.AddEdit(it) },
                        onToggleRoute = { route, enabled ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                val updated = route.copy(enabled = enabled)
                                dao.upsert(updated)
                                if (enabled) {
                                    withContext(Dispatchers.Main) { maybeRequestExactAlarmPermission() }
                                    AlarmScheduler.scheduleAllForRoute(this@MainActivity, updated)
                                } else {
                                    AlarmScheduler.cancelAllForRoute(this@MainActivity, updated.id)
                                }
                            }
                        },
                        onOpenSettings = { screen = Screen.Settings },
                    )
                    is Screen.AddEdit -> AddEditRouteScreen(
                        existing = currentScreen.route,
                        onSave = { route ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                val id = dao.upsert(route)
                                val saved = route.copy(id = if (route.id == 0L) id else route.id)
                                AlarmScheduler.scheduleAllForRoute(this@MainActivity, saved)
                            }
                            screen = Screen.List
                        },
                        onDelete = currentScreen.route?.let { existing ->
                            {
                                AlarmScheduler.cancelAllForRoute(this@MainActivity, existing.id)
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
                        onTestKey = {
                            val key = settingsStore.getApiKey()
                            lifecycleScope.launch(Dispatchers.IO) {
                                val result = DistanceMatrixClient.checkTraffic(
                                    "Dresden Hauptbahnhof, Dresden",
                                    "Frauenkirche Dresden, Dresden",
                                    key ?: "",
                                )
                                withContext(Dispatchers.Main) {
                                    testResult = if (result.success) {
                                        "Key works. Test route: ${result.trafficDurationMinutes} min."
                                    } else {
                                        "Test failed: ${result.errorMessage}"
                                    }
                                }
                            }
                        },
                        testResultMessage = testResult,
                        onBack = { screen = Screen.List },
                    )
                }
            }
        }
    }
    // On Android 12+, exact alarms require an explicit user grant via system settings
    private fun maybeRequestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    },
                )
            }
        }
    }
    // On Android 13+, showing notifications requires an explicit runtime grant too
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

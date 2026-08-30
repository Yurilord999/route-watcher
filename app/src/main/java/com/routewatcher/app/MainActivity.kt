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
import androidx.core.content.ContextCompat
import com.routewatcher.app.data.AppDatabase
import com.routewatcher.app.data.SettingsStore
import com.routewatcher.app.ui.theme.RouteWatcherTheme
import com.routewatcher.app.ui.RouteWatcherApp
import com.routewatcher.app.alarm.NotificationHelper

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
                RouteWatcherApp(
                    dao = dao,
                    settingsStore = settingsStore,
                    onRequestExactAlarmPermission = { maybeRequestExactAlarmPermission() },
                )
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
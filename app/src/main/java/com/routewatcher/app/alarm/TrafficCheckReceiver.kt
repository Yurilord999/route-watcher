package com.routewatcher.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.routewatcher.app.data.AppDatabase
import com.routewatcher.app.data.SettingsStore
import com.routewatcher.app.network.RoutesApiClient
import com.routewatcher.app.network.TrafficErrorCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Fires when a scheduled alarm goes off. Runs one traffic check, shows a notification,
// then reschedules the next occurrence of the same offset
class TrafficCheckReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val routeId = intent.getLongExtra(EXTRA_ROUTE_ID, -1)
        val offsetMinutes = intent.getIntExtra(EXTRA_OFFSET_MINUTES, 0)
        if (routeId == -1L) return

        val powerManager = context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "routewatcher:trafficcheck")
        wakeLock.acquire(60_000L)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCheck(context, routeId, offsetMinutes)
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
                pendingResult.finish()
            }
        }
    }

    private suspend fun runCheck(context: Context, routeId: Long, offsetMinutes: Int) {
        val dao = AppDatabase.get(context).routeDao()
        val route = dao.getById(routeId) ?: return
        val apiKey = SettingsStore(context).getApiKey()

        if (apiKey.isNullOrBlank()) {
            NotificationHelper.showCheckFailed(context, route,TrafficErrorCode.NO_API_KEY)
        } else {
            val result = RoutesApiClient.checkTrafficOnRoute(
                route.originAddress,
                route.destinationAddress,
                route.lockedWaypointsList(),
                apiKey,
            )
            if (result.success) {
                if (result.delayMinutes >= route.delayThresholdMinutes) {
                    NotificationHelper.showJamAlert(context, route, result, offsetMinutes)
                } else {
                    NotificationHelper.showAllClear(context, route, result, offsetMinutes)
                }
            } else {
                NotificationHelper.showCheckFailed(context, route, result.errorCode)
            }
        }

        if (route.enabled) AlarmScheduler.scheduleNext(context, route, offsetMinutes)
    }

    companion object {
        const val EXTRA_ROUTE_ID = "extra_route_id"
        const val EXTRA_OFFSET_MINUTES = "extra_offset_minutes"
    }
}
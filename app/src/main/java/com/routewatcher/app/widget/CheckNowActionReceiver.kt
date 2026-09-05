package com.routewatcher.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.routewatcher.app.alarm.NotificationHelper
import com.routewatcher.app.data.AppDatabase
import com.routewatcher.app.data.SettingsStore
import com.routewatcher.app.network.RoutesApiClient
import com.routewatcher.app.network.TrafficErrorCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Fires when the widgets "Check now" button is tapped (one immediate check per enabled route)
// Same logic as TrafficCheckReceiver but triggered manually instead
class CheckNowActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.get(context).routeDao()
                val apiKey = SettingsStore(context).getApiKey()
                val routes = dao.getAllEnabled()

                if (apiKey.isNullOrBlank()) {
                    NotificationHelper.showCheckFailed(context, routes.firstOrNull() ?: return@launch, TrafficErrorCode.NO_API_KEY)
                    return@launch
                }

                routes.forEach { route ->
                    val result = RoutesApiClient.checkTrafficOnRoute(
                        route.originAddress,
                        route.destinationAddress,
                        route.lockedWaypointsList(),
                        apiKey,
                    )
                    if (result.success) {
                        if (result.delayMinutes >= route.delayThresholdMinutes) {
                            NotificationHelper.showJamAlert(context, route, result, 0)
                        } else {
                            NotificationHelper.showAllClear(context, route, result, 0)
                        }
                    } else {
                        NotificationHelper.showCheckFailed(context, route, result.errorCode)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
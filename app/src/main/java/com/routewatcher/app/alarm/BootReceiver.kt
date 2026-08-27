package com.routewatcher.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.routewatcher.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// AlarmManager alarms are cleared on reboot. This reschedules every enabled routes alarm once the device boots
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val routes = AppDatabase.get(context).routeDao().getAllEnabled()
                routes.forEach { AlarmScheduler.scheduleAllForRoute(context, it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
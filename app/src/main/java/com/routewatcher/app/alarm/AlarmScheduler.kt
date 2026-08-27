package com.routewatcher.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.routewatcher.app.data.RouteEntity
import java.util.Calendar

// Schedules one alarm per (route, check-offset) pair, for the next upcoming active day
// Uses setAlarmClock() so Doze/App Standby cannot delay it
// TODO: called from MainActivity's onToggleRoute/onSave once the UI is done. (Not used yet)

object AlarmScheduler {

    fun scheduleAllForRoute(context: Context, route: RouteEntity) {
        cancelAllForRoute(context, route.id)
        if (!route.enabled) return
        route.offsetsList().forEach { offsetMinutes ->
            scheduleNext(context, route, offsetMinutes)
        }
    }

    fun cancelAllForRoute(context: Context, routeId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (slot in 0 until 10) {
            val requestCode = requestCode(routeId, slot)
            val pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, TrafficCheckReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (pi != null) {
                alarmManager.cancel(pi)
                pi.cancel()
            }
        }
    }

    fun scheduleNext(context: Context, route: RouteEntity, offsetMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = nextTriggerTime(route, offsetMinutes)

        val intent = Intent(context, TrafficCheckReceiver::class.java).apply {
            putExtra(TrafficCheckReceiver.EXTRA_ROUTE_ID, route.id)
            putExtra(TrafficCheckReceiver.EXTRA_OFFSET_MINUTES, offsetMinutes)
        }
        val slot = route.offsetsList().indexOf(offsetMinutes).coerceAtLeast(0)
        val requestCode = requestCode(route.id, slot)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val showIntent = PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, Class.forName("com.routewatcher.app.MainActivity")),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent), pendingIntent)
    }

    // Finds the next timestamp (ms) when this route+offset should fire
    private fun nextTriggerTime(route: RouteEntity, offsetMinutes: Int): Long {
        val now = System.currentTimeMillis()
        for (dayAhead in 0..7) {
            val candidate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, dayAhead)
                set(Calendar.HOUR_OF_DAY, route.departureHour)
                set(Calendar.MINUTE, route.departureMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MINUTE, -offsetMinutes)
            }
            val dayBit = dayBitFor(candidate)
            if ((route.activeDays and dayBit) != 0 && candidate.timeInMillis > now) {
                return candidate.timeInMillis
            }
        }
        return now + 7L * 24 * 60 * 60 * 1000
    }

    private fun dayBitFor(cal: Calendar): Int = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 4
        Calendar.THURSDAY -> 8
        Calendar.FRIDAY -> 16
        Calendar.SATURDAY -> 32
        else -> 64
    }

    private fun requestCode(routeId: Long, slot: Int): Int = (routeId * 100 + slot).toInt()
}
package com.routewatcher.app.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.routewatcher.app.MainActivity
import com.routewatcher.app.data.RouteEntity
import com.routewatcher.app.network.TrafficResult

object NotificationHelper {
    private const val CHANNEL_ALERTS = "traffic_alerts"
    private const val CHANNEL_STATUS = "traffic_status"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "Traffic jam alerts", NotificationManager.IMPORTANCE_HIGH),
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_STATUS, "Route check status", NotificationManager.IMPORTANCE_LOW),
        )
    }

    fun showJamAlert(context: Context, route: RouteEntity, result: TrafficResult, offsetMinutes: Int) {
        val n = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Traffic jam on ${route.name}")
            .setContentText("+${result.delayMinutes} min delay - leaves in $offsetMinutes min")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        NotificationManagerCompat.from(context).notify(route.id.toInt() * 10 + 1, n)
    }

    fun showAllClear(context: Context, route: RouteEntity, result: TrafficResult, offsetMinutes: Int) {
        val n = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentTitle("${route.name}: all clear")
            .setContentText("No delays - leaves in $offsetMinutes min")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        NotificationManagerCompat.from(context).notify(route.id.toInt() * 10 + 2, n)
    }

    fun showCheckFailed(context: Context, route: RouteEntity, error: String?) {
        val n = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Couldn't check ${route.name}")
            .setContentText(error ?: "Unknown error")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        NotificationManagerCompat.from(context).notify(route.id.toInt() * 10 + 3, n)
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
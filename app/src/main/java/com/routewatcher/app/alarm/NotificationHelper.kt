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
import com.routewatcher.app.R
import com.routewatcher.app.network.TrafficErrorCode
import com.routewatcher.app.network.errorMessageRes

object NotificationHelper {
    private const val CHANNEL_ALERTS = "traffic_alerts"
    private const val CHANNEL_STATUS = "traffic_status"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTS,
                context.getString(R.string.channel_alerts_name),
                NotificationManager.IMPORTANCE_HIGH
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_STATUS,
                context.getString(R.string.channel_status_name),
                NotificationManager.IMPORTANCE_LOW
            ),
        )
    }

    fun showJamAlert(context: Context, route: RouteEntity, result: TrafficResult, offsetMinutes: Int) {
        val n = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(
                context.getString(
                    R.string.notif_jam_title,
                    route.name
                )
            )
            .setContentText(
                context.getString(
                    R.string.notif_jam_text,
                    result.delayMinutes,
                    offsetMinutes
                )
            )
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

    fun showCheckFailed(context: Context, route: RouteEntity, errorCode: TrafficErrorCode?) {
        val n = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(
                context.getString(
                    R.string.notif_check_failed_title,
                    route.name
                )
            )
            .setContentText(
                context.getString(
                    errorMessageRes(errorCode)
                )
            )
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
package com.routewatcher.app.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.Manifest
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.routewatcher.app.MainActivity
import com.routewatcher.app.data.RouteEntity
import com.routewatcher.app.network.TrafficResult
import com.routewatcher.app.R
import com.routewatcher.app.network.TrafficErrorCode
import com.routewatcher.app.network.errorMessageRes

object NotificationHelper {
    private const val CHANNEL_ALERTS = "traffic_alerts"
    private const val CHANNEL_STATUS = "traffic_status"

    //route.id 0 is always free for this one
    private const val NOTIFICATION_ID_API_KEY_MISSING = 0

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
            .setContentIntent(openRouteIntent(context, route))
            .addAction(
                android.R.drawable.ic_menu_directions,
                context.getString(R.string.open_in_maps),
                openMapsIntent(context, route),
            )
            .build()
        notifyIfPermitted(context, route.id.toInt() * 10 + 1, n)
    }

    fun showAllClear(context: Context, route: RouteEntity, result: TrafficResult, offsetMinutes: Int) {
        val n = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentTitle(context.getString(R.string.notif_all_clear_title, route.name))
            .setContentText(context.getString(R.string.notif_all_clear_text, offsetMinutes))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        notifyIfPermitted(context, route.id.toInt() * 10 + 2, n)
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
            .setContentIntent(
                if (errorCode == TrafficErrorCode.NO_API_KEY) openSettingsIntent(context) else openAppIntent(context)
            )
            .build()
        notifyIfPermitted(context, route.id.toInt() * 10 + 3, n)
    }

    // For failures not tied to a specific route (zero routes enabled)
    fun showApiKeyMissing(context: Context) {
        val n = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.notif_api_key_missing_title))
            .setContentText(context.getString(R.string.notif_api_key_missing_text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(openSettingsIntent(context))
            .build()
        notifyIfPermitted(context, NOTIFICATION_ID_API_KEY_MISSING, n)
    }
    // Android 13+ requires the POST_NOTIFICATIONS runtime permission for every notify() call
    private fun notifyIfPermitted(context: Context, id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN_APP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
    private fun openSettingsIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_SETTINGS, true)
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN_SETTINGS,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    // Deep link to a specific, congested, expanded route in route list.
    private fun openRouteIntent(context: Context, route: RouteEntity): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_EXPAND_ROUTE_ID, route.id)
        return PendingIntent.getActivity(
            context,
            route.id.toInt() * 10 + REQUEST_CODE_OPEN_ROUTE_OFFSET,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    // Google Maps hotkey for the given route
    private fun openMapsIntent(context: Context, route: RouteEntity): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(route.directionsUrl()))
        return PendingIntent.getActivity(
            context,
            route.id.toInt() * 10 + REQUEST_CODE_OPEN_MAPS_OFFSET,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
    private const val REQUEST_CODE_OPEN_APP = 0
    private const val REQUEST_CODE_OPEN_SETTINGS = 1
    private const val REQUEST_CODE_OPEN_MAPS_OFFSET = 4
    private const val REQUEST_CODE_OPEN_ROUTE_OFFSET = 6
}
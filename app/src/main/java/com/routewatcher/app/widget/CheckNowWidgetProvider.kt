package com.routewatcher.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.routewatcher.app.R

// Homescreen widget with a single "Check now" button
// Tapping it fires CheckNowActionReceiver, which runs a check for every enabled route immediately
class CheckNowWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_check_now)

            val checkIntent = Intent(context, CheckNowActionReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                checkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_check_button, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
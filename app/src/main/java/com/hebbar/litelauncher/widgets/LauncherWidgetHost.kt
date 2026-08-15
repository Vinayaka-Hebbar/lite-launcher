package com.hebbar.litelauncher.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Bundle

class LauncherWidgetHost(context: Context, hostId: Int = 1024) : AppWidgetHost(context, hostId) {

    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView {
        return LauncherAppWidgetHostView(context)
    }

    companion object {
        const val HOST_ID = 1024
    }
}

class LauncherAppWidgetHostView(context: Context) : AppWidgetHostView(context) {
    override fun updateAppWidgetSize(
        newOptions: Bundle?,
        minWidth: Int,
        minHeight: Int,
        maxWidth: Int,
        maxHeight: Int
    ) {
        super.updateAppWidgetSize(newOptions, minWidth, minHeight, maxWidth, maxHeight)
    }
}

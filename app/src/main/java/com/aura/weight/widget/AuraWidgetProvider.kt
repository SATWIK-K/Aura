package com.aura.weight.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

abstract class AuraWidgetProvider : AppWidgetProvider() {

    protected abstract val style: WidgetStyle

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        renderAsync(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        // Rebuild for the new size so text stays sharp and the layout adapts.
        renderAsync(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        renderAsync(context, AppWidgetManager.getInstance(context), newWidgetIds)
    }

    private fun renderAsync(
        context: Context,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray,
    ) {
        if (appWidgetManager == null || appWidgetIds.isEmpty()) return
        val pending: BroadcastReceiver.PendingResult? = try {
            goAsync()
        } catch (e: IllegalStateException) {
            null
        }
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val data = loadWidgetData(context)
                val dark = data.profile.darkTheme
                appWidgetIds.forEach { id ->
                    val options = appWidgetManager.getAppWidgetOptions(id)
                    val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                        .takeIf { it > 0 } ?: style.minWidthDp
                    val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
                        .takeIf { it > 0 } ?: style.minHeightDp
                    val views = WidgetRenderer.build(
                        context = context,
                        style = style,
                        data = data,
                        dark = dark,
                        minWidthDp = minW,
                        minHeightDp = minH,
                    )
                    appWidgetManager.updateAppWidget(id, views)
                }
            } finally {
                pending?.finish()
            }
        }
    }
}

object WidgetUpdater {

    private val providers = listOf(
        TodayWidgetProvider::class.java,
        TrendWidgetProvider::class.java,
        RingWidgetProvider::class.java,
        StreakWidgetProvider::class.java,
        BmiWidgetProvider::class.java,
        CompactWidgetProvider::class.java,
        ProgressWidgetProvider::class.java,
    )

    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        providers.forEach { clazz ->
            val ids = manager.getAppWidgetIds(ComponentName(context, clazz))
            if (ids.isEmpty()) return@forEach
            val intent = Intent(context, clazz).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}

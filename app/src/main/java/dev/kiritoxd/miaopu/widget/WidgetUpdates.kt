package dev.kiritoxd.miaopu.widget

import android.content.Context
import dev.kiritoxd.miaopu.data.Esport
import dev.kiritoxd.miaopu.data.Schedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

internal object WidgetUpdates {
    suspend fun publish(context: Context, sport: Esport, schedule: Schedule?, startedAt: Long) = withContext(Dispatchers.IO) {
        val store = WidgetScheduleStore(context)
        store.record(sport, schedule, startedAt)
        if (WidgetRenderer.ids(context).isEmpty()) return@withContext
        WidgetRenderer.renderAll(context)
        try {
            WidgetLogoStore(context).load(store.state(System.currentTimeMillis()).matches)
        } catch (_: IOException) {
            // A missing logo uses the team initial; the schedule remains available.
        }
        WidgetRenderer.renderAll(context)
    }

    suspend fun onAppForeground(context: Context) {
        if (WidgetRenderer.ids(context).isEmpty()) return
        WidgetRenderer.renderAll(context)
        WidgetRefreshService.enqueue(context)
    }

    suspend fun subscriptionsChanged(context: Context) {
        WidgetRenderer.renderAll(context)
        WidgetRefreshService.enqueue(context)
    }
}

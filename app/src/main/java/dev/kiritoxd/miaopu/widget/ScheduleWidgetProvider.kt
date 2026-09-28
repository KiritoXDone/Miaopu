package dev.kiritoxd.miaopu.widget

import android.app.job.JobScheduler
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MatchWidgetProvider : BaseScheduleWidgetProvider()
class ScheduleWidgetProvider : BaseScheduleWidgetProvider()

open class BaseScheduleWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WidgetIntents.REFRESH) {
            render(context)
            WidgetRefreshService.enqueue(context)
        } else {
            super.onReceive(context, intent)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        render(context)
        WidgetRefreshService.enqueue(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        render(context)
    }

    override fun onDisabled(context: Context) {
        if (WidgetRenderer.ids(context).isEmpty()) {
            context.getSystemService(JobScheduler::class.java).cancel(WidgetRefreshService.JOB_ID)
        }
    }

    private fun render(context: Context) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                WidgetRenderer.renderAll(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }
}

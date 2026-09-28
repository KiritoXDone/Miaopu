package dev.kiritoxd.miaopu.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import dev.kiritoxd.miaopu.R

/** Visual demo only: never writes mock matches to the application's schedule cache. */
internal object WidgetLivePreview {
    fun show(context: Context): Int {
        val now = System.currentTimeMillis()
        val actual = WidgetScheduleStore(context).state(now)
        val original = requireNotNull(actual.matches.firstOrNull()) { "Load a real schedule before previewing" }
        val live = original.copy(status = "进行中", statusCode = "LIVE", startsAt = now - 30 * 60_000L,
            teams = original.teams.map { it.copy(score = "1") })
        val preview = actual.copy(matches = listOf(live) + actual.matches.drop(1), updatedAt = now, failed = false)
        val manager = AppWidgetManager.getInstance(context)
        val widgets = WidgetRenderer.ids(context)
        widgets.forEach { (id, wide) ->
            val options = manager.getAppWidgetOptions(id)
            val views = WidgetRenderer.create(context, id, wide, preview, now,
                height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180),
                width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, if (wide) 340 else 170),
                largeFont = context.resources.configuration.fontScale > 1.3f)
            views.setTextViewText(R.id.widget_footer, if (wide) "演示数据 · 刷新恢复" else "${live.sportLabel} · 演示")
            views.setContentDescription(R.id.widget_root, "正在展示模拟赛况，刷新恢复真实数据")
            manager.updateAppWidget(id, views)
        }
        return widgets.size
    }
}

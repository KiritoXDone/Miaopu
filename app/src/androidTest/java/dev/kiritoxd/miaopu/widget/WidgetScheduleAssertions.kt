package dev.kiritoxd.miaopu.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.view.ViewGroup
import android.widget.RemoteViews
import android.widget.TextView
import dev.kiritoxd.miaopu.R

internal object WidgetScheduleAssertions {
    fun verifyHostSizes(context: Context) {
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 340)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 900)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 140)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 280)
        }
        val rendered = mutableListOf<Pair<Int, Int>>()
        val create = { width: Int, height: Int ->
            rendered += width to height
            RemoteViews(context.packageName, R.layout.widget_schedule)
        }
        responsiveWidgetViews(options, true, create)
        check(rendered == listOf(900 to 140, 340 to 280)) { "Legacy hosts must keep orientation size pairs" }
        if (Build.VERSION.SDK_INT >= 31) {
            rendered.clear()
            options.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,
                arrayListOf(SizeF(760f, 300f), SizeF(340f, 500f)))
            responsiveWidgetViews(options, true, create)
            check(rendered == listOf(760 to 300, 340 to 500)) { "Use exact sizes reported by the launcher" }
        }
        val normal = context.createConfigurationContext(Configuration(context.resources.configuration).apply { fontScale = 1f })
        val large = context.createConfigurationContext(Configuration(context.resources.configuration).apply { fontScale = 1.5f })
        check(!WidgetScheduleLayout.hasTwoColumns(normal, 340, false))
        check(WidgetScheduleLayout.hasTwoColumns(normal, 760, false))
        check(!WidgetScheduleLayout.hasTwoColumns(large, 760, false))
        check(WidgetScheduleLayout.hasTwoColumns(large, 1200, true))
    }

    fun verifyColumns(view: View, state: WidgetScheduleState, expectColumns: Boolean) {
        val history = view.findViewById<ViewGroup>(R.id.widget_history_rows)
        val recent = view.findViewById<ViewGroup>(R.id.widget_recent_rows)
        check((history != null && recent != null) == expectColumns) { "Unexpected column count" }
        if (!expectColumns) return
        check(history != null && recent != null)
        verifySection(history, state.matches.filter(WidgetMatch::terminal))
        verifySection(recent, state.matches.filterNot(WidgetMatch::terminal))
        check(view.findViewById<View>(R.id.widget_history_empty).visibility ==
            if (state.matches.none(WidgetMatch::terminal)) View.VISIBLE else View.GONE)
        check(view.findViewById<View>(R.id.widget_recent_empty).visibility ==
            if (state.matches.all(WidgetMatch::terminal)) View.VISIBLE else View.GONE)
        if (history.visibility == View.VISIBLE && recent.visibility == View.VISIBLE) {
            check(kotlin.math.abs(history.width - recent.width) <= 1) { "Columns must share available width equally" }
            repeat(minOf(history.childCount, recent.childCount)) { index ->
                check(history.getChildAt(index).top == recent.getChildAt(index).top) { "Opposing rows must align" }
                check(history.getChildAt(index).height == recent.getChildAt(index).height) { "Opposing rows must have equal heights" }
            }
        }
    }

    private fun verifySection(rows: ViewGroup, matches: List<WidgetMatch>) {
        val visible = (0 until rows.childCount).map(rows::getChildAt).filter { it.visibility == View.VISIBLE }
        check(visible.size == minOf(matches.size, rows.childCount))
        visible.forEachIndexed { index, row ->
            check(row.findViewById<TextView>(R.id.widget_row_team_0).text.toString() == matches[index].teams[0].name)
            check(row.findViewById<TextView>(R.id.widget_row_score).text.toString() == matches[index].scoreLabel())
            check(row.findViewById<TextView>(R.id.widget_row_team_1).text.toString() == matches[index].teams[1].name)
            verifyHorizontalBounds(row)
        }
    }

    private fun verifyHorizontalBounds(view: View) {
        if (view.visibility != View.VISIBLE) return
        if (view is TextView && view.text.isNotEmpty()) check(view.width > 0) { "Text has no horizontal space: ${view.text}" }
        if (view is ViewGroup) repeat(view.childCount) {
            val child = view.getChildAt(it)
            if (child.visibility == View.VISIBLE) {
                check(child.left >= -1 && child.right <= view.width + 1) { "Child exceeds column width" }
                verifyHorizontalBounds(child)
            }
        }
    }
}

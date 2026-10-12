package dev.kiritoxd.miaopu.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import dev.kiritoxd.miaopu.R

/** Derive the column breakpoint from the existing row, not a device model or screen-pixel width. */
internal object WidgetScheduleLayout {
    fun hasTwoColumns(context: Context, widthDp: Int, compact: Boolean): Boolean {
        val inflater = LayoutInflater.from(context)
        val container = inflater.inflate(
            if (compact) R.layout.widget_schedule_compact else R.layout.widget_schedule, null,
        )
        val row = inflater.inflate(R.layout.widget_schedule_row, null)
        val leftTeam = row.findViewById<TextView>(R.id.widget_row_team_0)
        val rightTeam = row.findViewById<TextView>(R.id.widget_row_team_1)
        // Reserve the existing team-name allowance on both sides, independent of today's names.
        leftTeam.layoutParams.width = rightTeam.maxWidth
        rightTeam.layoutParams.width = rightTeam.maxWidth
        row.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        val resources = context.resources
        val available = widthDp * resources.displayMetrics.density - container.paddingLeft - container.paddingRight
        val columnWidth = row.measuredWidth * resources.configuration.fontScale.coerceAtLeast(1f)
        val gap = resources.getDimension(R.dimen.widget_schedule_column_gap)
        return available >= columnWidth * 2 + gap
    }
}

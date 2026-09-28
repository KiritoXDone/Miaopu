package dev.kiritoxd.miaopu.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.SuperscriptSpan
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import dev.kiritoxd.miaopu.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

internal object WidgetRenderer {
    fun ids(context: Context): List<Pair<Int, Boolean>> {
        val manager = AppWidgetManager.getInstance(context)
        return listOf(MatchWidgetProvider::class.java to false, ScheduleWidgetProvider::class.java to true)
            .flatMap { (provider, wide) ->
                manager.getAppWidgetIds(ComponentName(context, provider)).map { it to wide }
            }
    }

    suspend fun renderAll(context: Context, refreshing: Boolean = false) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val state = WidgetScheduleStore(context).state(now)
        val manager = AppWidgetManager.getInstance(context)
        ids(context).forEach { (id, wide) ->
            val options = manager.getAppWidgetOptions(id)
            val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160)
            val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, if (wide) 340 else 170)
            val largeFont = context.resources.configuration.fontScale > 1.3f
            val views = create(context, id, wide, state, now, height, largeFont, refreshing, width)
            manager.updateAppWidget(id, views)
        }
    }

    internal fun create(
        context: Context, id: Int, wide: Boolean, state: WidgetScheduleState, now: Long,
        height: Int = 180, largeFont: Boolean = false, refreshing: Boolean = false,
        width: Int = if (wide) 340 else 170,
    ): RemoteViews {
        val contentHeight = if (wide) height else minOf(width, height)
        val compact = contentHeight < 155 || largeFont
        val layout = when {
            wide && compact -> R.layout.widget_schedule_compact
            wide -> R.layout.widget_schedule
            compact -> R.layout.widget_match_compact
            else -> R.layout.widget_match
        }
        val views = RemoteViews(context.packageName, layout)
        if (!wide) {
            val density = context.resources.displayMetrics.density
            val horizontal = ((width - contentHeight) * density).roundToInt().coerceAtLeast(0)
            val vertical = ((height - contentHeight) * density).roundToInt().coerceAtLeast(0)
            views.setViewPadding(R.id.widget_host, horizontal / 2, vertical / 2,
                horizontal - horizontal / 2, vertical - vertical / 2)
        }
        views.setOnClickPendingIntent(R.id.widget_root, WidgetIntents.open(context, id))
        views.setOnClickPendingIntent(R.id.widget_open, WidgetIntents.open(context, id))
        views.setOnClickPendingIntent(R.id.widget_refresh, WidgetIntents.refresh(context, id))
        views.setViewVisibility(R.id.widget_open, if (largeFont) View.GONE else View.VISIBLE)
        views.setContentDescription(R.id.widget_refresh, if (refreshing) "正在刷新赛程" else "刷新赛程")
        val updated = if (state.updatedAt > 0) {
            SimpleDateFormat(if (now - state.updatedAt > DAY) "MM-dd HH:mm" else "HH:mm", Locale.ROOT)
                .format(Date(state.updatedAt))
        } else ""
        val network = context.getSystemService(ConnectivityManager::class.java)
        val online = network.getNetworkCapabilities(network.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val stale = state.failed || !online || state.updatedAt > 0 && now - state.updatedAt >= 30 * 60_000L
        val footer = when {
            refreshing -> "正在刷新"
            !online && updated.isEmpty() -> "联网后自动更新"
            state.failed && updated.isEmpty() -> "刷新失败 · 点击重试"
            stale -> "缓存 $updated"
            updated.isNotEmpty() -> "$updated 更新"
            else -> "已订阅赛事"
        }
        views.setTextViewText(R.id.widget_footer, footer)
        views.setContentDescription(R.id.widget_footer, if (stale) "显示上次获取的赛程，更新时间 $updated，可点击刷新获取最新数据" else footer)
        val empty = state.matches.isEmpty()
        views.setViewVisibility(R.id.widget_empty, if (empty) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.widget_empty, when {
            !online && !state.hasFetched -> if (contentHeight < 135) "等待联网" else "等待网络连接"
            refreshing || !state.hasFetched -> if (contentHeight < 135) "加载中" else "正在加载赛程"
            state.failed -> if (contentHeight < 135) "获取失败" else "暂时无法获取赛程"
            else -> if (contentHeight < 135) "暂无比赛" else "近期暂无比赛"
        })
        views.setInt(R.id.widget_empty, "setMaxLines", 3)
        if (wide) {
            views.setTextViewText(R.id.widget_date, SimpleDateFormat("MM–dd", Locale.ROOT).format(Date(now)))
            views.setViewVisibility(R.id.widget_date, if (largeFont) View.GONE else View.VISIBLE)
            views.setViewVisibility(R.id.widget_rows, if (empty) View.GONE else View.VISIBLE)
            views.removeAllViews(R.id.widget_rows)
            state.matches.take(if (contentHeight < 130 || largeFont && contentHeight < 175) 1 else if (compact) 2 else 3).forEach { match ->
                views.addView(R.id.widget_rows, row(context, id, match, now, largeFont))
            }
        } else {
            if (compact) {
                views.setViewVisibility(R.id.widget_logo_container_0, if (contentHeight < 155) View.GONE else View.VISIBLE)
                views.setViewVisibility(R.id.widget_logo_container_1, if (contentHeight < 155) View.GONE else View.VISIBLE)
            }
            views.setViewVisibility(R.id.widget_match_content, if (empty) View.GONE else View.VISIBLE)
            views.setViewVisibility(R.id.widget_status, if (empty) View.INVISIBLE else View.VISIBLE)
            views.setViewVisibility(R.id.widget_competition, if (empty || contentHeight < 155 || largeFont) View.GONE else View.VISIBLE)
            state.matches.firstOrNull()?.let { match ->
                views.setTextViewText(R.id.widget_footer, match.sportLabel)
                views.setContentDescription(R.id.widget_footer, "赛事类型 ${match.sportLabel}")
                views.setTextViewText(R.id.widget_status, if (match.live) "● ${match.status}" else if (match.terminal) match.status else "${SimpleDateFormat("MM-dd HH:mm", Locale.ROOT).format(Date(match.startsAt))}")
                views.textColor(context, R.id.widget_status, if (match.live) R.color.widget_accent else R.color.widget_secondary)
                views.setTextViewText(R.id.widget_competition, match.competition.ifBlank { match.name })
                views.setTextViewText(R.id.widget_score, match.scoreLabel())
                views.setTextViewTextSize(R.id.widget_score, TypedValue.COMPLEX_UNIT_SP, if (compact || match.scoreLabel().length > 6) 18f else 24f)
                bindTeams(context, views, match, false)
                views.setOnClickPendingIntent(R.id.widget_match_content, WidgetIntents.open(context, id, match))
                views.setOnClickPendingIntent(R.id.widget_status, WidgetIntents.open(context, id, match))
                views.setOnClickPendingIntent(R.id.widget_competition, WidgetIntents.open(context, id, match))
                views.setOnClickPendingIntent(R.id.widget_open, WidgetIntents.open(context, id, match))
                views.setContentDescription(R.id.widget_match_content, description(match, now))
            }
        }
        return views
    }

    private fun row(context: Context, id: Int, match: WidgetMatch, now: Long, largeFont: Boolean): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_schedule_row)
        views.setTextViewText(R.id.widget_row_status, when {
            match.live -> if (largeFont) match.status else "● ${match.status}"
            match.terminal -> match.status
            else -> match.widgetTime(now)
        })
        views.textColor(context, R.id.widget_row_status, if (match.live) R.color.widget_accent else R.color.widget_text)
        views.setInt(R.id.widget_row_status, "setMaxLines", 1)
        views.setViewVisibility(R.id.widget_row_sport, View.VISIBLE)
        views.setTextViewText(R.id.widget_row_sport, match.sportLabel)
        views.setTextViewText(R.id.widget_row_score, match.scoreLabel())
        if (match.scoreLabel().length > 6) views.setTextViewTextSize(R.id.widget_row_score, TypedValue.COMPLEX_UNIT_SP, 12f)
        bindTeams(context, views, match, true)
        views.setContentDescription(R.id.widget_row, description(match, now))
        views.setOnClickPendingIntent(R.id.widget_row, WidgetIntents.open(context, id, match))
        return views
    }

    private fun WidgetMatch.widgetTime(now: Long): CharSequence {
        val time = clockLabel()
        val days = startDayOffset(now)
        val suffix = if (days > 0) "+$days" else if (days < 0) days.toString() else ""
        return SpannableString(time + suffix).apply {
            if (suffix.isNotEmpty()) {
                setSpan(SuperscriptSpan(), time.length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(RelativeSizeSpan(0.65f), time.length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun bindTeams(context: Context, views: RemoteViews, match: WidgetMatch, wide: Boolean) {
        val logos = if (wide) intArrayOf(R.id.widget_row_logo_0, R.id.widget_row_logo_1) else intArrayOf(R.id.widget_logo_0, R.id.widget_logo_1)
        val names = if (wide) intArrayOf(R.id.widget_row_team_0, R.id.widget_row_team_1) else intArrayOf(R.id.widget_team_0, R.id.widget_team_1)
        val fallbacks = if (wide) intArrayOf(R.id.widget_row_fallback_0, R.id.widget_row_fallback_1) else intArrayOf(R.id.widget_fallback_0, R.id.widget_fallback_1)
        val store = WidgetLogoStore(context)
        repeat(2) { index ->
            val team = match.teams.getOrNull(index)
            val name = team?.name?.takeIf(String::isNotBlank) ?: if (index == 0) match.name.ifBlank { "待定" } else "待定"
            val bitmap = store.cached(team?.logoUrl)
            views.setTextViewText(names[index], name)
            views.setTextViewText(fallbacks[index], name.take(1))
            views.setViewVisibility(fallbacks[index], if (bitmap == null) View.VISIBLE else View.GONE)
            views.setViewVisibility(logos[index], if (bitmap == null) View.GONE else View.VISIBLE)
            if (bitmap != null) views.setImageViewBitmap(logos[index], bitmap)
        }
    }

    private fun description(match: WidgetMatch, now: Long) =
        "${match.sportLabel}，${match.competition}，${match.dateTimeLabel(now)}，${match.teams.joinToString(" 对 ") { it.name }.ifBlank { match.name }}，${match.status}，${match.scoreLabel()}，查看${if (match.hasRatings) "比赛详情" else "赛程"}"

    private fun RemoteViews.textColor(context: Context, viewId: Int, color: Int) {
        if (Build.VERSION.SDK_INT >= 31) setColor(viewId, "setTextColor", color)
        else setTextColor(viewId, context.getColor(color))
    }

    private const val DAY = 24 * 60 * 60_000L
}

package dev.kiritoxd.miaopu.widget

/** Refresh feedback takes precedence over the single-match widget's sport label. */
internal fun widgetFooterText(
    refresh: WidgetRefreshStatus,
    failed: Boolean,
    online: Boolean,
    stale: Boolean,
    updated: String,
    sportLabel: String? = null,
): String = when {
    refresh == WidgetRefreshStatus.RUNNING -> "正在刷新"
    refresh == WidgetRefreshStatus.WAITING -> if (online) "等待刷新" else "等待联网"
    refresh == WidgetRefreshStatus.FAILED -> "刷新失败 · 点击重试"
    !online -> if (updated.isEmpty()) "联网后自动更新" else "离线 · 缓存 $updated"
    failed -> "刷新失败 · 点击重试"
    stale -> "缓存 $updated"
    sportLabel != null -> sportLabel
    updated.isNotEmpty() -> "$updated 更新"
    else -> "已订阅赛事"
}

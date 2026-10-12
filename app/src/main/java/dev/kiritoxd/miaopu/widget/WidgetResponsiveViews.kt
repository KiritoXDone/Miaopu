package dev.kiritoxd.miaopu.widget

import android.appwidget.AppWidgetManager
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import kotlin.math.roundToInt

/** Use the host's actual size pairs; its minimum width and height may belong to different orientations. */
internal fun responsiveWidgetViews(
    options: Bundle, wide: Boolean, create: (width: Int, height: Int) -> RemoteViews,
): RemoteViews {
    if (Build.VERSION.SDK_INT >= 31) {
        val sizes = if (Build.VERSION.SDK_INT >= 33) {
            options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java)
        } else {
            @Suppress("DEPRECATION")
            options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
        }
        val validSizes = sizes.orEmpty().filter {
            it.width.isFinite() && it.height.isFinite() && it.width > 0 && it.height > 0
        }.distinct().take(16)
        if (validSizes.isNotEmpty()) {
            return RemoteViews(validSizes.associateWith { create(it.width.roundToInt(), it.height.roundToInt()) })
        }
    }
    val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, if (wide) 340 else 170).coerceAtLeast(1)
    val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160).coerceAtLeast(1)
    val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidth).coerceAtLeast(minWidth)
    val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeight).coerceAtLeast(minHeight)
    return RemoteViews(create(maxWidth, minHeight), create(minWidth, maxHeight))
}

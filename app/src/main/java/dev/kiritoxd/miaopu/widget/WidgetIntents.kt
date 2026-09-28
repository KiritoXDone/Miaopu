package dev.kiritoxd.miaopu.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import dev.kiritoxd.miaopu.MainActivity

internal object WidgetIntents {
    const val OPEN = "dev.kiritoxd.miaopu.OPEN_WIDGET"
    const val REFRESH = "dev.kiritoxd.miaopu.REFRESH_WIDGET"
    const val BUSINESS_ID = "widget_business_id"
    const val MATCH_ID = "widget_match_id"

    fun open(context: Context, widgetId: Int, match: WidgetMatch? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = OPEN
            data = Uri.Builder().scheme("miaopu").authority("widget").appendPath(widgetId.toString())
                .appendPath(match?.businessId ?: "schedule").appendPath(match?.id ?: "all").build()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(BUSINESS_ID, match?.businessId)
            putExtra(MATCH_ID, match?.id)
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun refresh(context: Context, widgetId: Int): PendingIntent {
        val intent = Intent(context, MatchWidgetProvider::class.java).apply {
            action = REFRESH
            data = Uri.parse("miaopu://widget/refresh/$widgetId")
        }
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}

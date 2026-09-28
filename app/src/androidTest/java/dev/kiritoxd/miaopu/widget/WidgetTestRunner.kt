package dev.kiritoxd.miaopu.widget

import android.app.Activity
import android.app.Instrumentation
import android.content.res.Configuration
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import dev.kiritoxd.miaopu.R
import dev.kiritoxd.miaopu.data.Esport
import dev.kiritoxd.miaopu.data.EsportSubscriptionStore
import dev.kiritoxd.miaopu.data.Schedule
import dev.kiritoxd.miaopu.data.ScheduleDay
import java.io.File

/** Runs RemoteViews on Android itself, including night mode, compact sizes and large fonts. */
class WidgetTestRunner : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }

    override fun onStart() {
        val result = Bundle()
        try {
            var count = 0
            runOnMainSync {
                val now = System.currentTimeMillis()
                val match = WidgetMatch("sample", "lol", "T1 vs GEN", "全球总决赛", "进行中", "LIVE", now,
                    "20:00", listOf(WidgetTeam("T1", null, "1"), WidgetTeam("GEN", null, "1")), "lol", "sample", "sample")
                val matches = listOf(match, match.copy(id = "next", businessId = "val", status = "未开始", statusCode = null, startsAt = now + 86_400_000,
                    teams = listOf(WidgetTeam("EDG", null, null), WidgetTeam("PRX", null, null))),
                    match.copy(id = "done", businessId = "cs2", status = "已结束", statusCode = "COMPLETED", startsAt = now - 3_600_000,
                        teams = listOf(WidgetTeam("NAVI", null, "2"), WidgetTeam("Vitality", null, "0"))))
                verifyCache(match, now)
                val states = mapOf(
                    "content" to WidgetScheduleState(matches, now, false, true),
                    "empty" to WidgetScheduleState(emptyList(), now, false, true),
                    "error" to WidgetScheduleState(emptyList(), 0, true, true),
                    "loading" to WidgetScheduleState(emptyList(), 0, false, false),
                    "cached" to WidgetScheduleState(matches, now - 3_600_000, true, true),
                )
                for (night in listOf(false, true)) for (wide in listOf(false, true)) for (profile in listOf("normal", "large", "small")) {
                    val large = profile == "large"
                    val config = Configuration(targetContext.resources.configuration).apply {
                        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                            if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                        fontScale = if (large) 1.5f else 1f
                    }
                    val context = targetContext.createConfigurationContext(config)
                    val widthDp = if (wide) 340 else 170
                    val heightDp = if (large) 140 else if (profile == "small") 110 else 180
                    for ((name, state) in states) {
                        val remote = WidgetRenderer.create(context, 999, wide, state, now, heightDp, large)
                        val view = remote.apply(context, null)
                        val density = context.resources.displayMetrics.density
                        val width = (widthDp * density).toInt()
                        val height = (heightDp * density).toInt()
                        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                        view.layout(0, 0, width, height)
                        verifyTextBounds(view)
                        if (!wide) {
                            val card = view.findViewById<View>(R.id.widget_root)
                            check(kotlin.math.abs(card.width - card.height) <= 1) { "Single match card must be square" }
                        }
                        check(view.findViewById<TextView>(R.id.widget_footer).height > 0)
                        check(view.findViewById<View>(R.id.widget_empty).visibility == if (state.matches.isEmpty()) View.VISIBLE else View.GONE)
                        if (wide && state.matches.isNotEmpty()) {
                            val status = view.findViewById<TextView>(R.id.widget_row_status)
                            val sport = view.findViewById<TextView>(R.id.widget_row_sport)
                            check(sport.left >= status.right) { "Sport must follow the time horizontally" }
                            val rows = view.findViewById<ViewGroup>(R.id.widget_rows)
                            repeat(rows.childCount) { index ->
                                val label = rows.getChildAt(index).findViewById<TextView>(R.id.widget_row_sport)
                                check(label.left == sport.left) { "Sport labels must share the same left edge" }
                            }
                        }
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        view.draw(Canvas(bitmap))
                        val file = File(targetContext.getExternalFilesDir(null), "widget-${if (wide) "4x2" else "2x2"}-${if (night) "dark" else "light"}-${profile}-$name.png")
                        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        bitmap.recycle()
                        count++
                    }
                }
            }
            result.putString("stream", "PASS: $count RemoteViews layouts rendered across sizes, themes, font scales and data states.\n")
            finish(Activity.RESULT_OK, result)
        } catch (error: Throwable) {
            result.putString("stream", "FAIL: ${error.stackTraceToString()}\n")
            finish(Activity.RESULT_CANCELED, result)
        }
    }

    private fun verifyTextBounds(view: View) {
        if (view.visibility != View.VISIBLE) return
        if (view is TextView && view.text.isNotEmpty()) {
            val layout = view.layout
            check(layout == null || layout.height <= view.height - view.paddingTop - view.paddingBottom + 1) {
                "Clipped text: ${view.text}, layout=${layout?.height}, height=${view.height}"
            }
        }
        if (view is ViewGroup) repeat(view.childCount) {
            val child = view.getChildAt(it)
            if (child.visibility == View.VISIBLE) check(child.top >= -1 && child.bottom <= view.height + 1) {
                "Child exceeds container: ${child.javaClass.simpleName}, top=${child.top}, bottom=${child.bottom}, parentHeight=${view.height}"
            }
            verifyTextBounds(child)
        }
    }

    private fun verifyCache(match: WidgetMatch, now: Long) {
        val context = object : ContextWrapper(targetContext) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                super.getSharedPreferences("widget-test-$name", mode)
        }
        val prefs = listOf("miaopu_widget_schedules", "miaopu_esport_subscriptions")
        prefs.forEach { context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }
        try {
            val subscriptions = EsportSubscriptionStore(context)
            subscriptions.saveSubscriptions(setOf(Esport.LOL))
            val store = WidgetScheduleStore(context)
            val schedule = Schedule(null, listOf(ScheduleDay("", "", listOf(match.toModel()))))
            store.record(Esport.LOL, schedule, now)
            store.record(Esport.LOL, null, now + 1)
            check(store.state(now).failed)
            check(store.state(now).matches.single().id == match.id)
            store.record(Esport.LOL, Schedule(null, emptyList()), now - 1)
            check(store.state(now).matches.single().id == match.id) // Slow stale result rejected.
            check(WidgetScheduleStore(context).find("lol", match.id)?.hasRatings == true)
            subscriptions.saveSubscriptions(setOf(Esport.VALORANT))
            check(store.state(now).matches.isEmpty())
            check(store.find("lol", match.id) == null)
        } finally {
            prefs.forEach { context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }
        }
    }
}

package dev.kiritoxd.miaopu.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

// Lucide SVG geometry, revision 66d8f9fc394b8530377e5f6112f0b8908ba01280.
// Source: https://github.com/lucide-icons/lucide. License: assets/licenses/lucide.txt.
internal object LucideIcons {
    val ArrowDown: ImageVector by lazy { icon("ArrowDown", "M12 5v14", "m19 12-7 7-7-7") }
    val ArrowUp: ImageVector by lazy { icon("ArrowUp", "m5 12 7-7 7 7", "M12 19V5") }
    val CalendarDays: ImageVector by lazy { icon("CalendarDays", "M8 2v3", "M16 2v3", "M5.0 3.0h14.0a2.0 2.0 0 0 1 2.0 2.0v14.0a2.0 2.0 0 0 1 -2.0 2.0h-14.0a2.0 2.0 0 0 1 -2.0 -2.0v-14.0a2.0 2.0 0 0 1 2.0 -2.0z", "M3 9h18", "M8 13h.01", "M12 13h.01", "M16 13h.01", "M8 17h.01", "M12 17h.01", "M16 17h.01") }
    val Check: ImageVector by lazy { icon("Check", "M20 6 9 17l-5-5") }
    val ChevronLeft: ImageVector by lazy { icon("ChevronLeft", "m15 18-6-6 6-6") }
    val ChevronRight: ImageVector by lazy { icon("ChevronRight", "m9 18 6-6-6-6") }
    val CircleUserRound: ImageVector by lazy { icon("CircleUserRound", "M17.925 20.056a6 6 0 0 0-11.851.001", "M8.0 11.0a4.0 4.0 0 1 0 8.0 0a4.0 4.0 0 1 0 -8.0 0", "M2.0 12.0a10.0 10.0 0 1 0 20.0 0a10.0 10.0 0 1 0 -20.0 0") }
    val GripVertical: ImageVector by lazy { icon("GripVertical", "M8.0 12.0a1.0 1.0 0 1 0 2.0 0a1.0 1.0 0 1 0 -2.0 0", "M8.0 5.0a1.0 1.0 0 1 0 2.0 0a1.0 1.0 0 1 0 -2.0 0", "M8.0 19.0a1.0 1.0 0 1 0 2.0 0a1.0 1.0 0 1 0 -2.0 0", "M14.0 12.0a1.0 1.0 0 1 0 2.0 0a1.0 1.0 0 1 0 -2.0 0", "M14.0 5.0a1.0 1.0 0 1 0 2.0 0a1.0 1.0 0 1 0 -2.0 0", "M14.0 19.0a1.0 1.0 0 1 0 2.0 0a1.0 1.0 0 1 0 -2.0 0") }
    val House: ImageVector by lazy { icon("House", "M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8", "M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z") }
    val Plus: ImageVector by lazy { icon("Plus", "M5 12h14", "M12 5v14") }
    val RefreshCw: ImageVector by lazy { icon("RefreshCw", "M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8", "M21 3v5h-5", "M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16", "M8 16H3v5") }
    val Search: ImageVector by lazy { icon("Search", "m21 21-4.34-4.34", "M3.0 11.0a8.0 8.0 0 1 0 16.0 0a8.0 8.0 0 1 0 -16.0 0") }
    val Star: ImageVector by lazy { icon("Star", "M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 .294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z") }
    val StarFilled: ImageVector by lazy { icon("StarFilled", "M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 .294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z", filled = true) }
    val ThumbsUp: ImageVector by lazy { icon("ThumbsUp", "M15 5.88 14 10h5.83a2 2 0 0 1 1.92 2.56l-2.33 8A2 2 0 0 1 17.5 22H4a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2h2.76a2 2 0 0 0 1.79-1.11L12 2a3.13 3.13 0 0 1 3 3.88Z", "M7 10v12") }
    val X: ImageVector by lazy { icon("X", "M18 6 6 18", "m6 6 12 12") }

    private fun icon(name: String, vararg paths: String, filled: Boolean = false): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { data ->
                addPath(
                    pathData = PathParser().parsePathString(data).toNodes(),
                    fill = if (filled) SolidColor(Color.Black) else null,
                    stroke = SolidColor(Color.Black), strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
}

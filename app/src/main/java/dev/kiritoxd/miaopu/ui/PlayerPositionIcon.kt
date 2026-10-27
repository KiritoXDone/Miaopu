package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import dev.kiritoxd.miaopu.data.Esport
import dev.kiritoxd.miaopu.data.matchdetail.PlayerPosition
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun PlayerPositionIcon(position: PlayerPosition, game: Esport) {
    val label = when (position) {
        PlayerPosition.TOP -> if (game == Esport.KOG) "对抗路" else "上路"
        PlayerPosition.JUN -> "打野"
        PlayerPosition.MID -> "中路"
        PlayerPosition.BOT -> if (game == Esport.KOG) "发育路" else "下路"
        PlayerPosition.SUP -> if (game == Esport.KOG) "游走" else "辅助"
    }
    Icon(
        imageVector = PositionIcons[position],
        contentDescription = label,
        modifier = Modifier.size(16.dp),
        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    )
}

/** App-owned lane, jungle and support glyphs; colors follow the surrounding theme. */
private object PositionIcons {
    private val top = icon("TopLane", "M3 3H21L17 7H7V17L3 21Z M10 10H14V14H10Z")
    private val jungle = icon("Jungle", "M12 22C7 19 4 14 3 6C7 9 9 12 10 16C10 10 9 6 8 2C12 5 14 10 14 16C16 12 18 9 21 7C20 14 17 20 12 22Z")
    private val mid = icon("MidLane", "M3 17L17 3H21V7L7 21H3Z M3 3H10L3 10Z M14 21H21V14Z")
    private val bottom = icon("BottomLane", "M21 21H3L7 17H17V7L21 3Z M10 10H14V14H10Z")
    private val support = icon("Support", "M12 3L16 7L12 11L8 7Z M2 7L9 10V14L5 12Z M22 7L15 10V14L19 12Z M10 13H14L16 21H8Z")

    operator fun get(position: PlayerPosition): ImageVector = when (position) {
        PlayerPosition.TOP -> top
        PlayerPosition.JUN -> jungle
        PlayerPosition.MID -> mid
        PlayerPosition.BOT -> bottom
        PlayerPosition.SUP -> support
    }

    private fun icon(name: String, path: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            addPath(PathParser().parsePathString(path).toNodes(), fill = SolidColor(Color.Black))
        }.build()
}

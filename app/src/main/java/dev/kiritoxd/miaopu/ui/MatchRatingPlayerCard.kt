package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kiritoxd.miaopu.data.RatingTarget
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MatchRatingPlayerCard(target: RatingTarget, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), cornerRadius = 16.dp,
        insideMargin = PaddingValues(12.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RatingTargetPortrait(target, size = 48.dp, cornerRadius = 8.dp, championSize = 18.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(target.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    target.description?.takeIf(String::isNotBlank)?.let {
                        Text(it, modifier = Modifier.weight(1f, fill = false), fontSize = 11.sp,
                            color = colors.onSurfaceVariantSummary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    target.labels.firstOrNull()?.let {
                        Text(it, modifier = Modifier.background(colors.onSurface.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp), fontSize = 10.sp, color = colors.onSurfaceVariantSummary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(if (target.scoreCount > 0) "%.1f".format(target.scoreAverage) else "—",
                    fontSize = 23.sp, color = colors.primary, fontWeight = FontWeight.Bold)
                Text("${formatScoreCount(target.scoreCount)}人评分", fontSize = 10.sp, color = colors.onSurfaceVariantSummary)
            }
        }
        target.hotComment?.takeIf(String::isNotBlank)?.let {
            val dark = colors.surface.luminance() < 0.5f
            Text("“$it”", modifier = Modifier.padding(top = 7.dp).fillMaxWidth()
                .background(if (dark) Color(0xFF382F20) else Color(0xFFFFF5E3), RoundedCornerShape(7.dp))
                .padding(horizontal = 9.dp, vertical = 6.dp), fontSize = 11.sp,
                color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun formatScoreCount(count: Int): String = if (count >= 10_000) "%.1f万".format(count / 10_000.0) else count.toString()

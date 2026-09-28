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
import coil3.compose.AsyncImage
import dev.kiritoxd.miaopu.data.matchdetail.MatchAllScores
import dev.kiritoxd.miaopu.data.matchdetail.PlayerAllScore
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun AllMatchScoreCard(scores: MatchAllScores) {
    if (scores.teams.all { it.players.isEmpty() }) {
        DetailNotice("全场评分暂未公布")
        return
    }
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), cornerRadius = 22.dp,
        insideMargin = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("全场评分", style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold)
            // Do not pair by player name: the endpoint provides independently sorted team lists.
            scores.teams.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { team ->
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (team.logoUrl != null) AsyncImage(team.logoUrl, contentDescription = null, modifier = Modifier.size(20.dp))
                                Text(team.name, style = MiuixTheme.textStyles.footnote1, fontWeight = FontWeight.SemiBold,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            }
                            team.players.forEach { player -> ScorePlayerRow(player) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScorePlayerRow(player: PlayerAllScore) {
    val dark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    val hex = if (dark) player.nightColor else player.dayColor
    val color = hex?.takeIf { Regex("#[0-9a-fA-F]{6}").matches(it) }
        ?.removePrefix("#")?.toLongOrNull(16)?.let { Color((it or 0xff000000L).toInt()) }
        ?: MiuixTheme.colorScheme.primary
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(player.name, modifier = Modifier.weight(1f), style = MiuixTheme.textStyles.footnote1,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.background(color.copy(alpha = 0.10f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 5.dp)) {
            Text(player.score, color = color, style = MiuixTheme.textStyles.body2, fontWeight = FontWeight.Bold)
        }
    }
}

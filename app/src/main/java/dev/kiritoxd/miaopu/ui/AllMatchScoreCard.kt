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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.kiritoxd.miaopu.data.matchdetail.*
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun AllMatchScoreCard(scores: MatchAllScores) {
    if (!scores.hasScores) return
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), cornerRadius = 18.dp,
        insideMargin = PaddingValues(12.dp)) {
        Text("全场评分", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        scores.teams.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                ScoreTeamHeading(pair.first(), false)
                pair.getOrNull(1)?.let { ScoreTeamHeading(it, true) }
            }
            repeat(pair.maxOf { it.players.size }) { index ->
                if (index > 0) Box(Modifier.fillMaxWidth().height(0.5.dp)
                    .background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.06f)))
                Row(Modifier.fillMaxWidth().heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                    ScorePlayer(pair.first().players.getOrNull(index), false, Modifier.weight(1f))
                    Text("Ⅰ", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center,
                        fontSize = 10.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    ScorePlayer(pair.getOrNull(1)?.players?.getOrNull(index), true, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ScoreTeamHeading(team: ScoredTeam, right: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!right && team.logoUrl != null) AsyncImage(team.logoUrl, null, Modifier.size(22.dp))
        Text(team.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        if (right && team.logoUrl != null) AsyncImage(team.logoUrl, null, Modifier.size(22.dp))
    }
}

@Composable
private fun ScorePlayer(player: PlayerAllScore?, right: Boolean, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (right) ScoreBadge(player)
        Text(player?.name.orEmpty(), Modifier.weight(1f), fontSize = 13.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = if (right) TextAlign.End else TextAlign.Start)
        if (!right) ScoreBadge(player)
    }
}

@Composable
private fun ScoreBadge(player: PlayerAllScore?) {
    val dark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    val hex = if (dark) player?.nightColor else player?.dayColor
    val color = hex?.takeIf { Regex("#[0-9a-fA-F]{6}").matches(it) }
        ?.removePrefix("#")?.toLongOrNull(16)?.let { Color((it or 0xff000000L).toInt()) }
        ?: if ((player?.score?.toDoubleOrNull() ?: 0.0) >= 6.0) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.primary
    Box(Modifier.width(44.dp).background(color.copy(alpha = if (player == null) 0f else 0.1f), RoundedCornerShape(6.dp))
        .padding(vertical = 3.dp), contentAlignment = Alignment.Center) {
        Text(player?.score.orEmpty(), color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

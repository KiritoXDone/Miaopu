package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kiritoxd.miaopu.data.MatchSummary
import dev.kiritoxd.miaopu.data.Team
import dev.kiritoxd.miaopu.data.matchdetail.MatchLiveScore
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MatchHero(match: MatchSummary, liveScore: MatchLiveScore? = null) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        cornerRadius = 18.dp, insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(match.introduction.ifBlank { match.name }, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Text(listOf(match.name.takeUnless { it == match.introduction }.orEmpty(), match.status)
                .filter(String::isNotBlank).joinToString(" · "), fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 2,
                overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            if (match.teams.size == 2) Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                HeroTeam(match.teams.getOrNull(0), Modifier.weight(1f))
                Column(Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                    liveScore?.currentMap?.let {
                        Text(it, fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                    Text(overallMatchScore(match) ?: "VS", fontSize = 36.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
                    liveScore?.currentScore?.let {
                        Text("当前比分 $it", fontSize = 12.sp, color = MiuixTheme.colorScheme.primary)
                    }
                }
                HeroTeam(match.teams.getOrNull(1), Modifier.weight(1f))
            } else {
                Text(match.teams.joinToString(" · ") { it.name }.ifBlank { match.esport.title },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), fontSize = 20.sp,
                    textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HeroTeam(team: Team?, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (team != null) TeamLogo(team, 40.dp)
        Text(team?.name ?: "待定", fontSize = 14.sp, fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

internal fun overallMatchScore(match: MatchSummary): String? {
    if (match.teams.size != 2) return null
    val bigScores = match.teams.map { it.bigScore?.takeIf(String::isNotBlank) }
    val scores = bigScores.takeIf { values -> values.all { it != null } }
        ?: match.teams.map { it.score?.takeIf(String::isNotBlank) }
    return scores.takeIf { values -> values.all { it != null } }?.joinToString(" : ") { it.orEmpty() }
}

package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.kiritoxd.miaopu.data.MatchSummary
import dev.kiritoxd.miaopu.data.Team
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MatchHero(match: MatchSummary) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp),
        cornerRadius = 24.dp, insideMargin = PaddingValues(18.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(match.name, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (match.teams.size == 2) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    HeroTeam(match.teams.getOrNull(0), Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(overallMatchScore(match) ?: "VS", style = MiuixTheme.textStyles.title1,
                            fontWeight = FontWeight.Bold, maxLines = 1)
                        Spacer(Modifier.height(6.dp))
                        TagPill(match.status)
                    }
                    HeroTeam(match.teams.getOrNull(1), Modifier.weight(1f))
                }
            } else {
                Text(match.teams.joinToString(" · ") { it.name }.ifBlank { match.esport.title },
                    style = MiuixTheme.textStyles.title3, textAlign = TextAlign.Center)
                TagPill(match.status)
            }
            Text(listOf(match.introduction, match.startTimeLabel).filter(String::isNotBlank).distinct().joinToString(" · "),
                style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun HeroTeam(team: Team?, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (team != null) TeamLogo(team, 52.dp)
        Text(team?.name ?: "待定", style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.SemiBold,
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

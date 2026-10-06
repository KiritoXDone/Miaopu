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
import coil3.compose.AsyncImage
import dev.kiritoxd.miaopu.data.matchdetail.StatsTeam
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text

/** Scores belong to the selected stats map, independently of the live match header. */
@Composable
internal fun MatchMapScoreCard(teams: List<StatsTeam>) {
    if (teams.size != 2) return
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), cornerRadius = 16.dp,
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MapScoreTeam(teams[0], Modifier.weight(1f), Arrangement.Start)
            Text("${teams[0].score?.takeIf(String::isNotBlank) ?: "—"} : ${teams[1].score?.takeIf(String::isNotBlank) ?: "—"}",
                modifier = Modifier.widthIn(max = 110.dp), fontSize = 24.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            MapScoreTeam(teams[1], Modifier.weight(1f), Arrangement.End)
        }
    }
}

@Composable
private fun MapScoreTeam(team: StatsTeam, modifier: Modifier, arrangement: Arrangement.Horizontal) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = arrangement) {
        team.logoUrl?.let { AsyncImage(it, null, Modifier.size(28.dp)) }
        Spacer(Modifier.width(6.dp))
        Text(team.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

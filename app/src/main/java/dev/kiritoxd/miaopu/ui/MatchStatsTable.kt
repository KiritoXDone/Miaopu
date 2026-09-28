package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import coil3.compose.AsyncImage
import dev.kiritoxd.miaopu.data.matchdetail.StatsTeam
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MatchStatsTable(team: StatsTeam) {
    val scroll = rememberScrollState()
    val rowHeight = with(LocalDensity.current) { 52.sp.toDp() }.coerceAtLeast(52.dp)
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), cornerRadius = 22.dp,
        insideMargin = PaddingValues(0.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (team.logoUrl != null) AsyncImage(team.logoUrl, contentDescription = null, modifier = Modifier.size(26.dp))
            Text(team.name, modifier = Modifier.weight(1f), style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold)
            team.score?.let { Text(it, style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold) }
        }
        Row(Modifier.fillMaxWidth()) {
            // Only the stats column scrolls, keeping player names visible at every offset.
            Column(Modifier.width(124.dp)) {
                Box(Modifier.fillMaxWidth().height(rowHeight).background(MiuixTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.CenterStart) {
                    Text("选手", modifier = Modifier.padding(start = 16.dp), style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
                team.players.forEachIndexed { index, cells ->
                    Row(Modifier.fillMaxWidth().height(rowHeight).background(tableRowColor(index)).padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val player = cells.firstOrNull()
                        if (player?.imageUrl != null) AsyncImage(player.imageUrl, contentDescription = null, modifier = Modifier.size(24.dp))
                        Text(player?.text ?: "—", maxLines = 2, overflow = TextOverflow.Ellipsis,
                            style = MiuixTheme.textStyles.footnote2, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Column(Modifier.weight(1f).horizontalScroll(scroll)) {
                Row(Modifier.height(rowHeight).background(MiuixTheme.colorScheme.surfaceVariant)) {
                    team.columns.drop(1).forEach { column -> StatText(column, header = true) }
                }
                team.players.forEachIndexed { index, cells ->
                    Row(Modifier.height(rowHeight).background(tableRowColor(index))) {
                        cells.drop(1).forEach { cell -> StatText(cell.text, header = false) }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StatText(value: String, header: Boolean) {
    Box(Modifier.width(88.dp).fillMaxHeight().padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
        Text(value, textAlign = TextAlign.Center, style = MiuixTheme.textStyles.footnote1,
            fontWeight = if (header) FontWeight.Normal else FontWeight.Medium,
            color = if (header) MiuixTheme.colorScheme.onSurfaceVariantSummary else MiuixTheme.colorScheme.onSurface,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun tableRowColor(index: Int) = if (index % 2 == 0) MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    else MiuixTheme.colorScheme.surfaceContainer

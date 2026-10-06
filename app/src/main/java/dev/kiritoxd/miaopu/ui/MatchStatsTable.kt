package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import coil3.compose.AsyncImage
import dev.kiritoxd.miaopu.data.matchdetail.StatsTeam
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MatchStatsTable(team: StatsTeam, scroll: ScrollState = rememberScrollState()) {
    val colors = MiuixTheme.colorScheme
    val density = LocalDensity.current
    val rowHeight = with(density) { 40.sp.toDp() }.coerceAtLeast(44.dp)
    val headerHeight = with(density) { 36.sp.toDp() }.coerceAtLeast(40.dp)
    val nameWidth = with(density) { 112.sp.toDp() }.coerceAtLeast(112.dp)
    val minCellWidth = with(density) { 54.sp.toDp() }.coerceAtLeast(54.dp)
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), cornerRadius = 14.dp,
        insideMargin = PaddingValues(0.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val firstColumnWidth = nameWidth.coerceAtMost(maxWidth * 0.45f)
            val columnCount = (team.columns.size - 1).coerceAtLeast(1)
            val cellWidth = ((maxWidth - firstColumnWidth) / columnCount).coerceAtLeast(minCellWidth)
            val headerColor = colors.primary.copy(alpha = 0.045f)
            Row(Modifier.fillMaxWidth()) {
                // Keep the team header and player identities visible while numeric columns scroll.
                Column(Modifier.width(firstColumnWidth)) {
                    Row(Modifier.fillMaxWidth().height(headerHeight).background(headerColor)
                        .padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        team.logoUrl?.let { AsyncImage(it, null, Modifier.size(24.dp)) }
                        Text(team.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 2, lineHeight = 14.sp, overflow = TextOverflow.Ellipsis)
                    }
                    team.players.forEach { cells ->
                        Row(Modifier.fillMaxWidth().height(rowHeight).padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val player = cells.firstOrNull()
                            player?.imageUrl?.let { AsyncImage(it, null, Modifier.size(28.dp).clip(CircleShape)) }
                            Text(player?.text ?: "—", maxLines = 2, overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp, lineHeight = 14.sp)
                        }
                        TableDivider()
                    }
                }
                Column(Modifier.weight(1f).horizontalScroll(scroll)) {
                    Row(Modifier.height(headerHeight).background(headerColor)) {
                        team.columns.drop(1).forEach { column -> StatText(column, header = true, width = cellWidth) }
                    }
                    team.players.forEach { cells ->
                        Row(Modifier.height(rowHeight)) {
                            team.columns.drop(1).forEachIndexed { index, _ ->
                                StatText(cells.getOrNull(index + 1)?.text ?: "—", header = false, width = cellWidth)
                            }
                        }
                        TableDivider(Modifier.width(cellWidth * (team.columns.size - 1).coerceAtLeast(0)))
                    }
                }
            }
        }
    }
}

@Composable
private fun TableDivider(modifier: Modifier = Modifier.fillMaxWidth()) {
    Box(modifier.height(0.5.dp).background(MiuixTheme.colorScheme.dividerLine.copy(alpha = 0.5f)))
}

@Composable
private fun StatText(value: String, header: Boolean, width: Dp) {
    Box(Modifier.width(width).fillMaxHeight().padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
        Text(value, textAlign = TextAlign.Center, fontSize = 12.sp, lineHeight = 14.sp,
            fontWeight = if (header) FontWeight.Normal else FontWeight.Medium,
            color = if (header) MiuixTheme.colorScheme.onSurfaceVariantSummary else MiuixTheme.colorScheme.onSurface,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

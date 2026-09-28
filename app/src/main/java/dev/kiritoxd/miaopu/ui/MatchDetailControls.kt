package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal enum class DetailTabStyle { PAGE, MAP, TEAM }

/** Compact segmented controls follow the match-detail design, with native tab semantics. */
@Composable
internal fun DetailTabs(
    labels: List<String>, selected: Int, style: DetailTabStyle = DetailTabStyle.MAP,
    logos: List<String?> = emptyList(), onSelect: (Int) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val connected = style != DetailTabStyle.MAP
    val overflowing = labels.size > 3
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).selectableGroup()
        .then(if (overflowing) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
        .clip(RoundedCornerShape(50)).background(if (connected) colors.onSurface.copy(alpha = 0.055f) else Color.Transparent),
        horizontalArrangement = Arrangement.spacedBy(if (connected) 2.dp else 8.dp)) {
        labels.forEachIndexed { index, label ->
            val active = selected == index
            val background = when {
                active && style == DetailTabStyle.PAGE -> colors.primary
                active && style == DetailTabStyle.TEAM -> colors.surfaceContainer
                active -> colors.primary.copy(alpha = 0.14f)
                connected -> Color.Transparent
                else -> colors.onSurface.copy(alpha = 0.045f)
            }
            val foreground = when {
                active && style == DetailTabStyle.PAGE -> colors.onPrimary
                active && style == DetailTabStyle.MAP -> colors.primary
                else -> colors.onSurface
            }
            Row((if (overflowing) Modifier.width(96.dp) else Modifier.weight(1f)).heightIn(min = if (style == DetailTabStyle.MAP) 32.dp else 36.dp)
                .clip(RoundedCornerShape(50)).background(background)
                .selectable(active, role = Role.Tab, onClick = { onSelect(index) }).padding(horizontal = 6.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                logos.getOrNull(index)?.let { AsyncImage(it, null, Modifier.size(22.dp)); Spacer(Modifier.width(6.dp)) }
                Text(label, color = foreground, fontSize = 13.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
internal fun DetailOrderSelector(selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).selectableGroup(), horizontalArrangement = Arrangement.End) {
        StageTargetOrder.entries.forEachIndexed { index, order ->
            Text(order.label, modifier = Modifier.clip(RoundedCornerShape(8.dp))
                .selectable(index == selected, role = Role.Tab, onClick = { onSelect(index) })
                .padding(horizontal = 9.dp, vertical = 4.dp), fontSize = 11.sp,
                color = if (index == selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    }
}

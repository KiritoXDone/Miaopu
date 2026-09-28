package dev.kiritoxd.miaopu.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
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
    if (labels.isEmpty()) return
    val colors = MiuixTheme.colorScheme
    val connected = style != DetailTabStyle.MAP
    val overflowing = labels.size > 3
    val selectedIndex = selected.coerceIn(labels.indices)
    val spacing = if (connected) 2.dp else 8.dp
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        val tabWidth = if (overflowing) 96.dp else (maxWidth - spacing * (labels.size - 1)) / labels.size
        val contentWidth = if (overflowing) tabWidth * labels.size + spacing * (labels.size - 1) else maxWidth
        val indicatorOffset by animateDpAsState(
            targetValue = (tabWidth + spacing) * selectedIndex,
            animationSpec = tween(240, easing = FastOutSlowInEasing), label = "detailTabIndicator",
        )
        val viewportWidth = maxWidth
        LaunchedEffect(selectedIndex, tabWidth, viewportWidth, scroll.maxValue) {
            if (overflowing) {
                val target = with(density) { ((tabWidth + spacing) * selectedIndex - (viewportWidth - tabWidth) / 2).roundToPx() }
                scroll.animateScrollTo(target.coerceIn(0, scroll.maxValue), tween(240, easing = FastOutSlowInEasing))
            }
        }
        Box(Modifier.fillMaxWidth().then(if (overflowing) Modifier.horizontalScroll(scroll) else Modifier)) {
            Box(Modifier.width(contentWidth).clip(RoundedCornerShape(50))
                .background(if (connected) colors.onSurface.copy(alpha = 0.055f) else Color.Transparent)) {
                // A single shared indicator moves behind the labels, including during interrupted transitions.
                if (connected) Box(Modifier.matchParentSize()) {
                    Box(Modifier.offset { IntOffset(with(density) { indicatorOffset.roundToPx() }, 0) }
                        .width(tabWidth).fillMaxHeight().clip(RoundedCornerShape(50))
                        .background(if (style == DetailTabStyle.PAGE) colors.primary else colors.surfaceContainer))
                }
                Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    labels.forEachIndexed { index, label ->
                        val active = selectedIndex == index
                        val background by animateColorAsState(
                            if (connected) Color.Transparent else if (active) colors.primary.copy(alpha = 0.14f)
                            else colors.onSurface.copy(alpha = 0.045f), tween(240), label = "detailTabBackground",
                        )
                        val foreground by animateColorAsState(
                            when {
                                active && style == DetailTabStyle.PAGE -> colors.onPrimary
                                active && style == DetailTabStyle.MAP -> colors.primary
                                else -> colors.onSurface
                            }, tween(240), label = "detailTabText",
                        )
                        Row(Modifier.width(tabWidth).heightIn(min = if (style == DetailTabStyle.MAP) 32.dp else 36.dp)
                            .clip(RoundedCornerShape(50)).background(background)
                            .selectable(active, role = Role.Tab, onClick = { onSelect(index) })
                            .padding(horizontal = 6.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            logos.getOrNull(index)?.let { AsyncImage(it, null, Modifier.size(22.dp)); Spacer(Modifier.width(6.dp)) }
                            Text(label, color = foreground, fontSize = 13.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
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

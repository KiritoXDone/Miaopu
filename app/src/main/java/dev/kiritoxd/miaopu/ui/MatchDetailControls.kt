package dev.kiritoxd.miaopu.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal const val TabMotionDurationMillis = 320
internal val TabMotionEasing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

internal enum class DetailTabStyle { PAGE, MAP, TEAM }

/** The approved layout uses text-width map tabs and equal-width page/team controls. */
@Composable
internal fun DetailTabs(
    labels: List<String>, selected: Int, style: DetailTabStyle = DetailTabStyle.MAP,
    logos: List<String?> = emptyList(), onSelect: (Int) -> Unit,
) {
    if (labels.isEmpty()) return
    val selectedIndex = selected.coerceIn(labels.indices)
    if (style == DetailTabStyle.MAP) {
        MapTabs(labels, selectedIndex, onSelect)
        return
    }
    val colors = MiuixTheme.colorScheme
    val isPage = style == DetailTabStyle.PAGE
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val indicatorPosition = animateFloatAsState(selectedIndex.toFloat(),
        tween(TabMotionDurationMillis, easing = TabMotionEasing), label = "detailTabIndicator")
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        .then(if (isPage) Modifier else Modifier.clip(RoundedCornerShape(16.dp))
            .background(colors.onSurface.copy(alpha = 0.055f)).padding(4.dp))) {
        val tabWidth = maxWidth / labels.size
        val stride = with(density) { tabWidth.toPx() }
        Box(Modifier.matchParentSize()) {
            if (isPage) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(1.dp)
                .background(colors.dividerLine))
            Box(Modifier.align(if (isPage) Alignment.BottomStart else Alignment.TopStart)
                .graphicsLayer { translationX = indicatorPosition.value * stride * if (rtl) -1f else 1f }
                .width(tabWidth).then(if (isPage) Modifier.height(4.dp) else Modifier.fillMaxHeight()),
                contentAlignment = Alignment.Center) {
                Box(Modifier.then(if (isPage) Modifier.width(30.dp) else Modifier.fillMaxWidth())
                    .fillMaxHeight().clip(RoundedCornerShape(if (isPage) 2.dp else 12.dp))
                    .background(if (isPage) colors.primary else colors.surfaceContainer))
            }
        }
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            labels.forEachIndexed { index, label ->
                val active = index == selectedIndex
                val foreground by animateColorAsState(
                    if (active) colors.onSurface else colors.onSurfaceVariantSummary,
                    tween(TabMotionDurationMillis, easing = TabMotionEasing), label = "detailTabText")
                Row(Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                    .selectable(active, role = Role.Tab, onClick = { if (!active) onSelect(index) })
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    logos.getOrNull(index)?.let { logo ->
                        AsyncImage(logo, null, Modifier.size(26.dp))
                        Spacer(Modifier.width(7.dp))
                    }
                    Text(label, color = foreground, fontSize = if (isPage) 17.sp else 14.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun MapTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val colors = MiuixTheme.colorScheme
    val listState = rememberLazyListState()
    LaunchedEffect(selected, labels) {
        if (listState.layoutInfo.visibleItemsInfo.none { it.index == selected }) {
            listState.animateScrollToItem(selected)
        }
    }
    LazyRow(state = listState, modifier = Modifier.fillMaxWidth().selectableGroup(),
        contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(labels) { index, label ->
            val active = index == selected
            val background by animateColorAsState(
                if (active) colors.primary.copy(alpha = 0.1f) else colors.onSurface.copy(alpha = 0.045f),
                tween(TabMotionDurationMillis, easing = TabMotionEasing), label = "mapTabBackground")
            Box(Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(10.dp))
                .background(background).border(1.dp, if (active) colors.primary else Color.Transparent, RoundedCornerShape(10.dp))
                .selectable(active, role = Role.Tab, onClick = { if (!active) onSelect(index) })
                .padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text(label, color = if (active) colors.primary else colors.onSurfaceVariantSummary,
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@Composable
internal fun DetailOrderSelector(
    selected: Int, labels: List<String> = StageTargetOrder.entries.map { it.label },
    onSelect: (Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).selectableGroup(), horizontalArrangement = Arrangement.End) {
        labels.forEachIndexed { index, label ->
            Text(label, modifier = Modifier.clip(RoundedCornerShape(8.dp))
                .selectable(index == selected, role = Role.Tab, onClick = { onSelect(index) })
                .padding(horizontal = 9.dp, vertical = 4.dp), fontSize = 11.sp,
                color = if (index == selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    }
}

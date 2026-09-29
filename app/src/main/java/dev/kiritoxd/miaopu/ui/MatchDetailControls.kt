package dev.kiritoxd.miaopu.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

// Nagram FilterTabsView: selection position and text colors share a 320 ms ease-out curve.
internal const val TabMotionDurationMillis = 320
internal val TabMotionEasing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

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
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        val tabWidth = if (overflowing) 96.dp else (maxWidth - spacing * (labels.size - 1)) / labels.size
        val contentWidth = if (overflowing) tabWidth * labels.size + spacing * (labels.size - 1) else maxWidth
        // Read only in graphicsLayer: progress never recomposes or remeasures the tab row.
        val indicatorPosition = animateFloatAsState(
            targetValue = selectedIndex.toFloat(),
            animationSpec = tween(TabMotionDurationMillis, easing = TabMotionEasing), label = "detailTabIndicator",
        )
        val stridePx = with(density) { (tabWidth + spacing).toPx() }
        val viewportWidth = maxWidth
        LaunchedEffect(selectedIndex, tabWidth, viewportWidth, scroll.maxValue) {
            if (overflowing) {
                val target = with(density) { ((tabWidth + spacing) * selectedIndex - (viewportWidth - tabWidth) / 2).roundToPx() }
                scroll.animateScrollTo(target.coerceIn(0, scroll.maxValue), tween(TabMotionDurationMillis, easing = TabMotionEasing))
            }
        }
        Box(Modifier.fillMaxWidth().then(if (overflowing) Modifier.horizontalScroll(scroll) else Modifier)) {
            Box(Modifier.width(contentWidth).clip(RoundedCornerShape(50))
                .background(if (connected) colors.onSurface.copy(alpha = 0.055f) else Color.Transparent)) {
                // A single shared indicator moves behind the labels, including during interrupted transitions.
                if (connected) Box(Modifier.matchParentSize()) {
                    Box(Modifier.graphicsLayer {
                        translationX = indicatorPosition.value * stridePx * if (rtl) -1f else 1f
                    }.width(tabWidth).fillMaxHeight().clip(RoundedCornerShape(50))
                        .background(if (style == DetailTabStyle.PAGE) colors.primary else colors.surfaceContainer))
                }
                Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    labels.forEachIndexed { index, label ->
                        DetailTabItem(label, logos.getOrNull(index), index == selectedIndex, style, tabWidth) {
                            if (index != selectedIndex) onSelect(index)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailTabItem(
    label: String, logo: String?, active: Boolean, style: DetailTabStyle,
    width: androidx.compose.ui.unit.Dp, onSelect: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val connected = style != DetailTabStyle.MAP
    val background by animateColorAsState(
        if (connected) Color.Transparent else if (active) colors.primary.copy(alpha = 0.14f)
        else colors.onSurface.copy(alpha = 0.045f), tween(TabMotionDurationMillis, easing = TabMotionEasing), label = "detailTabBackground",
    )
    val foreground by animateColorAsState(
        when {
            active && style == DetailTabStyle.PAGE -> colors.onPrimary
            active && style == DetailTabStyle.MAP -> colors.primary
            else -> colors.onSurface
        }, tween(TabMotionDurationMillis, easing = TabMotionEasing), label = "detailTabText",
    )
    Row(Modifier.width(width).heightIn(min = if (connected) 36.dp else 32.dp)
        .clip(RoundedCornerShape(50)).background(background)
        .selectable(active, interactionSource = null, indication = null, role = Role.Tab, onClick = onSelect)
        .padding(horizontal = 6.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        if (logo != null) {
            AsyncImage(logo, null, Modifier.size(22.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, color = foreground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
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

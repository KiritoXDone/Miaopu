package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kiritoxd.miaopu.data.Esport
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SubscriptionGroup(
    sports: List<Esport>, subscribed: List<Esport>, sorting: Boolean = false,
    onToggle: (Esport) -> Unit, onMove: (Esport, Int) -> Unit = { _, _ -> },
) {
    if (sports.isEmpty()) return
    val currentMove by rememberUpdatedState(onMove)
    val rowPixels = with(LocalDensity.current) { 72.dp.toPx() }
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), cornerRadius = 18.dp, insideMargin = PaddingValues(horizontal = 12.dp)) {
        sports.forEachIndexed { index, sport -> key(sport.businessId) {
            if (index > 0) Box(Modifier.fillMaxWidth().height(0.5.dp).background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.06f)))
            Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(MiuixTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Text(sport.shortTitle, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MiuixTheme.colorScheme.primary, maxLines = 1)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(sport.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(sport.shortTitle, fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
                if (sorting) {
                    IconButton(onClick = { onMove(sport, -1) }, enabled = index > 0, minWidth = 28.dp, minHeight = 40.dp,
                        modifier = Modifier.semantics { contentDescription = "上移${sport.title}" }) { Icon(LucideIcons.ArrowUp, null, Modifier.size(18.dp)) }
                    IconButton(onClick = { onMove(sport, 1) }, enabled = index < sports.lastIndex, minWidth = 28.dp, minHeight = 40.dp,
                        modifier = Modifier.semantics { contentDescription = "下移${sport.title}" }) { Icon(LucideIcons.ArrowDown, null, Modifier.size(18.dp)) }
                    Box(Modifier.size(36.dp).semantics { contentDescription = "长按拖动${sport.title}排序" }
                        .pointerInput(sport, rowPixels) {
                            var distance = 0f
                            detectDragGesturesAfterLongPress(onDragStart = { distance = 0f }, onDrag = { change, amount ->
                                change.consume(); distance += amount.y
                                if (distance >= rowPixels) { currentMove(sport, 1); distance -= rowPixels }
                                if (distance <= -rowPixels) { currentMove(sport, -1); distance += rowPixels }
                            })
                        }, contentAlignment = Alignment.Center) { Icon(LucideIcons.GripVertical, null, Modifier.size(20.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantSummary) }
                } else {
                    val active = sport in subscribed
                    IconButton(onClick = { onToggle(sport) }, modifier = Modifier.semantics {
                        contentDescription = "${if (active) "取消订阅" else "订阅"}${sport.title}"
                    }) {
                        Box(Modifier.size(24.dp).background(if (active) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(if (active) LucideIcons.Check else LucideIcons.Plus, null, Modifier.size(18.dp),
                                tint = if (active) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.primary)
                        }
                    }
                }
            }
        } }
    }
}

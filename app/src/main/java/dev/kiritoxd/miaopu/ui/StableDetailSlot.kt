package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Owned by the match page, so lazy disposal and loading placeholders cannot reset layout. */
internal class DetailLayoutState {
    val heights = mutableMapOf<String, Int>()
    private val tableScrolls = mutableMapOf<String, ScrollState>()
    fun tableScroll(key: String): ScrollState = tableScrolls.getOrPut(key) { ScrollState(0) }
}

/** Keep each lazy item in place without displaying or allowing clicks on the previous map's data. */
@Composable
internal fun StableDetailSlot(
    pending: Boolean,
    layout: DetailLayoutState,
    slotKey: String,
    notice: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val measuredHeight = layout.heights[slotKey] ?: 0
    val density = LocalDensity.current
    if (pending) {
        val height = if (measuredHeight > 0) with(density) { measuredHeight.toDp() } else 112.dp
        Box(
            Modifier.fillMaxWidth().height(height).padding(horizontal = 16.dp)
                .background(MiuixTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) { notice?.invoke() }
    } else {
        Box(Modifier.fillMaxWidth().onSizeChanged { layout.heights[slotKey] = it.height }) { content() }
    }
}

@Composable
internal fun StageLoadNotice(state: LoadState<*>, onRetry: () -> Unit) {
    DetailLoadNotice(state, "正在加载这一局的评分", onRetry)
}

@Composable
internal fun StatsLoadNotice(state: LoadState<*>?, onRetry: () -> Unit) {
    DetailLoadNotice(state, "正在加载比赛数据", onRetry)
}

@Composable
private fun DetailLoadNotice(state: LoadState<*>?, label: String, onRetry: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state !is LoadState.Failed) CircularProgressIndicator(modifier = Modifier.size(18.dp))
        Text(
            text = (state as? LoadState.Failed)?.message ?: label,
            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        if (state is LoadState.Failed && state.retryable) TextButton(text = "重试", onClick = onRetry)
    }
}

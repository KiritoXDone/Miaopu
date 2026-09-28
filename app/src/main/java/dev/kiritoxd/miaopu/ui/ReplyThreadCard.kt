package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import dev.kiritoxd.miaopu.data.CommentThreadRow
import dev.kiritoxd.miaopu.data.HupuComment
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ReplyThreadCard(
    row: CommentThreadRow, root: HupuComment, actions: CommentActionsController,
    selected: Boolean, onLike: () -> Unit, onReply: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val rail = colors.onSurfaceVariantSummary.copy(alpha = 0.25f)
    val depth = row.depth.coerceAtMost(3)
    Box(Modifier.fillMaxWidth().background(if (selected) colors.primary.copy(alpha = 0.08f) else Color.Transparent)
        .clickable(onClick = onReply)) {
        Canvas(Modifier.matchParentSize()) {
            val step = 22.dp.toPx()
            val start = 16.dp.toPx()
            val center = 31.dp.toPx().coerceAtMost(size.height)
            row.ancestorContinues.take(depth - 1).forEachIndexed { index, continues ->
                if (continues) drawLine(rail, Offset(start + step * index, 0f), Offset(start + step * index, size.height), 1.dp.toPx())
            }
            val x = start + step * (depth - 1)
            drawLine(rail, Offset(x, 0f), Offset(x, if (row.hasNextSibling) size.height else center), 1.dp.toPx(), StrokeCap.Round)
            drawLine(rail, Offset(x, center), Offset(x + 12.dp.toPx(), center), 1.dp.toPx(), StrokeCap.Round)
        }
        Box(Modifier.fillMaxWidth().padding(start = (16 + depth * 22).dp, end = 16.dp, top = 14.dp, bottom = 14.dp)) {
            CommentRow(row.comment, root, actions, onLike, onReply, metadataAbove = true)
        }
    }
}

package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import dev.kiritoxd.miaopu.data.commentThreadRows
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kiritoxd.miaopu.data.HupuComment
import dev.kiritoxd.miaopu.data.RatingTarget
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CommentRepliesSheet(viewModel: MiaopuViewModel, target: RatingTarget, parent: HupuComment?, initiallyReply: Boolean = false, onDismiss: () -> Unit) {
    val key = "sheet:${parent?.id}"
    val entry = viewModel.commentReplies.entry(key)
    val page = (entry?.state as? LoadState.Ready)?.value
    val rows = remember(parent, page?.comments) {
        if (parent == null || page == null) emptyList() else commentThreadRows(parent, page.comments)
    }
    var selectedReply by remember(parent?.id, initiallyReply) { mutableStateOf<HupuComment?>(parent.takeIf { initiallyReply }) }
    val recipient = selectedReply ?: parent
    val actions = viewModel.commentActions
    val like: (HupuComment) -> Unit = { if (viewModel.isLoggedIn) actions.toggleLike(it) else viewModel.openLogin() }
    OverlayBottomSheet(show = parent != null, title = "${maxOf(parent?.let(actions::replyCount) ?: 0, page?.totalCount ?: 0)} 条回复",
        onDismissRequest = onDismiss, startAction = {
            IconButton(onClick = onDismiss) { Icon(LucideIcons.X, "关闭回复", Modifier.size(20.dp)) }
        }, backgroundColor = MiuixTheme.colorScheme.surfaceContainer, cornerRadius = 24.dp,
        insideMargin = DpSize(16.dp, 12.dp), outsideMargin = DpSize(0.dp, 0.dp)) {
        if (parent != null) Column(Modifier.fillMaxWidth().imePadding()) {
            LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                item(key = "parent") {
                    Box(Modifier.fillMaxWidth().clickable { selectedReply = parent }.padding(14.dp)) {
                        CommentRow(parent, actions = actions, onLike = { like(parent) }, onReply = { selectedReply = parent }, metadataAbove = true)
                    }
                }
                when (val state = entry?.state) {
                    null, LoadState.Loading -> item { DetailNotice("正在加载回复", loading = true) }
                    is LoadState.Failed -> item { DetailNotice(state.message, onRetry = { viewModel.commentReplies.retry(target, key) }) }
                    is LoadState.Ready -> {
                        itemsIndexed(rows, key = { _, row -> "reply-${row.comment.id}" }) { _, row ->
                            ReplyThreadCard(row, parent, actions, selectedReply?.id == row.comment.id,
                                onLike = { like(row.comment) }, onReply = { selectedReply = row.comment })
                        }
                        if (state.value.comments.isEmpty()) item { DetailNotice("暂时没有回复") }
                        if (state.value.hasMore && state.value.nextPublishTime != null) item {
                            Row(Modifier.fillMaxWidth().clickable(enabled = !entry.isLoadingMore, role = Role.Button) {
                                viewModel.commentReplies.loadMore(target, key)
                            }.padding(start = 38.dp, top = 14.dp, bottom = 14.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(if (entry.isLoadingMore) "正在加载…" else if (entry.paginationError != null) "重新加载回复" else "展开更多回复",
                                    fontSize = 14.sp, color = MiuixTheme.colorScheme.primary)
                                Icon(LucideIcons.ChevronRight, null, Modifier.size(16.dp).rotate(90f), tint = MiuixTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            if (selectedReply != null) {
                val replyTo = recipient ?: parent
                val draft = actions.draft(replyTo)
                if (selectedReply != null) {
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("回复 ${replyTo.author}", Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = { selectedReply = null }, minWidth = 32.dp, minHeight = 32.dp) {
                                Icon(LucideIcons.X, "取消指定回复", Modifier.size(18.dp))
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(3.dp).height(24.dp).background(MiuixTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                            Text(replyTo.content.ifBlank { if (replyTo.imageUrls.isNotEmpty()) "[图片]" else "该评论暂无文字" },
                                Modifier.padding(start = 10.dp).weight(1f), fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                draft.error?.let { Text(it, fontSize = 12.sp, color = MiuixTheme.colorScheme.error) }
                CommentComposer(
                    focusKey = selectedReply?.id,
                    value = draft.text, placeholder = "回复 ${replyTo.author}…",
                    action = if (!viewModel.isLoggedIn) "登录" else if (draft.publishing) "发送中" else "发送",
                    inputEnabled = viewModel.isLoggedIn && target.canComment && !draft.publishing,
                    actionEnabled = !draft.publishing && (!viewModel.isLoggedIn || (target.canComment && draft.text.isNotBlank())),
                    onValueChange = { actions.updateDraft(replyTo, it) },
                    onSend = {
                        if (!viewModel.isLoggedIn) viewModel.openLogin()
                        else actions.publish(target, replyTo) {
                            if (replyTo.id != parent.id) actions.recordThreadReply(parent)
                            selectedReply = null
                            viewModel.commentReplies.retry(target, key)
                        }
                    },
                )
            }
        }
    }
}

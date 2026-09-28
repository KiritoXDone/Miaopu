package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.kiritoxd.miaopu.data.HupuComment
import dev.kiritoxd.miaopu.data.nestedReplyTarget
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun PlayerCommentCard(comment: HupuComment, actions: CommentActionsController, onLike: () -> Unit, onReply: () -> Unit, onReplies: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp).clickable(onClick = onReply),
        insideMargin = PaddingValues(14.dp), cornerRadius = 18.dp) {
        CommentRow(comment, actions = actions, onLike = onLike, onReply = onReply)
        if (comment.previewReplies.isNotEmpty() || actions.replyCount(comment) > 0) {
            Column(Modifier.padding(start = 44.dp, top = 7.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(MiuixTheme.colorScheme.surface).clickable(onClick = onReplies).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                comment.previewReplies.firstOrNull()?.let {
                    Text("${it.author}：${it.content}", fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("查看全部 ${maxOf(actions.replyCount(comment), comment.previewReplies.size)} 条回复", fontSize = 12.sp, color = MiuixTheme.colorScheme.primary)
                    Icon(LucideIcons.ChevronRight, null, Modifier.size(14.dp), tint = MiuixTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
internal fun CommentRow(comment: HupuComment, root: HupuComment? = null, actions: CommentActionsController, onLike: () -> Unit, onReply: (() -> Unit)? = null, metadataAbove: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(MiuixTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            if (comment.avatarUrl.isNullOrBlank()) Text(comment.author.take(1), fontSize = 13.sp, color = MiuixTheme.colorScheme.primary)
            else AsyncImage(comment.avatarUrl, "${comment.author}头像", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(comment.author, Modifier.weight(1f, fill = false), fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val author = root?.authorId?.takeIf(String::isNotBlank)?.let { it == comment.authorId } == true
                    val badge = if (author) "作者" else if (comment.score > 0) "${comment.score}分" else comment.badge?.name
                    badge?.takeIf(String::isNotBlank)?.let {
                        Text(it, Modifier.padding(start = 5.dp).background(MiuixTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(5.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp), fontSize = 10.sp, color = MiuixTheme.colorScheme.primary)
                    }
                }
                val like = actions.like(comment)
                IconButton(onClick = onLike, enabled = !like.pending, minWidth = 44.dp, minHeight = 32.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val tint = if (like.selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary
                        Icon(LucideIcons.ThumbsUp, if (like.selected) "取消点赞" else "点赞", Modifier.size(16.dp), tint = tint)
                        Text("${like.count}", fontSize = 11.sp, color = tint)
                    }
                }
            }
            val repliedTo = if (root != null) comment.nestedReplyTarget(root.id)
                ?: root.author.takeIf { comment.parentCommentId == root.id } else null
            val metadata = listOfNotNull(repliedTo?.let { "回复 $it" }, comment.date.takeIf(String::isNotBlank),
                comment.location?.takeIf(String::isNotBlank)).joinToString(" · ")
            if (metadataAbove && metadata.isNotBlank()) Text(metadata, fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Text(comment.content, modifier = if (onReply != null) Modifier.clickable(onClick = onReply) else Modifier, fontSize = 14.sp)
            CommentImages(comment.imageUrls)
            if (!metadataAbove && metadata.isNotBlank()) Text(metadata, fontSize = 10.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    }
}

internal fun shouldShowReplyToggle(replyCount: Int, hasPreview: Boolean, expanded: Boolean): Boolean =
    expanded || replyCount > 1 || (replyCount == 1 && !hasPreview)

package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.kiritoxd.miaopu.data.RatingTarget
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun RatingCommentSheet(
    show: Boolean, viewModel: MiaopuViewModel, target: RatingTarget, selectedScore: Int,
    onPublish: () -> Unit, onDismiss: () -> Unit,
) {
    OverlayBottomSheet(show = show, title = "发表评论 · ${selectedScore}分",
        onDismissRequest = onDismiss, endAction = {
            IconButton(onClick = onDismiss) { Icon(LucideIcons.X, "关闭评论输入", Modifier.size(20.dp)) }
        }, backgroundColor = MiuixTheme.colorScheme.surfaceContainer,
        insideMargin = DpSize(0.dp, 12.dp), outsideMargin = DpSize(0.dp, 0.dp)) {
        if (show) Column(Modifier.fillMaxWidth().imePadding()) {
            CommentInputBar(viewModel.commentDraft, target, viewModel.isLoggedIn, viewModel.isPublishingComment,
                selectedScore, viewModel::updateCommentDraft, onPublish, viewModel::openLogin,
                focusKey = "rating-comment")
        }
    }
}

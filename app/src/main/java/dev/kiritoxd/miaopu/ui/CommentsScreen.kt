package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kiritoxd.miaopu.data.HupuComment
import dev.kiritoxd.miaopu.data.RatingTarget
import dev.kiritoxd.miaopu.data.listKey
import dev.kiritoxd.miaopu.data.mergeCommentsByHeat
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun CommentsScreen(viewModel: MiaopuViewModel, target: RatingTarget) {
    val currentTarget = viewModel.latestRatingTarget(target)
    var order by rememberSaveable(target.outBizType, target.outBizNo) { mutableIntStateOf(0) }
    var selectedScore by rememberSaveable(target.outBizType, target.outBizNo) { mutableIntStateOf(currentTarget.userScore) }
    var showCommentInput by remember { mutableStateOf(false) }
    var replyImmediately by remember { mutableStateOf(false) }
    var publishAttempt by remember { mutableStateOf(false) }
    var replyParent by remember { mutableStateOf<HupuComment?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    LaunchedEffect(publishAttempt, viewModel.isPublishingComment) {
        if (publishAttempt && !viewModel.isPublishingComment) {
            if (viewModel.lastCommentSubmissionSucceeded) { selectedScore = currentTarget.userScore; showCommentInput = false; keyboard?.hide() }
            publishAttempt = false
        }
    }
    val openReplies: (HupuComment) -> Unit = { comment ->
        replyImmediately = false
        replyParent = comment
        val key = "sheet:${comment.id}"
        if (viewModel.commentReplies.entry(key) == null) viewModel.commentReplies.toggle(target, comment, key)
    }
    Scaffold(containerColor = MiuixTheme.colorScheme.surface, modifier = Modifier.imePadding(), topBar = {
        SmallTopAppBar(title = "选手评分", navigationIcon = {
            IconButton(onClick = viewModel::goBack) { Icon(LucideIcons.ChevronLeft, "返回评分") }
        })
    }) { padding ->
        when (val state = viewModel.commentState) {
            LoadState.Loading -> LoadingPane("正在加载评论", Modifier.padding(padding))
            is LoadState.Failed -> ErrorPane(state.message, state.retryable, viewModel::retry, Modifier.padding(padding))
            is LoadState.Ready -> {
                val page = state.value
                val comments = remember(page, order) {
                    if (order == 0) mergeCommentsByHeat(page.hottestComments, page.comments, page.hottestComments.map { it.id })
                    else page.comments.distinctBy { it.listKey }.sortedByDescending { it.publishTime ?: Long.MIN_VALUE }
                }
                LaunchedEffect(target.outBizType, target.outBizNo, page.comments.size, page.nextPublishTime, page.hasMore, viewModel.commentPaginationError) {
                    if (!page.hasMore || page.nextPublishTime == null || viewModel.commentPaginationError != null) return@LaunchedEffect
                    snapshotFlow {
                        val layout = listState.layoutInfo
                        layout.totalItemsCount > 0 && (layout.visibleItemsInfo.lastOrNull()?.index ?: -1) >= layout.totalItemsCount - 3
                    }.filter { it }.first()
                    viewModel.loadMoreComments(target)
                }
                LazyColumn(Modifier.fillMaxSize().padding(padding), state = listState,
                    contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    item(key = "player") {
                        PlayerIdentityCard(currentTarget, viewModel.commentMatchLabel(), selectedScore,
                            canSelectScore = viewModel.isLoggedIn && !viewModel.isPublishingComment,
                            onScoreChange = { selectedScore = it; showCommentInput = true }, onLogin = { if (!viewModel.isLoggedIn) viewModel.openLogin() })
                    }
                    item(key = "heading") {
                        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 18.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("全部评论 ${page.totalCount}", Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Box(Modifier.width(130.dp)) { DetailOrderSelector(order, labels = listOf("最热", "最新")) { order = it } }
                        }
                    }
                    if (comments.isEmpty()) item { DetailNotice("还没有评论，来说说你的看法") }
                    itemsIndexed(comments, key = { _, comment -> "comment-${comment.listKey}" }) { _, comment ->
                        PlayerCommentCard(comment, viewModel.commentActions,
                            onLike = { if (viewModel.isLoggedIn) viewModel.commentActions.toggleLike(comment) else viewModel.openLogin() },
                            onReply = { openReplies(comment); replyImmediately = true },
                            onReplies = { openReplies(comment) })
                    }
                    item(key = "pagination") {
                        if (viewModel.commentPaginationError != null) TextButton("重新加载评论", onClick = { viewModel.loadMoreComments(target) }, modifier = Modifier.padding(horizontal = 16.dp))
                        else if (page.hasMore && page.nextPublishTime != null) Text(if (viewModel.isLoadingMoreComments) "正在加载…" else "上滑加载更多", Modifier.padding(16.dp), fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
            }
        }
        CommentRepliesSheet(viewModel, target, replyParent, replyImmediately) { replyParent = null; keyboard?.hide() }
        RatingCommentSheet(showCommentInput, viewModel, currentTarget, selectedScore,
            onPublish = {
                publishAttempt = true
                viewModel.publishComment(currentTarget, selectedScore.takeIf { currentTarget.canScore && it > 0 && it != currentTarget.userScore })
            }, onDismiss = { showCommentInput = false; keyboard?.hide() })
    }
}

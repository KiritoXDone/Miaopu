package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.kiritoxd.miaopu.data.MatchSummary
import dev.kiritoxd.miaopu.data.RatingStage
import dev.kiritoxd.miaopu.data.RatingTarget
import dev.kiritoxd.miaopu.data.StageRatingDetail
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

internal enum class StageTargetOrder(val label: String) {
    HOT("热门"), LATEST("最新"), HIGH_SCORE("高分"), LOW_SCORE("低分"),
}

private data class StageTargetTab(val name: String, val targets: List<RatingTarget>)

@Composable
fun MatchStageScreen(
    viewModel: MiaopuViewModel,
    match: MatchSummary,
    stage: RatingStage,
    stageNumber: Int,
    showStageNumber: Boolean,
) {
    Scaffold(
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            SmallTopAppBar(
                title = "单局详情",
                subtitle = if (showStageNumber) "第 $stageNumber 局 · ${stage.name}" else stage.name,
                navigationIcon = {
                    IconButton(onClick = viewModel::goBack) {
                        Icon(LucideIcons.ChevronLeft, contentDescription = "返回赛事详情")
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = viewModel.stageRatingState) {
            LoadState.Loading -> LoadingPane("正在加载这一局的全部评分", Modifier.padding(innerPadding))
            is LoadState.Failed -> ErrorPane(
                message = state.message,
                retryable = state.retryable,
                onRetry = viewModel::retry,
                modifier = Modifier.padding(innerPadding),
            )
            is LoadState.Ready -> StageRatingContent(
                detail = state.value,
                match = match,
                stage = stage,
                modifier = Modifier.padding(innerPadding),
                onTargetClick = viewModel::openComments,
                savedViewport = viewModel.stageViewport(match, stage),
                onSaveViewport = { viewModel.saveStageViewport(match, stage, it) },
            )
        }
    }
}

@Composable
private fun StageRatingContent(
    detail: StageRatingDetail,
    match: MatchSummary,
    stage: RatingStage,
    modifier: Modifier,
    onTargetClick: (RatingTarget) -> Unit,
    savedViewport: StageViewportSnapshot?,
    onSaveViewport: (StageViewportSnapshot) -> Unit,
) {
    val tabs = remember(detail) {
        buildList {
            add(StageTargetTab("全部", detail.targets))
            detail.groups.filter { it.targets.isNotEmpty() }.forEach {
                add(StageTargetTab(it.name, it.targets))
            }
        }
    }
    var selectedTabIndex by rememberSaveable {
        mutableIntStateOf(savedViewport?.selectedTabIndex ?: 0)
    }
    var selectedOrderIndex by rememberSaveable {
        mutableIntStateOf(savedViewport?.selectedOrderIndex ?: 0)
    }
    val currentTabIndex = selectedTabIndex.coerceIn(0, tabs.lastIndex.coerceAtLeast(0))
    val currentOrderIndex = selectedOrderIndex.coerceIn(0, StageTargetOrder.entries.lastIndex)
    val selectedTab = tabs[currentTabIndex]
    val selectedOrder = StageTargetOrder.entries[currentOrderIndex]
    val visibleTargets = remember(selectedTab, selectedOrder) {
        orderRatingTargets(selectedTab.targets, selectedOrder)
    }

    val restoredViewport = savedViewport?.coerceFor(
        tabCount = tabs.size,
        orderCount = StageTargetOrder.entries.size,
        itemCount = visibleTargets.size,
    )
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = restoredViewport?.listIndex ?: 0,
        initialFirstVisibleItemScrollOffset = restoredViewport?.listOffset ?: 0,
    )
    val latestTabIndex by rememberUpdatedState(currentTabIndex)
    val latestOrderIndex by rememberUpdatedState(currentOrderIndex)

    DisposableEffect(listState) {
        onDispose {
            onSaveViewport(
                StageViewportSnapshot(
                    listIndex = listState.firstVisibleItemIndex,
                    listOffset = listState.firstVisibleItemScrollOffset,
                    selectedTabIndex = latestTabIndex,
                    selectedOrderIndex = latestOrderIndex,
                ),
            )
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        StageOverview(
            title = detail.title.ifBlank { stage.name },
            description = detail.description ?: match.introduction.ifBlank {
                match.teams.joinToString(" vs ") { it.name }.ifBlank { match.name }
            },
            imageUrl = detail.imageUrl,
            match = match,
        )
        TabRow(
            tabs = tabs.map { it.name },
            selectedTabIndex = currentTabIndex,
            onTabSelected = { selectedTabIndex = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            colors = flatTabRowColors(),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${visibleTargets.size}个评分",
                modifier = Modifier.weight(1f),
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Bold,
            )
            CompactOrderSelector(
                selectedIndex = currentOrderIndex,
                onSelected = { selectedOrderIndex = it },
            )
        }

        if (visibleTargets.isEmpty()) {
            EmptyPane("这个分组暂时没有评分对象", Modifier.weight(1f))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(
                    items = visibleTargets,
                    key = { target -> "${target.outBizType}:${target.outBizNo}" },
                    contentType = { "rating-target" },
                ) { target ->
                    OfficialRatingTargetCard(target = target, onClick = { onTargetClick(target) })
                }
            }
        }
    }
}

@Composable
internal fun CompactOrderSelector(selectedIndex: Int, onSelected: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        StageTargetOrder.entries.forEachIndexed { index, order ->
            val selected = index == selectedIndex
            TextButton(
                text = order.label,
                onClick = { onSelected(index) },
                modifier = Modifier.width(42.dp),
                minWidth = 42.dp,
                minHeight = 30.dp,
                cornerRadius = 10.dp,
                insideMargin = PaddingValues(0.dp),
                colors = ButtonDefaults.textButtonColors(
                    color = if (selected) MiuixTheme.colorScheme.primary else {
                        MiuixTheme.colorScheme.surfaceVariant
                    },
                    textColor = if (selected) MiuixTheme.colorScheme.onPrimary else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    },
                ),
                textStyle = MiuixTheme.textStyles.footnote2,
            )
        }
    }
}

@Composable
private fun StageOverview(
    title: String,
    description: String,
    imageUrl: String?,
    match: MatchSummary,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(58.dp).clip(RoundedCornerShape(14.dp))
                .background(MiuixTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (imageUrl.isNullOrBlank()) {
                Text(
                    text = match.esport.shortTitle,
                    color = MiuixTheme.colorScheme.primary,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MiuixTheme.textStyles.title2,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = description,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
internal fun OfficialRatingTargetCard(target: RatingTarget, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).semantics {
            role = Role.Button
            contentDescription = "查看 ${target.name} 的评分与评论"
        },
        insideMargin = PaddingValues(0.dp),
        cornerRadius = 18.dp,
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RatingTargetPortrait(
                target = target,
                size = 68.dp,
                cornerRadius = 12.dp,
                championSize = 25.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = target.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MiuixTheme.textStyles.title3,
                    fontWeight = FontWeight.Bold,
                )
                val label = target.labels.firstOrNull() ?: target.description
                if (!label.isNullOrBlank()) {
                    Spacer(Modifier.height(5.dp))
                    TagPill(
                        text = label,
                        modifier = Modifier.offset(x = (-9).dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (target.commentCount > 0) "${target.commentCount} 条评论" else "点击查看评论",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.width(78.dp), horizontalAlignment = Alignment.End) {
                FractionalRatingStars(
                    scoreAverage = target.scoreAverage,
                    scoreCount = target.scoreCount,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (target.scoreCount > 0) "%.1f".format(target.scoreAverage) else "—",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    style = MiuixTheme.textStyles.title1,
                    color = HupuScoreBlue,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${target.scoreCount} JRs评分",
                    maxLines = 1,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
        target.hotComment?.takeIf(String::isNotBlank)?.let { comment ->
            Text(
                text = "“$comment”",
                modifier = Modifier.fillMaxWidth().background(HupuCommentBackground)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MiuixTheme.textStyles.footnote1,
                color = HupuCommentText,
            )
        }
    }
}

@Composable
private fun FractionalRatingStars(scoreAverage: Double, scoreCount: Int) {
    val outlineColor = MiuixTheme.colorScheme.onSurfaceVariantSummary
    Row(Modifier.fillMaxWidth().semantics {
        contentDescription = if (scoreCount > 0) "平均评分 %.1f 分".format(scoreAverage) else "暂无评分"
    }, horizontalArrangement = Arrangement.SpaceBetween) {
        ratingStarFillFractions(scoreAverage, scoreCount).forEach { fraction ->
            Box(Modifier.size(12.dp)) {
                Icon(LucideIcons.Star, null, Modifier.fillMaxSize(), tint = outlineColor)
                if (fraction > 0f) Icon(LucideIcons.StarFilled, null,
                    Modifier.fillMaxSize().drawWithContent {
                        clipRect(right = size.width * fraction) { this@drawWithContent.drawContent() }
                    }, tint = HupuScoreBlue)
            }
        }
    }
}

internal fun ratingStarFillFractions(scoreAverage: Double, scoreCount: Int): List<Float> {
    val starScore = if (scoreCount > 0) (scoreAverage / 2).coerceIn(0.0, 5.0) else 0.0
    return List(5) { index -> (starScore - index).coerceIn(0.0, 1.0).toFloat() }
}

private val HupuScoreBlue = Color(0xFF28A4ED)
private val HupuCommentBackground = Color(0xFFFFF5EC)
private val HupuCommentText = Color(0xFFE85D2A)

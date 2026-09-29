package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kiritoxd.miaopu.data.MatchSummary
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun RatingsScreen(viewModel: MiaopuViewModel, match: MatchSummary) {
    val detail = viewModel.matchDetail
    val ratingListState = rememberLazyListState()
    val dataListState = rememberLazyListState()
    var page by rememberSaveable(match.uniqueKey) { mutableIntStateOf(0) }
    val activePage = if (detail.hasStatistics) page else 0
    var stageIndex by rememberSaveable(match.uniqueKey) { mutableIntStateOf(0) }
    val stages = (viewModel.ratingState as? LoadState.Ready)?.value?.stages.orEmpty()
    val selectedStageIndex = if (stages.isEmpty()) 0 else stageIndex.coerceIn(stages.indices)
    val selectedStage = stages.getOrNull(selectedStageIndex)
    var funSelected by rememberSaveable(match.uniqueKey, selectedStageIndex) { mutableStateOf(false) }
    val funGroup = (detail.stage as? LoadState.Ready)?.value?.groups?.firstOrNull { it.name == "趣评" && it.targets.isNotEmpty() }
    var groupIndex by rememberSaveable(match.uniqueKey, selectedStage?.outBizNo, stageIndex) { mutableIntStateOf(0) }
    var orderIndex by rememberSaveable(match.uniqueKey) { mutableIntStateOf(0) }
    val stageDetail = (detail.stage as? LoadState.Ready)?.value
    val groups = remember(stageDetail, match.teams) {
        stageDetail?.groups.orEmpty().filter { it.name != "趣评" && it.targets.isNotEmpty() }
            .sortedBy { group -> match.teams.indexOfFirst { it.name == group.name }.takeIf { it >= 0 } ?: Int.MAX_VALUE }
    }
    val groupNames = remember(groups) { groups.map { it.name } }
    val selectedGroup = if (groups.isEmpty()) 0 else groupIndex.coerceIn(groups.indices)
    val targets = if (funSelected && funGroup != null) funGroup.targets
        else groups.getOrNull(selectedGroup)?.targets ?: stageDetail?.targets.orEmpty()
    // Score/count updates refresh card values without moving cards under the reader's finger.
    val targetOrder = remember(match.uniqueKey, selectedStageIndex, selectedStage?.outBizType, selectedStage?.outBizNo,
        funSelected, selectedGroup, orderIndex, targets.isEmpty()) {
        orderRatingTargets(targets, StageTargetOrder.entries[orderIndex])
            .map { it.outBizType to it.outBizNo }.distinct()
    }
    val orderedTargets = remember(targets, targetOrder) {
        val byKey = targets.associateBy { it.outBizType to it.outBizNo }
        targetOrder.mapNotNull(byKey::get)
    }
    LaunchedEffect(match.uniqueKey) {
        detail.bind(match)
        detail.ensureStats()
    }
    LaunchedEffect(match.uniqueKey, selectedStage) { selectedStage?.let(detail::loadStage) }

    Scaffold(
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            SmallTopAppBar(
                title = "比赛详情",
                navigationIcon = {
                    IconButton(onClick = viewModel::goBack) {
                        Icon(LucideIcons.ChevronLeft, contentDescription = "返回赛事")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = if (activePage == 0) ratingListState else dataListState,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "hero") { MatchHero(match) }
            if (detail.hasStatistics) item(key = "pages") {
                DetailTabs(listOf("评分", "数据"), activePage, style = DetailTabStyle.PAGE) { page = it }
            }
            if (activePage == 0) {
                val summary = (detail.summary as? LoadState.Ready)?.value
                if (summary?.hasScores == true) item(key = "summary") { AllMatchScoreCard(summary) }
                when (val state = viewModel.ratingState) {
                    LoadState.Loading -> item { DetailNotice("正在加载单局评分", loading = true) }
                    is LoadState.Failed -> item {
                        DetailNotice(state.message, onRetry = if (state.retryable) viewModel::retry else null)
                    }
                    is LoadState.Ready -> {
                        if (stages.isEmpty()) {
                            item { DetailNotice("这场比赛暂时没有可评分的选手") }
                        } else {
                            item(key = "stages") {
                                val choices = stages.indices.map { it as Int? }.toMutableList()
                                if (funGroup != null) choices.add(minOf(1, choices.size), null)
                                DetailTabs(choices.map { it?.let { index -> stages[index].name } ?: "趣评" },
                                    if (funSelected && funGroup != null) choices.indexOf(null) else choices.indexOf(selectedStageIndex)) { choice ->
                                    val nextStage = choices[choice]
                                    funSelected = nextStage == null
                                    if (nextStage != null) {
                                        stageIndex = nextStage
                                        detail.loadStage(stages[nextStage])
                                    }
                                }
                            }
                            when (val stageState = detail.stage) {
                                LoadState.Loading -> item { DetailNotice("正在加载这一局的评分", loading = true) }
                                is LoadState.Failed -> item {
                                    DetailNotice(stageState.message, onRetry = {
                                        selectedStage?.let { detail.loadStage(it, retry = true) }
                                    })
                                }
                                is LoadState.Ready -> {
                                    if (groups.isNotEmpty() && !funSelected) item(key = "teams") {
                                        DetailTabs(groupNames, selectedGroup, style = DetailTabStyle.TEAM, logos = groups.map { it.logoUrl }) { groupIndex = it }
                                    }
                                    item(key = "order") {
                                        DetailOrderSelector(orderIndex) { orderIndex = it }
                                    }
                                    if (targets.isEmpty()) item { DetailNotice("这个分组暂时没有评分对象") }
                                    itemsIndexed(orderedTargets, key = { _, target -> "target-${target.outBizType.length}:${target.outBizType}${target.outBizNo}" }, contentType = { _, _ -> "player" }) { _, target ->
                                        MatchRatingPlayerCard(target) { viewModel.openComments(target) }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (detail.maps.isNotEmpty()) item(key = "maps") {
                    DetailTabs(detail.maps.map { it.name }, detail.maps.indexOfFirst { it.id == detail.selectedMapId }.coerceAtLeast(0)) {
                        detail.loadStats(detail.maps[it].id)
                    }
                }
                when (val state = detail.statistics) {
                    null, LoadState.Loading -> item { DetailNotice("正在加载比赛数据", loading = true) }
                    is LoadState.Failed -> item { DetailNotice(state.message, onRetry = { detail.loadStats() }) }
                    is LoadState.Ready -> {
                        if (state.value.teams.isEmpty() || state.value.teams.all { it.players.isEmpty() }) {
                            item { DetailNotice("这场比赛暂时没有技术统计") }
                        } else {
                            itemsIndexed(state.value.teams, key = { index, _ -> "stats-$index-${detail.selectedMapId}" }, contentType = { _, _ -> "stats" }) { _, team ->
                                MatchStatsTable(team)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DetailNotice(message: String, loading: Boolean = false, onRetry: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(message, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        if (loading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
        if (onRetry != null) TextButton(text = "重新加载", onClick = onRetry)
    }
}

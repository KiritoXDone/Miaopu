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
import dev.kiritoxd.miaopu.data.RatingTarget
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
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
    val selectedStage = stages.getOrNull(stageIndex) ?: stages.firstOrNull()
    var groupIndex by rememberSaveable(match.uniqueKey, selectedStage?.outBizNo, stageIndex) { mutableIntStateOf(0) }
    var orderIndex by rememberSaveable(match.uniqueKey) { mutableIntStateOf(0) }
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
                actions = {
                    TextButton(text = "刷新", onClick = {
                        viewModel.retry()
                        detail.refresh()
                    })
                },
                navigationIcon = {
                    IconButton(onClick = viewModel::goBack) {
                        Icon(MiuixIcons.ChevronBackward, contentDescription = "返回赛事")
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
                DetailTabs(listOf("评分", "数据"), activePage) { page = it }
            }
            if (activePage == 0) {
                item(key = "summary") {
                    when (val state = detail.summary) {
                        LoadState.Loading -> DetailNotice("正在加载全场评分", loading = true)
                        is LoadState.Failed -> DetailNotice(state.message, onRetry = detail::loadSummary)
                        is LoadState.Ready -> AllMatchScoreCard(state.value)
                    }
                }
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
                                DetailTabs(stages.map { it.name }, stageIndex.coerceIn(stages.indices)) {
                                    stageIndex = it
                                    detail.loadStage(stages[it])
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
                                    val groups = stageState.value.groups.filter { it.targets.isNotEmpty() }
                                    val groupNames = listOf("全部") + groups.map { it.name }
                                    val selectedGroup = groupIndex.coerceIn(groupNames.indices)
                                    val targets: List<RatingTarget> = if (selectedGroup == 0) stageState.value.targets
                                        else groups.getOrNull(selectedGroup - 1)?.targets ?: stageState.value.targets
                                    if (groups.isNotEmpty()) item(key = "teams") {
                                        DetailTabs(groupNames, selectedGroup) { groupIndex = it }
                                    }
                                    item(key = "order") {
                                        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                            Text("${targets.size} 个评分", style = MiuixTheme.textStyles.footnote1)
                                            CompactOrderSelector(orderIndex) { orderIndex = it }
                                        }
                                    }
                                    val orderedTargets = orderRatingTargets(targets, StageTargetOrder.entries[orderIndex])
                                    if (targets.isEmpty()) item { DetailNotice("这个分组暂时没有评分对象") }
                                    itemsIndexed(orderedTargets, key = { index, target -> "target-$index-${target.outBizNo}" }) { _, target ->
                                        OfficialRatingTargetCard(target) { viewModel.openComments(target) }
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
                            item {
                                Text("左右滑动查看全部数据", modifier = Modifier.padding(horizontal = 20.dp),
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            }
                            itemsIndexed(state.value.teams, key = { index, _ -> "stats-$index-${detail.selectedMapId}" }) { _, team ->
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
internal fun DetailTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    TabRow(tabs = labels, selectedTabIndex = selected, onTabSelected = onSelect,
        colors = dataSourceTabRowColors(), modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
internal fun DetailNotice(message: String, loading: Boolean = false, onRetry: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(message, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        if (loading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
        if (onRetry != null) TextButton(text = "重新加载", onClick = onRetry)
    }
}

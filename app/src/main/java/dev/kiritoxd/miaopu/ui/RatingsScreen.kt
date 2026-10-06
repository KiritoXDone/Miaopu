package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kiritoxd.miaopu.data.StageRatingDetail
import dev.kiritoxd.miaopu.data.matchdetail.MatchStats
import dev.kiritoxd.miaopu.data.MatchSummary
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun RatingsScreen(viewModel: MiaopuViewModel, match: MatchSummary) {
    val detail = viewModel.matchDetail
    val liveScore = rememberMatchLiveScore(match)
    val density = LocalDensity.current
    val width = LocalConfiguration.current.screenWidthDp
    val layout = remember(match.uniqueKey, width, density.density, density.fontScale) { DetailLayoutState() }
    val ratingListState = rememberLazyListState()
    val dataListState = rememberLazyListState()
    var page by rememberSaveable(match.uniqueKey) { mutableIntStateOf(0) }
    var header by remember(match.uniqueKey) { mutableStateOf<MatchHeaderSnapshot?>(null) }
    val candidateHeader = remember(detail.summary, detail.statistics, detail.hasStatistics) {
        matchHeaderSnapshot(detail.summary, detail.statistics, detail.hasStatistics)
    }
    val bodyReady = viewModel.ratingState !is LoadState.Loading &&
        ((viewModel.ratingState as? LoadState.Ready)?.value?.stages.isNullOrEmpty() || detail.stage !is LoadState.Loading)
    val visibleHeader = header ?: candidateHeader?.takeIf { bodyReady }
    SideEffect { if (header == null && visibleHeader != null) header = visibleHeader }
    val activePage = if (visibleHeader?.hasStatistics == true) page else 0
    var stageIndex by rememberSaveable(match.uniqueKey) { mutableIntStateOf(0) }
    val stages = (viewModel.ratingState as? LoadState.Ready)?.value?.stages.orEmpty()
    val selectedStageIndex = if (stages.isEmpty()) 0 else stageIndex.coerceIn(stages.indices)
    val selectedStage = stages.getOrNull(selectedStageIndex)
    var funSelected by rememberSaveable(match.uniqueKey, selectedStageIndex) { mutableStateOf(false) }
    val readyStage = (detail.stage as? LoadState.Ready)?.value
    val readyStats = (detail.statistics as? LoadState.Ready)?.value
    var previousStage by remember(match.uniqueKey) { mutableStateOf<StageRatingDetail?>(null) }
    var previousStats by remember(match.uniqueKey) { mutableStateOf<MatchStats?>(null) }
    var hasFunTab by rememberSaveable(match.uniqueKey) { mutableStateOf(false) }
    SideEffect {
        if (readyStage != null) {
            previousStage = readyStage
            if (readyStage.groups.any { it.name == "趣评" && it.targets.isNotEmpty() }) hasFunTab = true
        }
        if (readyStats != null) previousStats = readyStats
    }
    val stageDetail = readyStage ?: previousStage
    val statsDetail = readyStats ?: previousStats
    val funGroup = stageDetail?.groups?.firstOrNull { it.name == "趣评" && it.targets.isNotEmpty() }
    val showFunTab = hasFunTab || funGroup != null
    val stagePending = readyStage == null
    val statsPending = readyStats == null
    var groupIndex by rememberSaveable(match.uniqueKey, selectedStage?.outBizNo, stageIndex) { mutableIntStateOf(0) }
    val groups = remember(stageDetail, match.teams) {
        stageDetail?.groups.orEmpty().filter { it.name != "趣评" && it.targets.isNotEmpty() }
            .sortedBy { group -> match.teams.indexOfFirst { it.name == group.name }.takeIf { it >= 0 } ?: Int.MAX_VALUE }
    }
    val groupNames = remember(groups) { groups.map { it.name } }
    val selectedGroup = if (groups.isEmpty()) 0 else groupIndex.coerceIn(groups.indices)
    val targets = if (funSelected) funGroup?.targets.orEmpty()
        else groups.getOrNull(selectedGroup)?.targets ?: stageDetail?.targets.orEmpty()
    // Score/count updates refresh card values without moving cards under the reader's finger.
    val targetOrder = remember(match.uniqueKey, selectedStageIndex, selectedStage?.outBizType, selectedStage?.outBizNo,
        funSelected, selectedGroup, targets.isEmpty(), stagePending) {
        orderRatingTargets(targets, StageTargetOrder.HOT)
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
            item(key = "hero") { MatchHero(match.withLiveScore(liveScore), liveScore) }
            if (visibleHeader == null) {
                item(key = "initial-loading") { DetailNotice("正在加载比赛详情", loading = true) }
                return@LazyColumn
            }
            if (visibleHeader.hasStatistics) item(key = "pages") {
                DetailTabs(listOf("评分", "数据"), activePage, style = DetailTabStyle.PAGE) { page = it }
            }
            if (activePage == 0) {
                val summary = visibleHeader.summary
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
                                if (showFunTab) choices.add(minOf(1, choices.size), null)
                                DetailTabs(choices.map { it?.let { index -> stages[index].name } ?: "趣评" },
                                    if (funSelected && showFunTab) choices.indexOf(null) else choices.indexOf(selectedStageIndex)) { choice ->
                                    val nextStage = choices[choice]
                                    funSelected = nextStage == null
                                    if (nextStage != null) {
                                        stageIndex = nextStage
                                        detail.loadStage(stages[nextStage])
                                    }
                                }
                            }
                            if (stageDetail == null) {
                                item(key = "stage-notice") { StageLoadNotice(detail.stage) {
                                    selectedStage?.let { detail.loadStage(it, retry = true) }
                                } }
                            } else {
                                if (groups.isNotEmpty() && !funSelected) item(key = "teams") {
                                    StableDetailSlot(stagePending, layout, "teams", notice = {
                                        StageLoadNotice(detail.stage) {
                                            selectedStage?.let { detail.loadStage(it, retry = true) }
                                        }
                                    }) {
                                        DetailTabs(groupNames, selectedGroup, style = DetailTabStyle.TEAM, logos = groups.map { it.logoUrl }) { groupIndex = it }
                                    }
                                }
                                if (stagePending && (groups.isEmpty() || funSelected)) item(key = "stage-progress") {
                                    StageLoadNotice(detail.stage) {
                                        selectedStage?.let { detail.loadStage(it, retry = true) }
                                    }
                                }
                                if (targets.isEmpty()) item { DetailNotice("这个分组暂时没有评分对象") }
                                itemsIndexed(orderedTargets, key = { _, target -> "target-${target.outBizType.length}:${target.outBizType}${target.outBizNo}" }, contentType = { _, _ -> "player" }) { _, target ->
                                    StableDetailSlot(stagePending, layout, "target-${target.outBizType.length}:${target.outBizType}${target.outBizNo}") {
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
                if (statsDetail?.teams?.size == 2) {
                    item(key = "map-score") {
                        StableDetailSlot(statsPending, layout, "map-score") {
                            MatchMapScoreCard(statsDetail.teams)
                        }
                    }
                }
                if (statsDetail == null) {
                    item(key = "stats-notice") { StatsLoadNotice(detail.statistics) { detail.loadStats() } }
                } else if (statsDetail.teams.isEmpty() || statsDetail.teams.all { it.players.isEmpty() }) {
                    item(key = "stats-notice") {
                        if (statsPending) StatsLoadNotice(detail.statistics) { detail.loadStats() }
                        else DetailNotice("这场比赛暂时没有技术统计")
                    }
                } else {
                    itemsIndexed(statsDetail.teams, key = { index, _ -> "stats-$index" }, contentType = { _, _ -> "stats" }) { index, team ->
                        StableDetailSlot(statsPending, layout, "stats-$index", notice = if (index == 0) ({
                            StatsLoadNotice(detail.statistics) { detail.loadStats() }
                        }) else null) { MatchStatsTable(team, layout.tableScroll("stats-$index")) }
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

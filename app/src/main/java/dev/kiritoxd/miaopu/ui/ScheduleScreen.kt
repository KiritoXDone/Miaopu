package dev.kiritoxd.miaopu.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.kiritoxd.miaopu.data.Esport
import dev.kiritoxd.miaopu.data.EsportCatalog
import dev.kiritoxd.miaopu.data.Schedule
import dev.kiritoxd.miaopu.data.focusMatchId
import dev.kiritoxd.miaopu.data.focusInitialItemIndex
import dev.kiritoxd.miaopu.data.homeWindowAround
import dev.kiritoxd.miaopu.data.mergeSchedules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

private class MainPageRequest(val section: MainSection)

@Composable
fun ScheduleScreen(viewModel: MiaopuViewModel) {
    val sections = MainSection.entries
    val subscriptions = viewModel.subscribedEsports
    val pagerState = rememberPagerState(
        initialPage = viewModel.selectedMainSection.ordinal,
        pageCount = { sections.size },
    )
    var pageRequest by remember { mutableStateOf<MainPageRequest?>(null) }
    val selectSection: (MainSection) -> Unit = { section ->
        if (pageRequest?.section != section &&
            (pagerState.currentPage != section.ordinal || pagerState.isScrollInProgress)) {
            pageRequest = MainPageRequest(section)
        }
        viewModel.selectMainSection(section)
    }
    val visibleSection = pageRequest?.section ?: sections[pagerState.currentPage]
    val backState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = backState,
        isBackEnabled = viewModel.selectedMainSection != MainSection.HOME,
        onBackCompleted = { selectSection(MainSection.HOME) },
    )
    // Widget/deep-link navigation can change the selection without a bar click.
    LaunchedEffect(viewModel.selectedMainSection) {
        val section = viewModel.selectedMainSection
        if (pageRequest?.section != section &&
            (pageRequest != null || pagerState.currentPage != section.ordinal)) {
            pageRequest = MainPageRequest(section)
        }
    }
    LaunchedEffect(pageRequest, pagerState) {
        val request = pageRequest ?: return@LaunchedEffect
        try {
            pagerState.animateScrollToPage(
                request.section.ordinal,
                animationSpec = tween(TabMotionDurationMillis, easing = TabMotionEasing),
            )
        } finally {
            if (pageRequest === request) pageRequest = null
        }
    }
    LaunchedEffect(pagerState, viewModel) {
        snapshotFlow {
            if (pageRequest == null && !pagerState.isScrollInProgress) pagerState.settledPage else null
        }
            .distinctUntilChanged()
            .filterNotNull()
            .collect { page ->
                val section = sections[page]
                if (section != viewModel.selectedMainSection) viewModel.selectMainSection(section)
            }
    }
    Scaffold(
        containerColor = MiuixTheme.colorScheme.surface,
        bottomBar = {
            MainNavigationBar(
                selected = visibleSection,
                onSelect = selectSection,
            )
        },
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { page -> sections[page].name },
        ) { page ->
            MainSectionContent(
                viewModel = viewModel,
                section = sections[page],
                subscriptions = subscriptions,
                innerPadding = innerPadding,
            )
        }
    }
}

@Composable
private fun MainSectionContent(
    viewModel: MiaopuViewModel,
    section: MainSection,
    subscriptions: List<Esport>,
    innerPadding: PaddingValues,
) {
    when (section) {
        MainSection.HOME -> HomeSectionContent(viewModel, subscriptions, innerPadding)
        MainSection.EVENTS -> EventsSectionContent(viewModel, subscriptions, innerPadding)
        MainSection.PROFILE -> ProfileContent(viewModel = viewModel, innerPadding = innerPadding)
    }
}

private class HomeScheduleMerge(val sources: List<Schedule>, val schedule: Schedule)

@Composable
private fun HomeSectionContent(
    viewModel: MiaopuViewModel,
    subscriptions: List<Esport>,
    innerPadding: PaddingValues,
) {
    val states = subscriptions.map(viewModel::scheduleStateFor)
    val loading = states.any { it is LoadState.Loading }
    val readySchedules = states.mapNotNull { state -> (state as? LoadState.Ready)?.value }
    val failedStates = states.filterIsInstance<LoadState.Failed>()
    // Canonicalize equal snapshots so an equal refresh cannot invalidate a completed merge.
    val mergeSources = remember(readySchedules) { readySchedules }
    var displayedSchedule by remember(subscriptions) { mutableStateOf<Schedule?>(null) }
    val mergeResult by produceState<HomeScheduleMerge?>(null, subscriptions, mergeSources, loading) {
        if (loading) return@produceState
        val displayed = displayedSchedule
        val merged = withContext(Dispatchers.Default) {
            mergeSchedules(mergeSources).let { if (displayed != null && it == displayed) displayed else it }
        }
        value = HomeScheduleMerge(mergeSources, merged)
    }
    val mergedSchedule = mergeResult?.takeIf { result ->
        !loading && result.sources === mergeSources
    }?.schedule
    // Once reading starts, a network completion must not replace the visible timeline.
    LaunchedEffect(loading, mergedSchedule, subscriptions) {
        if (mergedSchedule != null && displayedSchedule == null && readySchedules.isNotEmpty()) {
            displayedSchedule = mergedSchedule
        }
    }
    val pendingUpdate = mergedSchedule != null && readySchedules.isNotEmpty() &&
        displayedSchedule != null && displayedSchedule !== mergedSchedule
    val bottomPadding = innerPadding.calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding()),
    ) {
        HomeHeader(viewModel)
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when {
                displayedSchedule != null -> HomeContent(viewModel, checkNotNull(displayedSchedule), bottomPadding)
                (loading || (mergedSchedule == null && readySchedules.isNotEmpty())) -> LoadingPane(
                    label = "正在合并已订阅赛事",
                    modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding),
                )
                failedStates.isNotEmpty() -> ErrorPane(
                    message = failedStates.first().message,
                    retryable = failedStates.any(LoadState.Failed::retryable),
                    onRetry = viewModel::refreshHomeSchedules,
                    modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding),
                )
                mergedSchedule != null -> HomeContent(viewModel, mergedSchedule, bottomPadding)
                else -> LoadingPane("正在合并已订阅赛事", Modifier.fillMaxSize())
            }
            if (pendingUpdate) {
                TextButton("赛程已更新，点击查看", onClick = { displayedSchedule = mergedSchedule },
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
            }
        }
    }
}

private class EsportPageRequest(val esport: Esport)

@Composable
private fun EventsSectionContent(
    viewModel: MiaopuViewModel,
    subscriptions: List<Esport>,
    innerPadding: PaddingValues,
) {
    if (subscriptions.isEmpty()) return

    val selectedEsportIndex = subscriptions.indexOf(viewModel.selectedEsport).coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = selectedEsportIndex,
        pageCount = { subscriptions.size },
    )
    var pageRequest by remember { mutableStateOf<EsportPageRequest?>(null) }
    val visibleEsport = pageRequest?.esport ?: subscriptions.getOrNull(pagerState.currentPage)
        ?: subscriptions[selectedEsportIndex]
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(viewModel.selectedEsport, subscriptions) {
        val esport = viewModel.selectedEsport
        if (pageRequest?.esport != esport &&
            (pageRequest != null || subscriptions.getOrNull(pagerState.currentPage) != esport)) {
            pageRequest = EsportPageRequest(esport)
        }
    }
    LaunchedEffect(pageRequest, subscriptions, pagerState) {
        val request = pageRequest ?: return@LaunchedEffect
        try {
            val targetPage = subscriptions.indexOf(request.esport)
            if (targetPage >= 0) {
                pagerState.animateScrollToPage(
                    targetPage,
                    animationSpec = tween(TabMotionDurationMillis, easing = TabMotionEasing),
                )
            }
        } finally {
            // An interrupted animation must not clear a newer tap's destination.
            if (pageRequest === request) pageRequest = null
        }
    }
    LaunchedEffect(pagerState, subscriptions, viewModel) {
        snapshotFlow {
            if (pageRequest == null && !pagerState.isScrollInProgress) {
                subscriptions.getOrNull(pagerState.settledPage)
            } else null
        }
            .distinctUntilChanged()
            .filterNotNull()
            .collect { esport ->
                if (esport != viewModel.selectedEsport) viewModel.selectEsport(esport)
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding()),
    ) {
        EventsHeader(
            viewModel = viewModel,
            esport = visibleEsport,
            onEsportSelect = { esport ->
                pageRequest = EsportPageRequest(esport)
                viewModel.selectEsport(esport)
            },
            searchExpanded = searchExpanded,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onSearchExpandedChange = { expanded ->
                searchExpanded = expanded
                if (!expanded) searchQuery = ""
            },
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            key = { page -> subscriptions[page].businessId },
        ) { page ->
            EventsPageContent(
                viewModel = viewModel,
                esport = subscriptions[page],
                innerPadding = innerPadding,
                searchQuery = searchQuery,
            )
        }
    }
}

@Composable
private fun MainNavigationBar(
    selected: MainSection,
    onSelect: (MainSection) -> Unit,
) {
    NavigationBar(showDivider = false) {
        NavigationBarItem(
            selected = selected == MainSection.HOME,
            onClick = { onSelect(MainSection.HOME) },
            icon = LucideIcons.House,
            label = "首页",
        )
        NavigationBarItem(
            selected = selected == MainSection.EVENTS,
            onClick = { onSelect(MainSection.EVENTS) },
            icon = LucideIcons.CalendarDays,
            label = "赛事",
        )
        NavigationBarItem(
            selected = selected == MainSection.PROFILE,
            onClick = { onSelect(MainSection.PROFILE) },
            icon = LucideIcons.CircleUserRound,
            label = "我的",
        )
    }
}

@Composable
private fun HomeContent(
    viewModel: MiaopuViewModel,
    schedule: Schedule,
    bottomPadding: Dp,
) {
    val nowMillis = remember(schedule) { System.currentTimeMillis() }
    val homeSchedule = remember(schedule, nowMillis) { schedule.homeWindowAround(nowMillis) }

    if (homeSchedule.days.isEmpty()) {
        EmptyPane("暂无赛程数据", Modifier.fillMaxSize().padding(bottom = bottomPadding))
        return
    }
    val listState = key("merged-home-feed-v3") {
        rememberSaveable(saver = LazyListState.Saver) {
            LazyListState(firstVisibleItemIndex = homeSchedule.focusInitialItemIndex(nowMillis))
        }
    }

    LaunchedEffect(homeSchedule, listState) {
        val lastIndex = (homeSchedule.days.sumOf { 1 + it.matches.size } - 1).coerceAtLeast(0)
        if (listState.firstVisibleItemIndex > lastIndex) listState.scrollToItem(lastIndex)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        contentPadding = PaddingValues(bottom = bottomPadding + 16.dp),
    ) {
        homeSchedule.days.forEach { day ->
            item(key = "home-day-${day.date}", contentType = "day") {
                ScheduleDayBand(
                    day = day,
                    isFocused = day.matches.any { it.id == homeSchedule.anchorMatchId },
                )
            }
            itemsIndexed(
                items = day.matches,
                key = { index, match ->
                    "home-${day.date}-${match.esport.businessId}-${match.id}-$index"
                },
                contentType = { _, _ -> "schedule-match" },
            ) { _, match ->
                HupuScheduleMatchCard(
                    match = match,
                    onClick = { viewModel.openMatch(match) },
                )
            }
        }
    }
}

@Composable
internal fun EventsContent(
    viewModel: MiaopuViewModel,
    schedule: Schedule,
    esport: Esport,
    bottomPadding: Dp,
) {
    if (schedule.days.isEmpty()) {
        EmptyPane("暂无赛程数据", Modifier.fillMaxSize().padding(bottom = bottomPadding))
        return
    }

    val nowMillis = remember(schedule) { System.currentTimeMillis() }
    val focusMatchId = remember(schedule, nowMillis) { schedule.focusMatchId(nowMillis) }
    val anchorDayIndex = remember(schedule, focusMatchId) {
        schedule.days.indexOfFirst { day -> day.matches.any { it.id == focusMatchId } }
            .coerceAtLeast(0)
    }
    val dayItemIndices = remember(schedule) {
        buildList {
            var itemIndex = 0
            schedule.days.forEach { day ->
                add(itemIndex)
                itemIndex += 1 + day.matches.size
            }
        }
    }
    val initialListIndex = remember(schedule, nowMillis) { schedule.focusInitialItemIndex(nowMillis) }
    val initialDayIndex = remember(schedule, initialListIndex) {
        dayItemIndices.indexOfLast { it <= initialListIndex }.coerceAtLeast(0)
    }
    val savedViewport = remember(esport, schedule) { viewModel.scheduleViewport(esport) }
    val totalListItems = remember(schedule) { schedule.days.sumOf { 1 + it.matches.size } }
    val restoredListIndex = savedViewport?.listIndex?.coerceIn(0, (totalListItems - 1).coerceAtLeast(0))
    val listState = rememberSaveable(esport.businessId, "events-list-v2", saver = LazyListState.Saver) {
        LazyListState(
            firstVisibleItemIndex = restoredListIndex ?: initialListIndex,
            firstVisibleItemScrollOffset = savedViewport?.listOffset?.coerceAtLeast(0) ?: 0,
        )
    }
    var selectedDayKey by rememberSaveable(esport.businessId, "events-day") {
        mutableStateOf(
            savedViewport?.selectedDayKey
                ?.takeIf { saved -> schedule.days.any { it.date == saved } }
                ?: schedule.days.getOrNull(initialDayIndex)?.date.orEmpty(),
        )
    }
    val latestSelectedDayKey by rememberUpdatedState(selectedDayKey)

    LaunchedEffect(esport, schedule, listState) {
        val lastListIndex = (totalListItems - 1).coerceAtLeast(0)
        if (listState.firstVisibleItemIndex > lastListIndex) listState.scrollToItem(lastListIndex)
        if (schedule.days.none { it.date == selectedDayKey }) {
            selectedDayKey = schedule.days.getOrNull(initialDayIndex)?.date.orEmpty()
        }
    }

    DisposableEffect(esport, schedule, listState) {
        onDispose {
            viewModel.saveScheduleViewport(
                esport,
                ScheduleViewportSnapshot(
                    listIndex = listState.firstVisibleItemIndex,
                    listOffset = listState.firstVisibleItemScrollOffset,
                    selectedDayKey = latestSelectedDayKey,
                ),
            )
        }
    }

    LaunchedEffect(esport, schedule, listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { itemIndex -> dayItemIndices.indexOfLast { it <= itemIndex }.coerceAtLeast(0) }
            .distinctUntilChanged()
            .collect { dayIndex -> selectedDayKey = schedule.days[dayIndex].date }
    }

    val listContentPadding = PaddingValues(bottom = bottomPadding + 16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = listContentPadding,
        ) {
            schedule.days.forEachIndexed { dayIndex, day ->
                item(key = "day-${day.date}", contentType = "day") {
                    ScheduleDayBand(day = day, isFocused = dayIndex == anchorDayIndex)
                }
                itemsIndexed(
                    items = day.matches,
                    key = { index, match -> "${day.date}-${match.id}-$index" },
                    contentType = { _, _ -> "schedule-match" },
                ) { _, match ->
                    HupuScheduleMatchCard(match = match, onClick = { viewModel.openMatch(match) })
                }
            }
        }
        ScheduleDateScrollBar(
            state = listState,
            date = selectedDayKey,
            trackPadding = listContentPadding,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun HomeHeader(viewModel: MiaopuViewModel) {
    PageHeading(
        title = "近期赛程",
        actions = {
            IconButton(
                onClick = viewModel::refreshHomeSchedules,
                backgroundColor = MiuixTheme.colorScheme.surfaceContainer,
            ) {
                Icon(LucideIcons.RefreshCw, contentDescription = "刷新赛程")
            }
        },
    )
}

@Composable
private fun EventsHeader(
    viewModel: MiaopuViewModel,
    esport: Esport,
    onEsportSelect: (Esport) -> Unit,
    searchExpanded: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchExpandedChange: (Boolean) -> Unit,
) {
    PageHeading(
        title = "完整赛程",
        actions = {
            Row {
                IconButton(
                    onClick = { onSearchExpandedChange(!searchExpanded) },
                    backgroundColor = MiuixTheme.colorScheme.surfaceContainer,
                ) {
                    Icon(
                        LucideIcons.Search,
                        contentDescription = if (searchExpanded) "关闭搜索" else "搜索赛程",
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = viewModel::refreshSchedule,
                    backgroundColor = MiuixTheme.colorScheme.surfaceContainer,
                ) {
                    Icon(LucideIcons.RefreshCw, contentDescription = "刷新赛程")
                }
            }
        },
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (searchExpanded) {
            EventsScheduleSearchBar(
                expanded = true,
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onExpandedChange = onSearchExpandedChange,
            )
        } else {
            EsportSelector(viewModel.subscribedEsports, esport, onEsportSelect)
        }
    }
}

@Composable
private fun EsportSelector(
    subscriptions: List<Esport>,
    selectedEsport: Esport,
    onSelect: (Esport) -> Unit,
) {
    DetailTabs(
        labels = subscriptions.map { it.shortTitle },
        selected = subscriptions.indexOf(selectedEsport).coerceAtLeast(0),
        style = DetailTabStyle.PAGE,
        onSelect = { onSelect(subscriptions[it]) },
    )
}

@Composable
private fun ProfileContent(viewModel: MiaopuViewModel, innerPadding: PaddingValues) {
    val uriHandler = LocalUriHandler.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = pagePadding(innerPadding),
    ) {
        item {
            PageHeading(
                title = "我的喵扑",
            )
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                cornerRadius = 24.dp,
                insideMargin = PaddingValues(20.dp),
                colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.primary,
                    contentColor = MiuixTheme.colorScheme.onPrimary,
                ),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("喵扑用户", style = MiuixTheme.textStyles.title2, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (viewModel.isLoggedIn) "已连接虎扑账号" else "登录后即可参与选手评分",
                            color = MiuixTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
                        )
                    }
                    Icon(LucideIcons.CircleUserRound, null, Modifier.size(32.dp))
                }
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = if (viewModel.isLoggedIn) viewModel::logout else viewModel::openLogin,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        color = MiuixTheme.colorScheme.onPrimary,
                        contentColor = MiuixTheme.colorScheme.primary,
                    ),
                ) {
                    Text(if (viewModel.isLoggedIn) "退出登录" else "登录虎扑账号")
                }
            }
        }
        item { SectionHeading("赛事订阅") }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "管理赛事订阅，当前 ${viewModel.subscribedEsports.size} 个项目"
                    },
                insideMargin = PaddingValues(18.dp),
                cornerRadius = 20.dp,
                onClick = viewModel::openSubscriptions,
                pressFeedbackType = PressFeedbackType.Sink,
                showIndication = true,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("管理赛事项目", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "已订阅 ${viewModel.subscribedEsports.size} 个赛事项目",
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                    Text(
                        "管理",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(LucideIcons.ChevronRight, null, Modifier.size(16.dp), tint = MiuixTheme.colorScheme.primary)
                }
            }
        }
        item { SectionHeading("应用") }
        item {
            val updateState = viewModel.updateCheckState
            val summary = when (updateState) {
                UpdateCheckState.Idle -> "当前版本 ${viewModel.currentVersion}"
                UpdateCheckState.Checking -> "正在查询 GitHub Release…"
                is UpdateCheckState.UpToDate -> "已是最新版本 · ${updateState.latestTag}"
                is UpdateCheckState.Available -> "发现新版本 ${updateState.release.tagName}"
                is UpdateCheckState.Failed -> updateState.message
            }
            ProfileActionCard(
                title = "检查更新",
                summary = summary,
                action = when (updateState) {
                    UpdateCheckState.Checking -> "检查中"
                    is UpdateCheckState.Available -> "下载"
                    is UpdateCheckState.Failed -> "重试"
                    else -> "检查"
                },
                enabled = updateState != UpdateCheckState.Checking,
                onClick = {
                    if (updateState is UpdateCheckState.Available) {
                        uriHandler.openUri(updateState.release.pageUrl)
                    } else {
                        viewModel.checkForUpdates()
                    }
                },
            )
        }
        item {
            ProfileActionCard(
                title = "关于",
                summary = "github.com/KiritoXDone/Miaopu · ${viewModel.currentVersion}",
                action = "查看",
                onClick = { uriHandler.openUri(viewModel.repositoryUrl) },
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ProfileActionCard(
    title: String,
    summary: String,
    action: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .semantics {
                role = Role.Button
                contentDescription = "$title，$summary"
            },
        insideMargin = PaddingValues(18.dp),
        cornerRadius = 20.dp,
        onClick = onClick.takeIf { enabled },
        pressFeedbackType = if (enabled) PressFeedbackType.Sink else PressFeedbackType.None,
        showIndication = enabled,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(
                    summary,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                action,
                style = MiuixTheme.textStyles.footnote1,
                color = if (enabled) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                },
                fontWeight = FontWeight.Bold,
            )
            Icon(LucideIcons.ChevronRight, null, Modifier.size(16.dp), tint = if (enabled) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    }
}

private fun pagePadding(innerPadding: PaddingValues) = PaddingValues(
    top = innerPadding.calculateTopPadding(),
    bottom = innerPadding.calculateBottomPadding() + 16.dp,
)

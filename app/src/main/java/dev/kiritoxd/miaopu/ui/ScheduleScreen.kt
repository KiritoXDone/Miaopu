package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import dev.kiritoxd.miaopu.data.searchSchedule
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
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ContactsCircle
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Tasks
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
fun ScheduleScreen(viewModel: MiaopuViewModel) {
    val sections = MainSection.entries
    val subscriptions = viewModel.subscribedEsports
    val pagerState = rememberPagerState(
        initialPage = viewModel.selectedMainSection.ordinal,
        pageCount = { sections.size },
    )
    val visibleSection = sections[pagerState.currentPage]
    val backState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = backState,
        isBackEnabled = viewModel.selectedMainSection != MainSection.HOME,
        onBackCompleted = { viewModel.selectMainSection(MainSection.HOME) },
    )
    LaunchedEffect(viewModel.selectedMainSection, pagerState) {
        val targetPage = viewModel.selectedMainSection.ordinal
        if (pagerState.settledPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }
    LaunchedEffect(pagerState, viewModel) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
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
                onSelect = viewModel::selectMainSection,
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

@Composable
private fun HomeSectionContent(
    viewModel: MiaopuViewModel,
    subscriptions: List<Esport>,
    innerPadding: PaddingValues,
) {
    val states = subscriptions.map(viewModel::scheduleStateFor)
    val readySchedules = states.mapNotNull { state -> (state as? LoadState.Ready)?.value }
    val failedStates = states.filterIsInstance<LoadState.Failed>()
    val mergedSchedule = remember(readySchedules) { mergeSchedules(readySchedules) }
    val bottomPadding = innerPadding.calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding()),
    ) {
        HomeHeader(viewModel)
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when {
                states.any { it is LoadState.Loading } -> LoadingPane(
                    label = "正在合并已订阅赛事",
                    modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding),
                )
                mergedSchedule.days.isNotEmpty() -> HomeContent(viewModel, mergedSchedule, bottomPadding)
                failedStates.isNotEmpty() -> ErrorPane(
                    message = failedStates.first().message,
                    retryable = failedStates.any(LoadState.Failed::retryable),
                    onRetry = viewModel::refreshHomeSchedules,
                    modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding),
                )
                else -> HomeContent(viewModel, mergedSchedule, bottomPadding)
            }
        }
    }
}

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
    val visibleEsport = subscriptions.getOrNull(pagerState.currentPage)
        ?: subscriptions[selectedEsportIndex]
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(viewModel.selectedEsport, subscriptions, pagerState) {
        val targetPage = subscriptions.indexOf(viewModel.selectedEsport).coerceAtLeast(0)
        if (pagerState.settledPage != targetPage) pagerState.animateScrollToPage(targetPage)
    }
    LaunchedEffect(pagerState, subscriptions, viewModel) {
        snapshotFlow { pagerState.settledPage }
            .map(subscriptions::getOrNull)
            .filterNotNull()
            .distinctUntilChanged()
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
private fun EventsPageContent(
    viewModel: MiaopuViewModel,
    esport: Esport,
    innerPadding: PaddingValues,
    searchQuery: String,
) {
    val bottomPadding = innerPadding.calculateBottomPadding()

    when (val state = viewModel.scheduleStateFor(esport)) {
        LoadState.Loading -> LoadingPane(
            label = "正在同步${esport.title}赛程",
            modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding),
        )
        is LoadState.Failed -> ErrorPane(
            message = state.message,
            retryable = state.retryable,
            onRetry = viewModel::retry,
            modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding),
        )
        is LoadState.Ready -> if (searchQuery.isBlank()) {
            EventsContent(viewModel, state.value, esport, bottomPadding)
        } else {
            val filteredSchedule = remember(state.value, searchQuery) {
                state.value.searchSchedule(searchQuery)
            }
            EventsScheduleSearchResults(
                schedule = filteredSchedule,
                query = searchQuery,
                bottomPadding = bottomPadding,
                onMatchClick = viewModel::openMatch,
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
            icon = MiuixIcons.Home,
            label = "首页",
        )
        NavigationBarItem(
            selected = selected == MainSection.EVENTS,
            onClick = { onSelect(MainSection.EVENTS) },
            icon = MiuixIcons.Tasks,
            label = "赛事",
        )
        NavigationBarItem(
            selected = selected == MainSection.PROFILE,
            onClick = { onSelect(MainSection.PROFILE) },
            icon = MiuixIcons.ContactsCircle,
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
private fun EventsContent(
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
                Icon(MiuixIcons.Refresh, contentDescription = "刷新赛程")
            }
        },
    )
}

@Composable
private fun EventsHeader(
    viewModel: MiaopuViewModel,
    esport: Esport,
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
                        MiuixIcons.Search,
                        contentDescription = if (searchExpanded) "关闭搜索" else "搜索赛程",
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = viewModel::refreshSchedule,
                    backgroundColor = MiuixTheme.colorScheme.surfaceContainer,
                ) {
                    Icon(MiuixIcons.Refresh, contentDescription = "刷新赛程")
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
            EsportSelector(viewModel, esport)
        }
    }
}

@Composable
private fun EsportSelector(viewModel: MiaopuViewModel, selectedEsport: Esport) {
    val subscriptions = viewModel.subscribedEsports
    DetailTabs(
        labels = subscriptions.map { it.shortTitle },
        selected = subscriptions.indexOf(selectedEsport).coerceAtLeast(0),
        style = DetailTabStyle.PAGE,
        onSelect = { viewModel.selectEsport(subscriptions[it]) },
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
                    Text("🐱", style = MiuixTheme.textStyles.title1)
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
                        "管理  ›",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
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
                    is UpdateCheckState.Available -> "下载 ›"
                    is UpdateCheckState.Failed -> "重试 ›"
                    else -> "检查 ›"
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
                action = "查看 ›",
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
        }
    }
}

private fun pagePadding(innerPadding: PaddingValues) = PaddingValues(
    top = innerPadding.calculateTopPadding(),
    bottom = innerPadding.calculateBottomPadding() + 16.dp,
)

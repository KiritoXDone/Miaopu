package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kiritoxd.miaopu.data.Esport
import dev.kiritoxd.miaopu.data.Schedule
import dev.kiritoxd.miaopu.data.searchSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton

@Composable
internal fun EventsPageContent(
    viewModel: MiaopuViewModel,
    esport: Esport,
    innerPadding: PaddingValues,
    searchQuery: String,
) {
    val bottomPadding = innerPadding.calculateBottomPadding()
    val state = viewModel.scheduleStateFor(esport)
    val incoming = (state as? LoadState.Ready)?.value
    var displayed by remember(esport) { mutableStateOf(viewModel.cachedScheduleFor(esport)) }
    val schedule = displayed ?: incoming
    SideEffect { if (displayed == null && incoming != null) displayed = incoming }
    val retry = { viewModel.refreshScheduleFor(esport) }
    Box(Modifier.fillMaxSize()) {
        if (schedule == null) {
            when (state) {
                is LoadState.Failed -> ErrorPane(state.message, state.retryable, retry,
                    Modifier.fillMaxSize().padding(bottom = bottomPadding))
                else -> LoadingPane("正在同步${esport.title}赛程", Modifier.fillMaxSize().padding(bottom = bottomPadding))
            }
        } else {
            if (searchQuery.isBlank()) {
                EventsContent(viewModel, schedule, esport, bottomPadding)
            } else {
                val result by produceState<Triple<Schedule, String, Schedule>?>(null, schedule, searchQuery) {
                    delay(120)
                    value = Triple(schedule, searchQuery, withContext(Dispatchers.Default) {
                        schedule.searchSchedule(searchQuery) { ensureActive() }
                    })
                }
                val current = result?.takeIf { it.first === schedule && it.second == searchQuery }
                if (current == null) LoadingPane("正在搜索赛程", Modifier.fillMaxSize().padding(bottom = bottomPadding))
                else EventsScheduleSearchResults(current.third, searchQuery, bottomPadding, viewModel::openMatch)
            }
            Box(Modifier.align(Alignment.TopCenter).padding(top = 8.dp)) {
                when {
                    state is LoadState.Loading -> Text("正在刷新赛程…")
                    state is LoadState.Failed -> if (state.retryable) {
                        TextButton("刷新失败，点击重试", onClick = retry)
                    } else Text("刷新失败，保留原赛程")
                    incoming != null && incoming != displayed ->
                        TextButton("赛程已更新，点击查看", onClick = { displayed = incoming })
                }
            }
        }
    }
}

package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kiritoxd.miaopu.data.*
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SubscriptionScreen(viewModel: MiaopuViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableIntStateOf(0) }
    var sorting by rememberSaveable { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(feedback) { if (feedback != null) { delay(2200); feedback = null } }
    val searching = query.isNotBlank()
    val categories = listOf("全部", "电竞", "篮球", "足球", "综合")
    val visible = EsportCatalog.all.filter { sport ->
        if (searching) listOf(sport.title, sport.shortTitle, sport.category.title).any { it.contains(query.trim(), ignoreCase = true) }
        else category == 0 || sport.category == ScheduleCategory.entries[category - 1]
    }
    val toggle: (Esport) -> Unit = { sport ->
        val subscribed = sport in viewModel.subscribedEsports
        if (subscribed && viewModel.subscribedEsports.size == 1) feedback = "至少保留一个赛事订阅"
        else {
            viewModel.toggleSubscription(sport)
            feedback = if (subscribed) "已取消订阅${sport.shortTitle}" else "已订阅${sport.shortTitle}"
        }
    }
    Scaffold(containerColor = MiuixTheme.colorScheme.surface, topBar = {
        SmallTopAppBar(title = "赛事订阅", navigationIcon = {
            IconButton(onClick = viewModel::goBack) { Icon(LucideIcons.ChevronLeft, "返回我的") }
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                TextField(value = query, onValueChange = { query = it }, label = "搜索赛事", useLabelAsPlaceholder = true,
                    singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    insideMargin = DpSize(14.dp, 12.dp), cornerRadius = 18.dp)
            }
            if (searching) {
                item { SubscriptionHeading("找到 ${visible.size} 项赛事") }
                item { SubscriptionGroup(visible, viewModel.subscribedEsports, onToggle = toggle) }
                if (visible.isEmpty()) item { SubscriptionHint("没有找到匹配的赛事") }
            } else {
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("已订阅  ${viewModel.subscribedEsports.size}", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        TextButton(if (sorting) "完成" else "排序", onClick = { sorting = !sorting }, minHeight = 28.dp,
                            insideMargin = PaddingValues(horizontal = 10.dp, vertical = 0.dp))
                    }
                }
                item {
                    SubscriptionGroup(viewModel.subscribedEsports, viewModel.subscribedEsports, sorting, toggle, viewModel::moveSubscription)
                }
                item { SubscriptionHint(if (sorting) "长按拖动手柄排序，也可使用上下按钮调整" else "订阅的赛事会显示在赛程首页") }
                item { SubscriptionHeading("发现赛事") }
                item { DetailTabs(categories, category) { category = it } }
                item { SubscriptionGroup(visible, viewModel.subscribedEsports, onToggle = toggle) }
            }
            feedback?.let { text -> item(key = "feedback") { SubscriptionHint(text, highlighted = true) } }
        }
    }
}

@Composable
private fun SubscriptionHeading(text: String) {
    Text(text, Modifier.padding(horizontal = 16.dp), fontSize = 17.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun SubscriptionHint(text: String, highlighted: Boolean = false) {
    Text(text, Modifier.padding(horizontal = 18.dp), fontSize = 12.sp,
        color = if (highlighted) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary)
}

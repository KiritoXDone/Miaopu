package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kiritoxd.miaopu.data.BottomBarStyle
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference

private val bottomBarStyleOptions = listOf(
    BottomBarStyle.CLASSIC to "经典",
    BottomBarStyle.FLOATING to "悬浮",
    BottomBarStyle.FLOATING_ICON to "悬浮（仅图标）",
    BottomBarStyle.LIQUID_GLASS to "液态玻璃",
    BottomBarStyle.OS4_GLASS to "OS4 玻璃",
)
private val bottomBarStyleLabels = bottomBarStyleOptions.map { it.second }

@Composable
internal fun AppearanceSettingsCard(style: BottomBarStyle, onStyleChange: (BottomBarStyle) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        insideMargin = PaddingValues(0.dp),
        cornerRadius = 20.dp,
    ) {
        OverlayDropdownPreference(
            title = "底栏样式",
            items = bottomBarStyleLabels,
            selectedIndex = bottomBarStyleOptions.indexOfFirst { it.first == style }.coerceAtLeast(0),
            onSelectedIndexChange = { index -> bottomBarStyleOptions.getOrNull(index)?.first?.let(onStyleChange) },
            insideMargin = PaddingValues(18.dp),
        )
    }
}

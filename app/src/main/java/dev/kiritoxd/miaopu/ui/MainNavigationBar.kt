package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.kiritoxd.miaopu.data.BottomBarStyle
import dev.kiritoxd.miaopu.ui.bottombar.liquid.IosLiquidGlassNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassNavigationBar
import top.yukonga.miuix.kmp.glass.GlassNavigationItem

private class NavigationDestination(val section: MainSection, val icon: ImageVector, val label: String)

private val destinations = listOf(
    NavigationDestination(MainSection.HOME, MiaopuIcons.House, "首页"),
    NavigationDestination(MainSection.EVENTS, MiaopuIcons.CalendarDays, "赛事"),
    NavigationDestination(MainSection.PROFILE, MiaopuIcons.CircleUserRound, "我的"),
)
private val navigationItems = destinations.map { NavigationItem(label = it.label, icon = it.icon) }
private val glassItems = destinations.map { GlassNavigationItem(icon = it.icon, label = it.label, contentDescription = it.label) }

@Composable
internal fun MainNavigationBar(
    selected: MainSection,
    style: BottomBarStyle,
    backdrop: LayerBackdrop?,
    onSelect: (MainSection) -> Unit,
) {
    val selectedIndex = destinations.indexOfFirst { it.section == selected }.coerceAtLeast(0)
    val selectIndex: (Int) -> Unit = { index -> destinations.getOrNull(index)?.let { onSelect(it.section) } }
    when (style) {
        BottomBarStyle.CLASSIC -> NavigationBar(showDivider = false) {
            destinations.forEach { item ->
                NavigationBarItem(
                    selected = selected == item.section,
                    onClick = { onSelect(item.section) },
                    icon = item.icon,
                    label = item.label,
                )
            }
        }
        BottomBarStyle.FLOATING_ICON -> FloatingNavigationBar {
            destinations.forEach { item ->
                FloatingNavigationBarItem(
                    selected = selected == item.section,
                    onClick = { onSelect(item.section) },
                    icon = item.icon,
                    label = item.label,
                )
            }
        }
        BottomBarStyle.FLOATING, BottomBarStyle.LIQUID_GLASS -> IosLiquidGlassNavigationBar(
            items = navigationItems,
            selectedIndex = selectedIndex,
            onItemClick = selectIndex,
            backdrop = backdrop.takeIf { style == BottomBarStyle.LIQUID_GLASS },
            isBlurActive = style == BottomBarStyle.LIQUID_GLASS && backdrop != null,
        )
        BottomBarStyle.OS4_GLASS -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
            GlassNavigationBar(
                items = glassItems,
                selectedIndex = selectedIndex,
                onSelect = selectIndex,
                backdrop = backdrop,
                modifier = Modifier.padding(horizontal = 24.dp).widthIn(max = 344.dp),
            )
        }
    }
}

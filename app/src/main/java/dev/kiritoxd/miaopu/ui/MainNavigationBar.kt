package dev.kiritoxd.miaopu.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem

private class NavigationDestination(val section: MainSection, val icon: ImageVector, val label: String)

private val destinations = listOf(
    NavigationDestination(MainSection.HOME, LucideIcons.House, "首页"),
    NavigationDestination(MainSection.EVENTS, LucideIcons.CalendarDays, "赛事"),
    NavigationDestination(MainSection.PROFILE, LucideIcons.CircleUserRound, "我的"),
)

@Composable
internal fun MainNavigationBar(
    selected: MainSection,
    floating: Boolean,
    onSelect: (MainSection) -> Unit,
) {
    if (floating) {
        FloatingNavigationBar {
            destinations.forEach { item ->
                FloatingNavigationBarItem(
                    selected = selected == item.section,
                    onClick = { onSelect(item.section) },
                    icon = item.icon,
                    label = item.label,
                )
            }
        }
    } else {
        NavigationBar(showDivider = false) {
            destinations.forEach { item ->
                NavigationBarItem(
                    selected = selected == item.section,
                    onClick = { onSelect(item.section) },
                    icon = item.icon,
                    label = item.label,
                )
            }
        }
    }
}

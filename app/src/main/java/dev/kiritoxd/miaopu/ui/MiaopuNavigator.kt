package dev.kiritoxd.miaopu.ui

import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavController
import top.yukonga.miuix.kmp.nav.core.navBackStackOf

internal class MiaopuNavigator(
    private val controller: NavController = NavController(navBackStackOf(AppScreen.Schedule)),
) {
    constructor(backStack: NavBackStack) : this(NavController(backStack))

    val backStack: NavBackStack get() = controller.backStack

    val currentScreen: AppScreen
        get() = backStack.last() as AppScreen

    fun push(screen: AppScreen) {
        if (backStack.none { (it as AppScreen).navigationContentKey == screen.navigationContentKey }) {
            controller.push(screen)
        }
    }

    fun replace(screen: AppScreen) {
        if (currentScreen != screen) controller.replace(screen)
    }

    fun contains(screen: AppScreen): Boolean =
        backStack.any { (it as AppScreen).navigationContentKey == screen.navigationContentKey }

    fun containsMatch(matchId: String): Boolean = backStack.any {
        when (val screen = it as AppScreen) {
            is AppScreen.Ratings -> screen.match.id == matchId
            is AppScreen.Stage -> screen.match.id == matchId
            else -> false
        }
    }

    fun pop(): Boolean = controller.pop()
}

internal val AppScreen.navigationContentKey: String
    get() = when (this) {
        AppScreen.Schedule -> "schedule"
        AppScreen.Subscriptions -> "subscriptions"
        is AppScreen.Ratings -> "ratings:${match.id}"
        is AppScreen.Stage -> buildString {
            append("stage:")
            append(match.id)
            append(':')
            append(stage.outBizType ?: match.outBizType)
            append(':')
            append(stage.outBizNo ?: match.outBizNo)
            append(':')
            append(stage.nodeId ?: stage.name)
        }
        is AppScreen.Comments -> "comments:${target.outBizType}:${target.outBizNo}"
        is AppScreen.Web -> "web:$login:$url"
    }

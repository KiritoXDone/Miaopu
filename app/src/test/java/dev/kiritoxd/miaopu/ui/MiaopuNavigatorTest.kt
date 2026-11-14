package dev.kiritoxd.miaopu.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import top.yukonga.miuix.kmp.nav.core.NavController
import top.yukonga.miuix.kmp.nav.core.navBackStackOf

class MiaopuNavigatorTest {
    @Test
    fun `navigator and display share the miuix controller stack`() {
        val controller = NavController(navBackStackOf(AppScreen.Schedule))
        val navigator = MiaopuNavigator(controller)

        navigator.push(AppScreen.Subscriptions)

        assertSame(controller.backStack, navigator.backStack)
        assertEquals(AppScreen.Subscriptions, controller.backStack.last())
        assertTrue(navigator.pop())
        assertEquals(AppScreen.Schedule, controller.backStack.last())
    }

    @Test
    fun `pop keeps the root screen`() {
        val navigator = MiaopuNavigator()

        assertFalse(navigator.pop())
        assertEquals(AppScreen.Schedule, navigator.currentScreen)
        assertEquals(1, navigator.backStack.size)
    }

    @Test
    fun `push and pop preserve screen order`() {
        val navigator = MiaopuNavigator()

        navigator.push(AppScreen.Subscriptions)

        assertEquals(AppScreen.Subscriptions, navigator.currentScreen)
        assertTrue(navigator.pop())
        assertEquals(AppScreen.Schedule, navigator.currentScreen)
    }

    @Test
    fun `push is idempotent for an existing content key`() {
        val navigator = MiaopuNavigator()
        val first = AppScreen.Web("登录", "https://hupu.com", login = true)
        val sameDestination = first.copy(title = "虎扑登录")

        navigator.push(first)
        navigator.push(sameDestination)

        assertEquals(2, navigator.backStack.size)
    }

    @Test
    fun `replace swaps only the top screen`() {
        val navigator = MiaopuNavigator()
        val web = AppScreen.Web("登录", "https://hupu.com", login = true)
        navigator.push(AppScreen.Subscriptions)

        navigator.replace(web)

        assertEquals(web, navigator.currentScreen)
        assertEquals(AppScreen.Schedule, navigator.backStack.first())
        assertEquals(2, navigator.backStack.size)
    }

    @Test
    fun `content keys are short and namespaced by route`() {
        val web = AppScreen.Web("登录", "https://hupu.com", login = true)

        assertEquals("schedule", AppScreen.Schedule.navigationContentKey)
        assertEquals("web:true:https://hupu.com", web.navigationContentKey)
    }

    @Test
    fun `navigator adopts a restored back stack`() {
        val web = AppScreen.Web("登录", "https://hupu.com", login = true)
        val navigator = MiaopuNavigator(navBackStackOf(AppScreen.Schedule, web))

        assertEquals(web, navigator.currentScreen)
        assertEquals(2, navigator.backStack.size)
    }
}

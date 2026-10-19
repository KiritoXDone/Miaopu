package dev.kiritoxd.miaopu.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetRefreshFeedbackTest {
    @Test fun singleMatchShowsRefreshProgressInsteadOfItsSportLabel() {
        assertEquals("正在刷新", widgetFooterText(WidgetRefreshStatus.RUNNING, false, true, false, "12:00", "CS2"))
        assertEquals("等待刷新", widgetFooterText(WidgetRefreshStatus.WAITING, false, true, false, "12:00", "CS2"))
        assertEquals("等待联网", widgetFooterText(WidgetRefreshStatus.WAITING, false, false, true, "12:00", "CS2"))
    }

    @Test fun failedRefreshIsVisibleEvenWhenCachedMatchesExist() {
        for (sport in listOf(null, "CS2")) {
            assertEquals("刷新失败 · 点击重试", widgetFooterText(WidgetRefreshStatus.IDLE, true, true, true, "12:00", sport))
            assertEquals("刷新失败 · 点击重试", widgetFooterText(WidgetRefreshStatus.FAILED, false, true, false, "12:00", sport))
        }
    }

    @Test fun successfulRefreshRestoresNormalFooterForBothWidgetSizes() {
        assertEquals("CS2", widgetFooterText(WidgetRefreshStatus.IDLE, false, true, false, "12:00", "CS2"))
        assertEquals("12:00 更新", widgetFooterText(WidgetRefreshStatus.IDLE, false, true, false, "12:00"))
    }

    @Test fun offlineAndStaleMatchesRemainIdentifiableAsCachedData() {
        assertEquals("离线 · 缓存 12:00", widgetFooterText(WidgetRefreshStatus.IDLE, false, false, true, "12:00", "CS2"))
        assertEquals("缓存 12:00", widgetFooterText(WidgetRefreshStatus.IDLE, false, true, true, "12:00", "CS2"))
        assertEquals("联网后自动更新", widgetFooterText(WidgetRefreshStatus.IDLE, false, false, true, ""))
    }
}

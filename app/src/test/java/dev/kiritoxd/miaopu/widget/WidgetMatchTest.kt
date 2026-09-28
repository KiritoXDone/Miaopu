package dev.kiritoxd.miaopu.widget

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class WidgetMatchTest {
    private val now = 1_790_640_000_000L
    private val hour = 3_600_000L

    @Test fun `prioritizes live then upcoming then latest results`() {
        val input = listOf(
            match("old", "已结束", now - 4 * hour), match("next", "未开始", now + hour),
            match("live", "进行中", now - hour), match("recent", "已结束", now - 2 * hour),
        )
        assertEquals(listOf("live", "next", "recent"), selectWidgetMatches(input, now).map { it.id })
    }

    @Test fun `excludes old seasons invalid dates and stale live flags`() {
        val input = listOf(match("old", "已结束", now - 49 * hour), match("invalid", "未开始", 0),
            match("old-live", "进行中", now - 13 * hour), match("far", "未开始", now + 8 * 24 * hour))
        assertTrue(selectWidgetMatches(input, now).isEmpty())
    }

    @Test fun `does not guess live from a scheduled start time`() {
        assertTrue(selectWidgetMatches(listOf(match("past", "未开始", now - hour)), now).isEmpty())
    }

    @Test fun `deduplicates shared source identities but not sport-local ids`() {
        val first = match("same", "未开始", now + hour)
        val other = first.copy(businessId = "val")
        assertEquals(2, selectWidgetMatches(listOf(first, first, other), now).size)
        assertEquals(1, selectWidgetMatches(listOf(first.copy(uniqueKey = "global"), other.copy(uniqueKey = "global")), now).size)
    }

    @Test fun `rejects unknown sports in restored cache`() {
        assertTrue(selectWidgetMatches(listOf(match("x", "进行中", now).copy(businessId = "removed")), now).isEmpty())
    }

    @Test fun `shows VS before start even when upstream uses zero scores`() {
        assertEquals("VS", match("x", "未开始", now).scoreLabel())
    }

    @Test fun `uses only provided scores without inferring format or stage`() {
        val match = match("x", "进行中", now)
        assertEquals("1 : 1", match.scoreLabel())
        assertEquals("— : 1", match.copy(teams = listOf(match.teams[0].copy(score = null), match.teams[1])).scoreLabel())
        assertFalse(match.scoreLabel().contains("BO"))
    }

    @Test fun `snapshot survives serialization and preserves rating destination and logo`() {
        val match = match("x", "已结束", now)
        val snapshot = WidgetScheduleSnapshot(listOf(match), now, now)
        val restored = Json.decodeFromString<WidgetScheduleSnapshot>(Json.encodeToString(snapshot))
        assertEquals(snapshot, restored)
        assertEquals("https://example.com/logo.png", restored.matches.first().toModel().teams.first().logoUrl)
        assertEquals("match-x", restored.matches.first().toModel().outBizNo)
        assertTrue(restored.matches.first().hasRatings)
        assertFalse(match.copy(outBizNo = null).hasRatings)
    }

    @Test fun `non-today matches include date and follow device timezone`() {
        val zone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
            val today = Calendar.getInstance().apply { set(2026, 8, 28, 10, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
            val tomorrow = Calendar.getInstance().apply { set(2026, 8, 29, 20, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
            assertEquals("10:00", match("x", "未开始", today).dateTimeLabel(today))
            assertEquals("09-29 20:00", match("x", "未开始", tomorrow).dateTimeLabel(today))
        } finally { TimeZone.setDefault(zone) }
    }

    private fun match(id: String, status: String, start: Long) = WidgetMatch(
        id, "lol", "T1 vs GEN", "全球总决赛", status, null, start, "20:00",
        listOf(WidgetTeam("T1", "https://example.com/logo.png", "1"), WidgetTeam("GEN", null, "1")),
        "lol", "match-$id", id,
    )
}

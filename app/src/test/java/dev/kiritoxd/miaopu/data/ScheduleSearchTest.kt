package dev.kiritoxd.miaopu.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ScheduleSearchTest {
    @Test(expected = java.util.concurrent.CancellationException::class)
    fun searchChecksCancellationWhileScanning() {
        val schedule = scheduleOf(match("1", "比赛", "决赛", "A", "B"))
        schedule.searchSchedule("A") { throw java.util.concurrent.CancellationException() }
    }

    @Test
    fun `blank query keeps the original schedule`() {
        val schedule = scheduleOf(match("1", "美国网球公开赛", "男单资格赛", "辛纳", "德约科维奇"))

        assertSame(schedule, schedule.searchSchedule("   "))
    }

    @Test
    fun `search matches event round team and player with multiple terms`() {
        val target = match("1", "羽毛球世锦赛", "男双决赛", "梁伟铿 王昶", "谢定峰 苏伟译")
        val other = match("2", "美国网球公开赛", "女单资格赛", "高芙", "郑钦文")
        val schedule = Schedule(
            anchorMatchId = target.id,
            days = listOf(
                ScheduleDay("2026-08-27", "8月27日 周四", listOf(target)),
                ScheduleDay("2026-08-28", "8月28日 周五", listOf(other)),
            ),
        )

        val result = schedule.searchSchedule("世锦赛 王昶 决赛")

        assertEquals(listOf(target.id), result.days.single().matches.map(MatchSummary::id))
        assertEquals(target.id, result.anchorMatchId)
    }

    @Test
    fun `search can match a featured player and clears an excluded anchor`() {
        val target = match("1", "F1荷兰站", "正赛", "迈凯伦", "法拉利").copy(
            featuredPlayer = FeaturedPlayer(
                name = "诺里斯",
                logoUrl = null,
                score = null,
                hotComment = null,
            ),
        )
        val schedule = scheduleOf(target).copy(anchorMatchId = "another-match")

        val result = schedule.searchSchedule("诺里斯")

        assertEquals(listOf(target.id), result.days.single().matches.map(MatchSummary::id))
        assertNull(result.anchorMatchId)
    }

    private fun scheduleOf(match: MatchSummary) = Schedule(
        anchorMatchId = match.id,
        days = listOf(ScheduleDay("2026-08-27", "8月27日 周四", listOf(match))),
    )

    private fun match(
        id: String,
        introduction: String,
        name: String,
        home: String,
        away: String,
    ) = MatchSummary(
        id = id,
        esport = Esport.BADMINTON,
        name = name,
        introduction = introduction,
        status = "未开始",
        startTimeMillis = 0L,
        startTimeLabel = "待定",
        teams = listOf(Team("home", home, null, null, false), Team("away", away, null, null, false)),
        scoreCountText = null,
        outBizType = null,
        outBizNo = null,
        featuredPlayer = null,
    )
}

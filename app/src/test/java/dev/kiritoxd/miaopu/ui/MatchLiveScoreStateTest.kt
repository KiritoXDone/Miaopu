package dev.kiritoxd.miaopu.ui

import dev.kiritoxd.miaopu.data.*
import dev.kiritoxd.miaopu.data.matchdetail.MatchLiveScore
import org.junit.Assert.*
import org.junit.Test

class MatchLiveScoreStateTest {
    private val match = MatchSummary("m", Esport.VALORANT, "比赛", "", "进行中", 0, "",
        listOf(Team("away", "Away", null, "0", false), Team("home", "Home", null, "0", false)),
        null, null, null, null)
    private val live = MatchLiveScore("m", "INPROGRESS", "进行中", "home", "away", "2", "1", "地图", "9:8", 5000)

    @Test fun scoresFollowTeamIdsRatherThanArrayOrder() {
        val updated = match.withLiveScore(live)
        assertEquals(listOf("1", "2"), updated.teams.map { it.bigScore })
        assertEquals("1 : 2", overallMatchScore(updated))
        assertEquals("0", match.teams.first().score)
    }

    @Test fun unavailableAndWrongMatchDataLeaveScheduleUntouched() {
        assertSame(match, match.withLiveScore(null))
        assertSame(match, match.withLiveScore(live.copy(matchId = "other")))
        assertEquals(match.teams, match.withLiveScore(live.copy(homeTeamId = "unknown", awayTeamId = "unknown")).teams)
    }
}

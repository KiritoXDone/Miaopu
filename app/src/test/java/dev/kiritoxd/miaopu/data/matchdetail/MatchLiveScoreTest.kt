package dev.kiritoxd.miaopu.data.matchdetail

import dev.kiritoxd.miaopu.data.AdapterResult
import dev.kiritoxd.miaopu.data.AdapterStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MatchLiveScoreTest {
    @Test fun liveMapAndRoundScoreAreSeparateFromSeriesScore() {
        val score = parseMatchLiveScore(fixture(), "match")
        assertEquals("1", score.homeScore)
        assertEquals("1", score.awayScore)
        assertEquals("阿努比斯", score.currentMap)
        assertEquals("13:9", score.currentScore)
        assertEquals(5_000L, score.pollMillis)
    }

    @Test fun finishedAndUpcomingMatchesHaveNoCurrentMapOrRoundScore() {
        for (status in listOf("NOTSTARTED", "COMPLETED")) {
            val score = parseMatchLiveScore(fixture(status), "match")
            assertNull(score.currentMap)
            assertNull(score.currentScore)
            if (status == "NOTSTARTED") assertNull(score.homeScore)
        }
    }

    @Test fun absentOptionalFieldsAndUnsafeIntervalsAreHandled() {
        val score = parseMatchLiveScore("""{"success":true,"result":{
            "matchId":"match","matchStatus":"INPROGRESS","pollTime":0,
            "currentPeriodDesc":null,"currentPeriodScoreDesc":"当前比分 "}}""", "match")
        assertNull(score.currentMap)
        assertNull(score.currentScore)
        assertNull(score.homeScore)
        assertEquals(5_000L, score.pollMillis)
        assertEquals(60_000L, parseMatchLiveScore(fixture().replace("5000", "999999"), "match").pollMillis)
    }

    @Test(expected = IllegalStateException::class)
    fun rejectWrongMatch() { parseMatchLiveScore(fixture(), "another") }

    @Test(expected = IllegalStateException::class)
    fun rejectBusinessFailure() { parseMatchLiveScore("""{"success":false}""", "match") }

    @Test fun initialThenPollingRequestsStopAtCompletion() = runBlocking {
        val calls = mutableListOf<Boolean>()
        val waits = mutableListOf<Long>()
        val values = mutableListOf<MatchLiveScore?>()
        pollMatchLiveScore(
            load = { polling ->
                calls += polling
                AdapterResult.success("fixture", parseMatchLiveScore(fixture(if (polling) "COMPLETED" else "INPROGRESS"), "match"))
            }, publish = { values += it }, wait = { waits += it },
        )
        assertEquals(listOf(false, true), calls)
        assertEquals(listOf(5_000L), waits)
        assertEquals("COMPLETED", values.last()?.status)
    }

    @Test fun failedRefreshClearsStaleDataAndBacksOff() = runBlocking {
        var calls = 0
        val waits = mutableListOf<Long>()
        val values = mutableListOf<MatchLiveScore?>()
        pollMatchLiveScore(load = {
            when (++calls) {
                2 -> AdapterResult.failure("fixture", AdapterStatus.TRANSIENT_FAILURE, "offline", true)
                else -> AdapterResult.success("fixture", parseMatchLiveScore(fixture(if (calls == 3) "COMPLETED" else "INPROGRESS"), "match"))
            }
        }, publish = { values += it }, wait = { waits += it })
        assertNull(values[1])
        assertEquals(listOf(5_000L, 15_000L), waits)
    }

    @Test fun cancellationDoesNotPublishAnErrorOrRetry() = runBlocking {
        var calls = 0
        try {
            pollMatchLiveScore(load = { calls++; throw CancellationException() },
                publish = { fail("Cancelled request must not publish") }, wait = { fail("Must not retry") })
            fail("Cancellation must propagate")
        } catch (_: CancellationException) {
            assertEquals(1, calls)
        }
    }

    private fun fixture(status: String = "INPROGRESS") = """{"success":true,"result":{
        "matchId":"match","matchStatus":"$status","pollTime":5000,
        "againstMatchBaseInfo":{"homeTeamId":"home","awayTeamId":"away","homeScore":"1","awayScore":"1"},
        "currentPeriodDesc":"阿努比斯","currentPeriodScoreDesc":"当前比分 13:9"}}"""
}

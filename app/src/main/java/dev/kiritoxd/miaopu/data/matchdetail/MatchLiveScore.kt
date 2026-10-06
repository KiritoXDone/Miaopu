package dev.kiritoxd.miaopu.data.matchdetail

import dev.kiritoxd.miaopu.data.AdapterResult
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import org.json.JSONObject

internal data class MatchLiveScore(
    val matchId: String,
    val status: String,
    val statusText: String?,
    val homeTeamId: String?,
    val awayTeamId: String?,
    val homeScore: String?,
    val awayScore: String?,
    val currentMap: String?,
    val currentScore: String?,
    val pollMillis: Long,
)

internal fun parseMatchLiveScore(body: String, expectedMatchId: String): MatchLiveScore {
    val root = JSONObject(body)
    check(root.optBoolean("success")) { "实时比分暂时不可用" }
    val result = root.getJSONObject("result")
    check(result.text("matchId") == expectedMatchId) { "实时比分与当前比赛不匹配" }
    val status = result.text("matchStatus") ?: error("实时比分缺少比赛状态")
    val teams = result.optJSONObject("againstMatchBaseInfo")
    val playing = status == "INPROGRESS"
    return MatchLiveScore(
        matchId = expectedMatchId,
        status = status,
        statusText = result.text("matchStatusShowText") ?: result.text("matchStatusDesc"),
        homeTeamId = teams?.text("homeTeamId"),
        awayTeamId = teams?.text("awayTeamId"),
        homeScore = teams?.text("homeScore").takeUnless { status == "NOTSTARTED" },
        awayScore = teams?.text("awayScore").takeUnless { status == "NOTSTARTED" },
        currentMap = result.text("currentPeriodDesc").takeIf { playing },
        currentScore = result.text("currentPeriodScoreDesc")?.removePrefix("当前比分")?.trim()
            ?.takeIf { playing && it.isNotBlank() },
        pollMillis = result.optLong("pollTime", 30_000L).coerceIn(5_000L, 60_000L),
    )
}

/** The page owns cancellation; a failed refresh hides stale live data and retries more slowly. */
internal suspend fun pollMatchLiveScore(
    load: suspend (polling: Boolean) -> AdapterResult<MatchLiveScore>,
    publish: (MatchLiveScore?) -> Unit,
    wait: suspend (Long) -> Unit = { delay(it) },
) {
    var polling = false
    while (true) {
        currentCoroutineContext().ensureActive()
        val score = load(polling).data
        currentCoroutineContext().ensureActive()
        publish(score)
        if (score?.status == "COMPLETED") return
        wait(score?.pollMillis ?: 15_000L)
        polling = true
    }
}

private fun JSONObject.text(key: String): String? =
    if (isNull(key)) null else optString(key).trim().takeIf(String::isNotEmpty)

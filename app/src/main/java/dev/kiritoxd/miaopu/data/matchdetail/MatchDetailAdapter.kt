package dev.kiritoxd.miaopu.data.matchdetail

import dev.kiritoxd.miaopu.data.Esport
import dev.kiritoxd.miaopu.data.AdapterResult
import dev.kiritoxd.miaopu.data.AdapterStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import dev.kiritoxd.miaopu.data.HupuHttpTransport
import okhttp3.Request
import java.net.URLEncoder

/** Read-only public endpoints; no session cookie or captured device credentials. */
internal interface MatchDetailSource {
    suspend fun allScores(matchId: String, businessType: String): AdapterResult<MatchAllScores>
    suspend fun playerScores(matchId: String, game: Esport): AdapterResult<MatchAllScores>
    suspend fun stats(matchId: String, mapId: String): AdapterResult<MatchStats>
}

internal class MatchDetailAdapter : MatchDetailSource {
    override suspend fun allScores(matchId: String, businessType: String): AdapterResult<MatchAllScores> = get(
        "https://match-api.hupu.com/1/8.2.58/matchallapi/queryMatchAllScoreInfo" +
            "?businessType=${encode(businessType)}&matchId=${encode(matchId)}", MatchDetailParser::allScores,
    )

    override suspend fun playerScores(matchId: String, game: Esport): AdapterResult<MatchAllScores> {
        require(game == Esport.LOL || game == Esport.KOG)
        return get("https://games.mobileapi.hupu.com/1/8.2.58/player/v1/${game.businessId}/getAllPlayerScore" +
            "?matchId=${encode(matchId)}", MatchDetailParser::playerScores)
    }

    override suspend fun stats(matchId: String, mapId: String): AdapterResult<MatchStats> = get(
        "https://match-api.hupu.com/1/8.0.0/matchallapi/stat/queryMatchStatsByMatchInfo/v2" +
            "?matchId=${encode(matchId)}&boNumber=${encode(mapId)}", MatchDetailParser::stats,
    )

    private suspend fun <T> get(url: String, parser: (String) -> T): AdapterResult<T> {
        val response = try {
            HupuHttpTransport.execute(Request.Builder().url(url).header("Accept", "application/json").build())
        } catch (_: IOException) {
            return AdapterResult.failure(url, AdapterStatus.TRANSIENT_FAILURE, "连接失败，请稍后重试", true)
        }
        val code = response.status
        if (code !in 200..299) {
            return AdapterResult.failure(url, AdapterStatus.TRANSIENT_FAILURE, "比赛数据暂时不可用", true, code)
        }
        return withContext(Dispatchers.Default) {
            try {
                AdapterResult.success(url, parser(response.body))
            } catch (_: org.json.JSONException) {
                AdapterResult.failure(url, AdapterStatus.INVALID_RESPONSE, "比赛数据格式已变化", true, code)
            } catch (error: CancellationException) {
                throw error
            } catch (error: IllegalStateException) {
                AdapterResult.failure(url, AdapterStatus.INVALID_RESPONSE, error.message ?: "暂无比赛数据", true, code)
            }
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}

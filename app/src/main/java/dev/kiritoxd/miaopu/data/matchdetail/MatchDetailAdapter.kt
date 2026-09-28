package dev.kiritoxd.miaopu.data.matchdetail

import dev.kiritoxd.miaopu.data.AdapterResult
import dev.kiritoxd.miaopu.data.AdapterStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

/** Read-only public endpoints; no session cookie or captured device credentials. */
internal interface MatchDetailSource {
    suspend fun allScores(matchId: String, businessType: String): AdapterResult<MatchAllScores>
    suspend fun stats(matchId: String, mapId: String): AdapterResult<MatchStats>
}

internal class MatchDetailAdapter : MatchDetailSource {
    override suspend fun allScores(matchId: String, businessType: String): AdapterResult<MatchAllScores> = get(
        "https://match-api.hupu.com/1/8.2.58/matchallapi/queryMatchAllScoreInfo" +
            "?businessType=${encode(businessType)}&matchId=${encode(matchId)}", MatchDetailParser::allScores,
    )

    override suspend fun stats(matchId: String, mapId: String): AdapterResult<MatchStats> = get(
        "https://match-api.hupu.com/1/8.0.0/matchallapi/stat/queryMatchStatsByMatchInfo/v2" +
            "?matchId=${encode(matchId)}&boNumber=${encode(mapId)}", MatchDetailParser::stats,
    )

    private suspend fun <T> get(url: String, parser: (String) -> T): AdapterResult<T> = withContext(Dispatchers.IO) {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 12_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            val code = connection.responseCode
            if (code !in 200..299) {
                AdapterResult.failure(url, AdapterStatus.TRANSIENT_FAILURE, "比赛数据暂时不可用", true, code)
            } else {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                try {
                    AdapterResult.success(url, parser(body))
                } catch (_: org.json.JSONException) {
                    AdapterResult.failure(url, AdapterStatus.INVALID_RESPONSE, "比赛数据格式已变化", true, code)
                } catch (error: IllegalStateException) {
                    AdapterResult.failure(url, AdapterStatus.INVALID_RESPONSE, error.message ?: "暂无比赛数据", true, code)
                }
            }
        } catch (_: IOException) {
            AdapterResult.failure(url, AdapterStatus.TRANSIENT_FAILURE, "连接失败，请稍后重试", true)
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}

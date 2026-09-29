package dev.kiritoxd.miaopu.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.HttpsURLConnection

class HupuAdapter(
    private val cookieSession: HupuCookieSession,
) {
    private val commonScheduleMutex = Mutex()
    private val commonScheduleGeneration = AtomicLong()

    @Volatile
    private var commonScheduleCache: CachedHttpResponse? = null

    fun invalidateSharedScheduleCache() {
        commonScheduleGeneration.incrementAndGet()
        commonScheduleCache = null
    }

    suspend fun getSchedule(esport: Esport): AdapterResult<Schedule> = coroutineScope {
        val primaryDeferred = esport.primaryScheduleBusinessId?.let { businessId ->
            async { getScheduleSource(businessId, esport) }
        }
        val supplementalDeferreds = esport.supplementalSources.map { source ->
            source to async { getScheduleSource(source.businessId, esport) }
        }
        val primaryResult = primaryDeferred?.await()
        val supplementalResults = supplementalDeferreds.map { (source, deferred) ->
            source to deferred.await()
        }
        val supplementalSchedules = supplementalResults.mapNotNull { (source, result) ->
            result.data?.filterMatches { match -> source.includes(match.introduction) }
        }
        val schedules = buildList {
            primaryResult?.data?.let(::add)
            addAll(supplementalSchedules.filter { it.days.isNotEmpty() })
        }

        if (schedules.isEmpty()) {
            primaryResult?.let { return@coroutineScope it }
            supplementalSchedules.firstOrNull()?.let { emptySchedule ->
                return@coroutineScope AdapterResult.success(
                    source = "hupu.schedule.${esport.businessId}",
                    data = emptySchedule,
                )
            }
            return@coroutineScope supplementalResults
                .firstNotNullOfOrNull { (_, result) ->
                    result.takeIf { it.status != AdapterStatus.SUCCESS }
                }
                ?: AdapterResult.failure(
                    source = "hupu.schedule.${esport.businessId}",
                    status = AdapterStatus.NOT_CONFIGURED,
                    message = "这个项目暂时没有赛程来源",
                    retryable = false,
                )
        }

        AdapterResult.success(
            source = "hupu.schedule.${esport.businessId}",
            data = schedules.singleOrNull() ?: mergeSchedules(schedules),
        )
    }

    private suspend fun getScheduleSource(
        businessId: String,
        esport: Esport,
    ): AdapterResult<Schedule> {
        val endpoint = "${HupuUrls.SCHEDULE_BASE}/1/8.2.10/matchallapi/bff/standard/getScheduleListByTagForH5" +
            "?businessType=common&businessId=${encode(businessId)}&datasource=navigation"
        val response = requestSchedule(endpoint, businessId)
        return withContext(Dispatchers.Default) {
            response.toAdapterResult("hupu.schedule.$businessId") { HupuJsonParser.schedule(it, esport) }
        }
    }

    suspend fun getRatings(match: MatchSummary): AdapterResult<RatingDetail> {
        val type = match.outBizType ?: return AdapterResult.failure(
            source = "hupu.ratings",
            status = AdapterStatus.NOT_CONFIGURED,
            message = "这场比赛没有评分入口",
            retryable = false,
        )
        val number = match.outBizNo ?: return AdapterResult.failure(
            source = "hupu.ratings",
            status = AdapterStatus.NOT_CONFIGURED,
            message = "这场比赛没有评分编号",
            retryable = false,
        )
        val endpoint = "${HupuUrls.SCORE_BASE}/bplcommentapi/bpl/score_tree/getCurAndSubNodeByBizKey" +
            "?outBizType=${encode(type)}&outBizNo=${encode(number)}&relation=CHILD&page=1&pageSize=100"
        return getAndParse(endpoint, "hupu.ratings.$type") { HupuJsonParser.ratingDetail(it) }
    }

    suspend fun getStageRatingDetail(
        match: MatchSummary,
        stage: RatingStage,
    ): AdapterResult<StageRatingDetail> = coroutineScope {
        val type = stage.outBizType ?: match.outBizType ?: return@coroutineScope AdapterResult.failure(
            source = "hupu.stage",
            status = AdapterStatus.NOT_CONFIGURED,
            message = "这一局没有评分入口",
            retryable = false,
        )
        val number = stage.outBizNo ?: match.outBizNo ?: return@coroutineScope AdapterResult.failure(
            source = "hupu.stage",
            status = AdapterStatus.NOT_CONFIGURED,
            message = "这一局没有评分编号",
            retryable = false,
        )
        val treeEndpoint = "${HupuUrls.SCORE_BASE}/bplcommentapi/bpl/score_tree/getCurAndSubNodeByBizKey" +
            "?outBizType=${encode(type)}&outBizNo=${encode(number)}&relation=CHILD&page=1&pageSize=100"
        val groupsEndpoint = "${HupuUrls.SCORE_BASE}/bplcommentapi/bpl/score_tree/getSubGroups" +
            "?outBizType=${encode(type)}&outBizNo=${encode(number)}"

        val treeDeferred = async {
            getAndParse(treeEndpoint, "hupu.stage.$type") { HupuJsonParser.stageRatingDetail(it) }
        }
        val populatedGroupsDeferred = async {
            val groups = getAndParse(groupsEndpoint, "hupu.stage.groups.$type") {
                HupuJsonParser.ratingGroups(it)
            }.data.orEmpty().sortedWith(
                compareBy<RatingGroup> { if (it.name == "趣评") 0 else 1 }.thenByDescending { it.sort },
            )
            groups.map { group ->
                async groupTask@{
                    val endpoint = "${HupuUrls.SCORE_BASE}/bplcommentapi/bff/bpl/score_tree/groupAndSubNodes" +
                        "?nodeId=${group.rootNodeId}&queryType=hot&page=1&pageSize=100"
                    // Start the independent network read before waiting for the tree's display title.
                    val response = request(endpoint)
                    val detail = treeDeferred.await().data ?: return@groupTask group
                    val targets = withContext(Dispatchers.Default) {
                        response.toAdapterResult("hupu.stage.group.${group.rootNodeId}") {
                            HupuJsonParser.ratingGroupTargets(it, detail.title)
                        }.data.orEmpty()
                    }
                    group.copy(targets = targets)
                }
            }.map { it.await() }
        }
        val treeResult = treeDeferred.await()
        val detail = treeResult.data ?: run {
            populatedGroupsDeferred.cancel()
            return@coroutineScope treeResult
        }
        val populatedGroups = populatedGroupsDeferred.await()

        AdapterResult.success(
            source = "hupu.stage.$type",
            data = detail.copy(groups = populatedGroups),
        )
    }

    suspend fun getComments(
        target: RatingTarget,
        publishTime: Long = System.currentTimeMillis(),
    ): AdapterResult<CommentPage> {
        val endpoint = "${HupuUrls.SCORE_BASE}/bplcommentapi/bpl/comment/list/primarySingleRow" +
            "?publishTime=$publishTime&order=desc" +
            "&outBizType=${encode(target.outBizType)}&outBizNo=${encode(target.outBizNo)}" +
            "&clientCode=&cid="
        return getAndParse(endpoint, "hupu.comments.${target.outBizType}") { HupuJsonParser.comments(it) }
    }

    suspend fun getHottestComments(target: RatingTarget): AdapterResult<List<HupuComment>> {
        val endpoint = "${HupuUrls.SCORE_BASE}/bplcommentapi/bpl/comment/list/primarySingleRow/hottest" +
            "?outBizType=${encode(target.outBizType)}&outBizNo=${encode(target.outBizNo)}" +
            "&clientCode="
        return getAndParse(endpoint, "hupu.comments.hottest.${target.outBizType}") {
            HupuJsonParser.hottestComments(it)
        }
    }

    /**
     * Loads the replies hidden behind a score comment's “X 条回复” affordance.
     * This mirrors Hupu's current inline expansion request rather than the
     * unrelated BBS thread-reply API used by general forum clients.
     */
    suspend fun getCommentReplies(
        target: RatingTarget,
        parentCommentId: String,
        publishTime: Long = INLINE_REPLY_INITIAL_CURSOR,
    ): AdapterResult<CommentPage> {
        val endpoint = "${HupuUrls.SCORE_BASE}/bplcommentapi/bpl/comment/list/primarySingleRow/getMore" +
            "?publishTime=$publishTime" +
            "&outBizType=${encode(target.outBizType)}&outBizNo=${encode(target.outBizNo)}" +
            "&parentCommentId=${encode(parentCommentId)}&pageSize=$INLINE_REPLY_PAGE_SIZE" +
            "&clientCode=&cid="
        return getAndParse(endpoint, "hupu.comments.replies.${target.outBizType}") {
            HupuJsonParser.comments(it)
        }
    }

    suspend fun submitScore(target: RatingTarget, score: Int): AdapterResult<Unit> {
        require(score in 2..10 && score % 2 == 0) { "虎扑评分必须是 2、4、6、8 或 10" }
        if (!cookieSession.isAuthenticated()) return AdapterResult.failure(
            source = "hupu.score.write",
            status = AdapterStatus.AUTH_REQUIRED,
            message = "请先登录虎扑",
            retryable = false,
        )
        val body = JSONObject()
            .put(
                "outBizKey",
                JSONObject()
                    .put("outBizType", target.outBizType)
                    .put("outBizNo", target.outBizNo),
            )
            .put("score", score)
            .put("source", "")
            .toString()
        val endpoint = "${HupuUrls.SCORE_BASE}/bplcommentapi/bpl/score/save"
        val response = request(endpoint, "POST", body)
        return withContext(Dispatchers.Default) {
            response.toAdapterResult("hupu.score.write", ::parseWriteResponse)
        }
    }

    suspend fun publishComment(target: RatingTarget, content: String): AdapterResult<Unit> {
        val cleaned = content.trim()
        require(cleaned.isNotEmpty()) { "评论内容不能为空" }
        if (!cookieSession.isAuthenticated()) return AdapterResult.failure(
            source = "hupu.comment.write",
            status = AdapterStatus.AUTH_REQUIRED,
            message = "请先在“我的”中登录虎扑",
            retryable = false,
        )
        val body = JSONObject()
            .put("content", cleaned)
            .put(
                "outBizKey",
                JSONObject()
                    .put("outBizType", target.outBizType)
                    .put("outBizNo", target.outBizNo),
            )
            .put("subjectId", "")
            .put("source", "m")
            .toString()
        val endpoint = "${HupuUrls.SCORE_BASE}/bplcommentapi/bpl/comment/m/publish"
        val response = request(endpoint, "POST", body)
        return withContext(Dispatchers.Default) {
            response.toAdapterResult("hupu.comment.write", ::parseWriteResponse)
        }
    }

    suspend fun publishReply(target: RatingTarget, parentId: String, content: String): AdapterResult<Unit> =
        writeCommentAction("publish", CommentWritePayloads.reply(target, parentId, content))

    suspend fun setCommentLight(comment: HupuComment, light: Boolean): AdapterResult<Unit> =
        writeCommentAction(if (light) "light" else "cancelLight", CommentWritePayloads.light(comment.id, comment.subjectId))

    private suspend fun writeCommentAction(action: String, body: String): AdapterResult<Unit> {
        val source = "hupu.comment.$action"
        if (!cookieSession.isAuthenticated()) return AdapterResult.failure(
            source, AdapterStatus.AUTH_REQUIRED, "请先登录虎扑", false,
        )
        val response = request("https://games.mobileapi.hupu.com/1/8.2.58/bplcommentapi/bpl/comment/$action", "POST", body)
        return withContext(Dispatchers.Default) { response.toAdapterResult(source, ::parseWriteResponse) }
    }

    private suspend fun <T> getAndParse(
        endpoint: String,
        source: String,
        parser: (String) -> T,
    ): AdapterResult<T> {
        val response = request(endpoint)
        return withContext(Dispatchers.Default) {
            response.toAdapterResult(source, parser)
        }
    }

    private fun parseWriteResponse(response: String) {
        val json = JSONObject(response)
        if (json.optInt("code") != 1) error(json.optString("msg", "提交失败"))
    }

    private suspend fun request(
        endpoint: String,
        method: String = "GET",
        body: String? = null,
    ): HttpResponse = withContext(Dispatchers.IO) {
        val connection = (URL(endpoint).openConnection() as HttpsURLConnection).apply {
            requestMethod = method
            connectTimeout = 12_000
            readTimeout = 15_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("reqId", UUID.randomUUID().toString())
            setRequestProperty("Referer", HupuUrls.DETAIL_BASE)
            cookieSession.cookieHeader().takeIf(String::isNotBlank)?.let { setRequestProperty("Cookie", it) }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            body?.let { connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer -> writer.write(it) } }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            HttpResponse(status, responseBody)
        } catch (error: IOException) {
            HttpResponse(-1, "", error.message ?: "网络连接失败")
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun requestSchedule(endpoint: String, businessId: String): HttpResponse {
        if (businessId != ScheduleSource.COMMON_HOT_SPORTS_BUSINESS_ID) return request(endpoint)

        val now = System.currentTimeMillis()
        commonScheduleCache?.takeIf { now - it.receivedAtMillis <= COMMON_SCHEDULE_CACHE_MILLIS }
            ?.let { return it.response }

        commonScheduleMutex.lock()
        try {
            val lockedNow = System.currentTimeMillis()
            val lockedGeneration = commonScheduleGeneration.get()
            commonScheduleCache?.takeIf {
                lockedNow - it.receivedAtMillis <= COMMON_SCHEDULE_CACHE_MILLIS
            }?.let { return it.response }

            return request(endpoint).also { response ->
                if (commonScheduleGeneration.get() == lockedGeneration) {
                    commonScheduleCache = CachedHttpResponse(lockedNow, response)
                }
            }
        } finally {
            commonScheduleMutex.unlock()
        }
    }

    private suspend fun <T> HttpResponse.toAdapterResult(
        source: String,
        parser: (String) -> T,
    ): AdapterResult<T> = try {
        when {
            transportError != null ->
                AdapterResult.failure(source, AdapterStatus.TRANSIENT_FAILURE, transportError, true)
            status == HttpURLConnection.HTTP_UNAUTHORIZED || status == HttpURLConnection.HTTP_FORBIDDEN ->
                AdapterResult.failure(source, AdapterStatus.AUTH_REQUIRED, "虎扑登录状态已失效", false, status)
            status !in 200..299 ->
                AdapterResult.failure(source, AdapterStatus.TRANSIENT_FAILURE, "虎扑服务暂时不可用", true, status)
            else -> AdapterResult.success(source, parser(body))
        }
    } catch (error: IOException) {
        AdapterResult.failure(source, AdapterStatus.TRANSIENT_FAILURE, error.message ?: "网络连接失败", true)
    } catch (error: Exception) {
        AdapterResult.failure(source, AdapterStatus.INVALID_RESPONSE, error.message ?: "虎扑响应格式已变化", false, status)
    }

    private data class HttpResponse(
        val status: Int,
        val body: String,
        val transportError: String? = null,
    )

    private data class CachedHttpResponse(
        val receivedAtMillis: Long,
        val response: HttpResponse,
    )

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

    private companion object {
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Miaopu/1.0) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36"
        const val INLINE_REPLY_INITIAL_CURSOR = 31_507_200_000L
        const val INLINE_REPLY_PAGE_SIZE = 20
        const val COMMON_SCHEDULE_CACHE_MILLIS = 60_000L
    }
}

private fun Schedule.filterMatches(predicate: (MatchSummary) -> Boolean): Schedule {
    val filteredDays = days.mapNotNull { day ->
        day.copy(matches = day.matches.filter(predicate)).takeIf { it.matches.isNotEmpty() }
    }
    val retainedMatchIds = filteredDays.flatMap(ScheduleDay::matches).mapTo(hashSetOf(), MatchSummary::id)
    return copy(
        anchorMatchId = anchorMatchId?.takeIf { it in retainedMatchIds },
        days = filteredDays,
    )
}

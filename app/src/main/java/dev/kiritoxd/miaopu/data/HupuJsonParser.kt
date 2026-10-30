package dev.kiritoxd.miaopu.data

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HupuJsonParser {
    fun schedule(json: String, esport: Esport): Schedule {
        val result = JSONObject(json).requiredObject("result")
        val daysJson = result.optJSONArray("dayGameData") ?: JSONArray()
        val days = buildList {
            for (dayIndex in 0 until daysJson.length()) {
                val day = daysJson.optJSONObject(dayIndex) ?: continue
                val matchesJson = day.optJSONArray("matchData") ?: JSONArray()
                val matches = buildList {
                    for (matchIndex in 0 until matchesJson.length()) {
                        matchesJson.optJSONObject(matchIndex)
                            ?.let { parseMatch(it, esport) }
                            ?.takeUnless { it.isSchedulePlaceholder() }
                            ?.let(::add)
                    }
                }
                if (matches.isNotEmpty()) {
                    add(
                        ScheduleDay(
                            date = day.stringOrNull("dayTime").orEmpty(),
                            label = day.stringOrNull("dateBlock").orEmpty(),
                            matches = matches,
                        ),
                    )
                }
            }
        }
        val retainedMatchIds = days
            .asSequence()
            .flatMap { it.matches.asSequence() }
            .flatMap { sequenceOf(it.id, it.uniqueKey) }
            .toSet()
        return Schedule(
            anchorMatchId = result.stringOrNull("anchorMatchId")?.takeIf(retainedMatchIds::contains),
            days = days,
        )
    }

    fun ratingDetail(json: String): RatingDetail {
        val data = JSONObject(json).requiredObject("data")
        val self = data.optJSONObject("self")
        val selfNode = self?.optJSONObject("node")
        val title = selfNode
            ?.stringOrNull("name")
            ?: data.optJSONObject("detail")?.stringOrNull("name")
            ?: "选手评分"

        val stages = mutableListOf<RatingStage>()
        val flatTargets = mutableListOf<RatingTarget>()
        val seen = mutableSetOf<String>()
        val pageData = data.optJSONObject("pageResult")?.optJSONArray("data")
        if (pageData != null) {
            for (index in 0 until pageData.length()) {
                val group = pageData.optJSONObject(index) ?: continue
                val groupNode = group.optJSONObject("node")
                val groupName = groupNode?.stringOrNull("name") ?: "评分"
                val subNodes = group.optJSONArray("subNodes")
                val groupTarget = groupNode?.toRatingTarget(group.optLongOrNull("nodeId"))

                if (
                    groupTarget?.canScore == true &&
                    groupTarget.isRenderable &&
                    (subNodes == null || subNodes.length() == 0)
                ) {
                    if (seen.add(groupTarget.key)) {
                        flatTargets += groupTarget.copy(stageName = "全场")
                    }
                    continue
                }

                val targets = mutableListOf<RatingTarget>()
                groupTarget?.takeIf { it.canScore && it.isRenderable }?.let { target ->
                    if (seen.add(target.key)) targets += target.copy(stageName = groupName)
                }
                collectTargets(subNodes, targets, seen, groupName)
                if (targets.isNotEmpty()) {
                    stages += RatingStage(
                        name = groupName,
                        targets = targets,
                        outBizType = groupNode?.stringOrNull("bizType"),
                        outBizNo = groupNode?.stringOrNull("bizId"),
                        nodeId = group.optLongOrNull("nodeId"),
                        targetCount = maxOf(group.optInt("subNodeCount", 0), targets.size),
                    )
                }
            }
        }

        if (flatTargets.isNotEmpty()) {
            stages.add(
                0,
                RatingStage(
                    name = "全场",
                    targets = flatTargets,
                    outBizType = selfNode?.stringOrNull("bizType"),
                    outBizNo = selfNode?.stringOrNull("bizId"),
                    nodeId = self?.optLongOrNull("nodeId"),
                    targetCount = maxOf(
                        data.optJSONObject("pageResult")?.optInt("totalCount", 0) ?: 0,
                        flatTargets.size,
                    ),
                ),
            )
        }

        if (stages.isEmpty()) {
            val direct = data.optJSONObject("detail")?.toRatingTarget()
            if (direct?.isRenderable == true) {
                stages += RatingStage("评分", listOf(direct.copy(stageName = "评分")))
            }
        }
        return RatingDetail(title, stages)
    }

    fun stageRatingDetail(json: String): StageRatingDetail {
        val data = JSONObject(json).requiredObject("data")
        val selfNode = data.optJSONObject("self")?.optJSONObject("node")
        val title = selfNode?.stringOrNull("name") ?: "单局详情"
        val targets = parseTargetWrappers(
            nodes = data.optJSONObject("pageResult")?.optJSONArray("data"),
            stageName = title,
        )
        return StageRatingDetail(
            title = title,
            description = selfNode?.optJSONObject("infoJson")?.firstArrayString("desc"),
            imageUrl = selfNode?.optJSONArray("image")?.stringOrNull(0),
            targets = targets,
            groups = emptyList(),
        )
    }

    fun ratingGroups(json: String): List<RatingGroup> {
        val groups = JSONObject(json).optJSONArray("data") ?: JSONArray()
        return buildList {
            for (index in 0 until groups.length()) {
                val group = groups.optJSONObject(index) ?: continue
                val rootNodeId = group.optLongOrNull("rootNodeId") ?: continue
                add(
                    RatingGroup(
                        id = group.optLongOrNull("groupId") ?: group.optLongOrNull("id"),
                        name = group.stringOrNull("groupName") ?: group.stringOrNull("name") ?: "分组",
                        rootNodeId = rootNodeId,
                        sort = group.optInt("sort", index),
                        logoUrl = HupuUrls.imageUrl(group.attributeValue("logo")),
                        teamId = group.attributeValue("teamId"),
                        childCount = group.optInt("childCount", 0),
                    ),
                )
            }
        }
    }

    fun ratingGroupTargets(json: String, stageName: String): List<RatingTarget> {
        val data = JSONObject(json).requiredObject("data")
        return parseTargetWrappers(
            nodes = data.optJSONObject("nodePageResult")?.optJSONArray("data"),
            stageName = stageName,
        )
    }

    fun comments(json: String): CommentPage {
        val data = JSONObject(json).requiredObject("data")
        val commentsJson = data.optJSONArray("comments") ?: JSONArray()
        val comments = parseComments(commentsJson)
        return CommentPage(
            comments = comments,
            totalCount = data.optInt("commentCount", comments.size),
            nextPublishTime = data.optJSONObject("cursor")?.optLongOrNull("publishTime"),
            hasMore = data.optBoolean("hasMore", false),
        )
    }

    fun hottestComments(json: String): List<HupuComment> {
        val data = JSONObject(json).optJSONArray("data") ?: JSONArray()
        return parseComments(data)
    }

    private fun parseMatch(json: JSONObject, esport: Esport): MatchSummary {
        val startMillis = json.stringOrNull("matchStartTimeStamp")?.toLongOrNull() ?: 0L
        val matchId = json.stringOrNull("matchId").orEmpty()
        val uniqueKey = json.stringOrNull("uniqueKey")
            ?: json.stringOrNull("businessType")
                ?.takeIf { matchId.isNotBlank() }
                ?.let { businessType -> "$businessType:$matchId" }
            ?: matchId
        val winnerId = json.optJSONObject("againstInfo")?.stringOrNull("winnerMemberId")
        val members = json.optJSONObject("againstInfo")?.optJSONArray("memberInfos") ?: JSONArray()
        val teams = buildList {
            for (index in 0 until members.length()) {
                val member = members.optJSONObject(index) ?: continue
                val id = member.stringOrNull("memberId").orEmpty()
                val name = member.stringOrNull("memberName")
                val logoUrl = HupuUrls.imageUrl(member.stringOrNull("memberLogo"))
                val score = member.stringOrNull("memberBaseScore")
                if (id.isBlank() && name.isNullOrBlank() && logoUrl.isNullOrBlank() && score.isNullOrBlank()) continue
                add(
                    Team(
                        id = id,
                        name = name ?: "待定",
                        logoUrl = logoUrl,
                        score = score,
                        winner = !winnerId.isNullOrBlank() && winnerId == id,
                        extraScore = member.stringOrNull("memberExtraScore"),
                        bigScore = member.stringOrNull("memberBigScore"),
                        type = member.stringOrNull("memberType"),
                        description = member.stringOrNull("memberDesc"),
                    ),
                )
            }
        }
        val scoreKey = json.optJSONObject("scoreItemKey")
        val featured = json.optJSONObject("scoreItemInfo")?.let {
            FeaturedPlayer(
                name = it.stringOrNull("name") ?: return@let null,
                logoUrl = it.stringOrNull("logo"),
                score = it.stringOrNull("scoreNum"),
                hotComment = it.stringOrNull("hotComment"),
                scoreCountText = it.stringOrNull("scoreCountText"),
                outBizType = it.stringOrNull("scoreOutBizType"),
                outBizNo = it.stringOrNull("scoreOutBizNo"),
                jumpLink = it.stringOrNull("jumpLink"),
                teamLogoUrl = HupuUrls.imageUrl(it.stringOrNull("teamLogo")),
            )
        }
        return MatchSummary(
            id = matchId.ifBlank { uniqueKey },
            esport = esport,
            name = json.stringOrNull("matchName") ?: "比赛",
            introduction = json.stringOrNull("matchIntroduction").orEmpty(),
            status = json.stringOrNull("matchStatusDesc") ?: "待定",
            startTimeMillis = startMillis,
            startTimeLabel = formatTime(startMillis),
            teams = teams,
            scoreCountText = json.stringOrNull("scoreCountText"),
            outBizType = scoreKey?.stringOrNull("outBizType"),
            outBizNo = scoreKey?.stringOrNull("outBizNo"),
            featuredPlayer = featured,
            matchType = json.stringOrNull("matchType"),
            statusCode = json.stringOrNull("matchStatus"),
            liveRoomLink = json.stringOrNull("liveRoomLink"),
            uniqueKey = uniqueKey,
        )
    }

    private fun MatchSummary.isSchedulePlaceholder(): Boolean {
        val description = "$introduction $name"
        val explicitlyOffSeason = description.contains("休赛期") || description.contains("休赛日")
        val emptyDiscussionRoom = teams.isEmpty() &&
            !matchType.equals("against", ignoreCase = true) &&
            introduction.trimStart().startsWith("讨论室")
        return explicitlyOffSeason || emptyDiscussionRoom
    }

    private fun collectTargets(
        nodes: JSONArray?,
        destination: MutableList<RatingTarget>,
        seen: MutableSet<String>,
        stageName: String,
    ) {
        if (nodes == null) return
        for (index in 0 until nodes.length()) {
            val wrapper = nodes.optJSONObject(index) ?: continue
            wrapper.optJSONObject("node")?.toRatingTarget(wrapper.optLongOrNull("nodeId"))?.let { target ->
                if (target.isRenderable && (target.canScore || target.scoreCount > 0) && seen.add(target.key)) {
                    destination += target.copy(stageName = stageName)
                }
            }
            collectTargets(wrapper.optJSONArray("subNodes"), destination, seen, stageName)
        }
    }

    private fun parseTargetWrappers(nodes: JSONArray?, stageName: String): List<RatingTarget> {
        val targets = mutableListOf<RatingTarget>()
        val seen = mutableSetOf<String>()
        collectTargets(nodes, targets, seen, stageName)
        return targets
    }

    private fun JSONObject.toRatingTarget(wrapperNodeId: Long? = null): RatingTarget? {
        val bizType = stringOrNull("bizType") ?: return null
        val bizId = stringOrNull("bizId") ?: return null
        val info = optJSONObject("infoJson")
        val labels = info?.optJSONArray("label").objectStringValues("text")
        val description = info?.firstArrayString("desc") ?: labels.firstOrNull()
        val scoreCount = optInt("scorePersonCount", optInt("summedScorePersonCount", 0))
        val directCommentCount = optInt("commentCount", 0)
        val commentCount = maxOf(directCommentCount, optInt("summedCommentCount", 0))
        val distribution = optJSONObject("scoreDistribution")
        val scoreDistribution = (2..10 step 2).associateWith { score ->
            distribution?.optInt(score.toString(), 0) ?: 0
        }
        return RatingTarget(
            nodeId = wrapperNodeId ?: optLongOrNull("nodeId") ?: optJSONArray("scoreItemNodeId")?.optLongOrNull(0),
            outBizType = bizType,
            outBizNo = bizId,
            name = stringOrNull("name") ?: "评分对象",
            description = description,
            stageName = null,
            labels = labels,
            imageUrl = optJSONArray("image")?.stringOrNull(0),
            scoreAverage = optDouble("scoreAvg", 0.0),
            scoreCount = scoreCount,
            commentCount = commentCount,
            userScore = optInt("userScore", 0),
            canScore = optBoolean("canScore", false),
            canComment = optBoolean("canComment", false),
            hotComment = optJSONArray("hottestComments")?.stringOrNull(0),
            scoreDistribution = scoreDistribution,
            category = info?.firstArrayString("type"),
            imageUrls = optJSONArray("image").stringValues(),
            directCommentCount = directCommentCount,
            showScore = optBoolean("showScore", true),
            visible = optBoolean("visible", true),
            deleted = optInt("del", 0) != 0,
            finalStatus = stringOrNull("finalStatus"),
            infoAttributes = info?.arrayAttributes().orEmpty(),
            hotCommentPreviews = parseComments(optJSONArray("hotCommentModels")),
            championImageUrl = info?.firstArrayString("auxiliaryPic")
                ?.takeIf { bizType == "lol_item" },
        )
    }

    private fun parseComments(comments: JSONArray?): List<HupuComment> = buildList {
        if (comments == null) return@buildList
        for (index in 0 until comments.length()) {
            val comment = comments.optJSONObject(index) ?: continue
            val media = comment.optJSONArray("commentContentImages").commentMedia()
            add(
                HupuComment(
                    id = comment.stringOrNull("commentId").orEmpty(),
                    subjectId = comment.stringOrNull("subjectId").orEmpty(),
                    author = comment.stringOrNull("commentUserName") ?: "虎扑用户",
                    avatarUrl = comment.stringOrNull("commentUserHeadImg"),
                    content = cleanComment(comment.stringOrNull("commentContent").orEmpty()),
                    date = comment.stringOrNull("commentDate").orEmpty(),
                    location = comment.stringOrNull("ipLocation"),
                    score = comment.optInt("score", 0),
                    lightCount = comment.optInt("lightCount", 0),
                    imageUrls = media.filter { it.type.isNullOrBlank() || it.type == "IMAGE" }.map { it.url },
                    replyCount = comment.optInt("descendantCount", comment.optInt("subCommentCount", 0)),
                    previewReplies = parseComments(comment.optJSONArray("subCommentList")).take(1),
                    parentCommentId = comment.stringOrNull("parentCommentId")?.takeUnless { it == "0" },
                    parentAuthor = comment.stringOrNull("parentCommentUserName"),
                    authorId = comment.stringOrNull("commentUserId"),
                    badge = comment.optJSONObject("commentUserTakeBadge")?.toCommentBadge(),
                    certificationIconUrl = comment.stringOrNull("commentUserCertIcon"),
                    ornamentIconUrl = comment.stringOrNull("commentUserOrnamentIcon"),
                    userLabel = comment.stringOrNull("userLabel"),
                    hasLight = comment.optBoolean("hasLight", false),
                    hasBlack = comment.optBoolean("hasBlack", false),
                    blackCount = comment.optInt("blackCount", 0),
                    lightScore = comment.optDouble("lightScore", 0.0),
                    publishTime = comment.optLongOrNull("publishTime"),
                    media = media,
                    ancillaryMedia = comment.optJSONArray("commentAncillaryContents").commentMedia(),
                    parentAuthorId = comment.stringOrNull("parentCommentUserId"),
                    parentAvatarUrl = comment.stringOrNull("parentCommentUserHeadImg"),
                    parentContent = comment.stringOrNull("parentCommentContent")?.let(::cleanComment),
                    parentCanSee = comment.optBooleanOrNull("parentCommentCanSee"),
                    parentDeleted = comment.optBoolean("parentCommentDeleteFlag", false),
                    parentMedia = comment.optJSONArray("parentCommentContentImages").commentMedia(),
                    parentAncillaryMedia = comment.optJSONArray("parentCommentAncillaryContents").commentMedia(),
                    parentBadge = comment.optJSONObject("parentCommentUserBadge")?.toCommentBadge(),
                    parentCertificationIconUrl = comment.stringOrNull("parentCommentUserCertIcon"),
                    parentOrnamentIconUrl = comment.stringOrNull("parentCommentUserOrnamentIcon"),
                    commentKey = comment.stringOrNull("commentKey"),
                    contentDigest = comment.stringOrNull("contentDigest"),
                    commentType = comment.stringOrNull("commentType"),
                    aiType = comment.stringOrNull("aiType"),
                    occasionText = comment.stringOrNull("commentOccasionText"),
                    prompt = comment.stringOrNull("commentPrompt"),
                    chosenTags = comment.optJSONArray("chosenTags").jsonValuesAsStrings(),
                    highlightTexts = comment.optJSONArray("highlightTextList").jsonValuesAsStrings(),
                ),
            )
        }
    }

    private val RatingTarget.key: String get() = "$outBizType:$outBizNo"

    private val RatingTarget.isRenderable: Boolean
        get() = visible && !deleted && showScore && (finalStatus.isNullOrBlank() || finalStatus == "PASS")

    private fun JSONObject.requiredObject(name: String): JSONObject =
        optJSONObject(name) ?: error("响应缺少 $name")

    private fun JSONObject.stringOrNull(name: String): String? =
        if (isNull(name)) null else optString(name).takeUnless { it.isBlank() || it == "null" }

    private fun JSONArray.stringOrNull(index: Int): String? =
        if (index >= length() || isNull(index)) null else optString(index).takeUnless { it.isBlank() || it == "null" }

    private fun JSONArray?.objectStringValues(name: String): List<String> = buildList {
        val values = this@objectStringValues ?: return@buildList
        for (index in 0 until values.length()) {
            values.optJSONObject(index)?.stringOrNull(name)?.let(::add)
        }
    }

    private fun JSONArray?.stringValues(): List<String> = buildList {
        val values = this@stringValues ?: return@buildList
        for (index in 0 until values.length()) values.stringOrNull(index)?.let(::add)
    }

    private fun JSONArray?.commentMedia(): List<CommentMedia> = buildList {
        val values = this@commentMedia ?: return@buildList
        for (index in 0 until values.length()) {
            val item = values.optJSONObject(index) ?: continue
            val url = item.stringOrNull("commentContent")
                ?: item.stringOrNull("content")
                ?: item.stringOrNull("url")
                ?: continue
            add(
                CommentMedia(
                    id = item.stringOrNull("commentContentId") ?: item.stringOrNull("id"),
                    url = url,
                    type = item.stringOrNull("commentContentType") ?: item.stringOrNull("type"),
                    durationSeconds = item.optIntOrNull("durationInSec") ?: item.optIntOrNull("duration"),
                    transcodeComplete = item.optBooleanOrNull("transcodeComplete"),
                    audioText = item.stringOrNull("audioConvertToText"),
                    audioConversionAvailable = item.optBooleanOrNull("audioConvertFeature"),
                ),
            )
        }
    }

    private fun JSONArray?.jsonValuesAsStrings(): List<String> = buildList {
        val values = this@jsonValuesAsStrings ?: return@buildList
        for (index in 0 until values.length()) {
            if (values.isNull(index)) continue
            values.opt(index)?.toString()?.takeUnless { it == "null" }?.let(::add)
        }
    }

    private fun JSONObject.toCommentBadge(): CommentBadge? {
        val badge = CommentBadge(
            id = stringOrNull("badgeId"),
            name = stringOrNull("name"),
            iconUrl = stringOrNull("badgeIcon"),
            backgroundImageUrl = stringOrNull("badgeBgImg"),
        )
        return badge.takeIf {
            !it.id.isNullOrBlank() || !it.name.isNullOrBlank() ||
                !it.iconUrl.isNullOrBlank() || !it.backgroundImageUrl.isNullOrBlank()
        }
    }

    private fun JSONObject.arrayAttributes(): Map<String, List<String>> = buildMap {
        val keys = keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val values = optJSONArray(key) ?: continue
            put(
                key,
                buildList {
                    for (index in 0 until values.length()) {
                        val value = values.opt(index) ?: continue
                        add(if (value is JSONObject) value.toString() else value.toString())
                    }
                },
            )
        }
    }

    private fun JSONObject.optLongOrNull(name: String): Long? =
        if (isNull(name) || !has(name)) null else runCatching { getLong(name) }.getOrNull()

    private fun JSONObject.optIntOrNull(name: String): Int? =
        if (isNull(name) || !has(name)) null else runCatching { getInt(name) }.getOrNull()

    private fun JSONObject.optBooleanOrNull(name: String): Boolean? =
        if (isNull(name) || !has(name)) null else runCatching { getBoolean(name) }.getOrNull()

    private fun JSONArray.optLongOrNull(index: Int): Long? =
        if (index >= length() || isNull(index)) null else runCatching { getLong(index) }.getOrNull()

    private fun JSONObject.firstArrayString(name: String): String? = optJSONArray(name)?.stringOrNull(0)

    private fun JSONObject.attributeValue(key: String): String? {
        val groupAttributes = optJSONArray("groupAttributes")
        if (groupAttributes != null) {
            for (index in 0 until groupAttributes.length()) {
                val attribute = groupAttributes.optJSONObject(index) ?: continue
                if (attribute.stringOrNull("attributeKey") == key) {
                    return attribute.stringOrNull("attributeValue")
                }
            }
        }
        val attributes = optJSONArray("attributes") ?: return null
        for (index in 0 until attributes.length()) {
            val attribute = attributes.optJSONObject(index) ?: continue
            if (attribute.stringOrNull("key") == key) return attribute.optJSONArray("values")?.stringOrNull(0)
        }
        return null
    }

    private fun formatTime(timeMillis: Long): String = if (timeMillis <= 0L) {
        "待定"
    } else {
        SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(timeMillis))
    }

    private fun cleanComment(content: String): String = content
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("<[^>]+>"), "")
        .trim()
}

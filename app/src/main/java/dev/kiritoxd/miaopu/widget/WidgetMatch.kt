package dev.kiritoxd.miaopu.widget

import dev.kiritoxd.miaopu.data.EsportCatalog
import dev.kiritoxd.miaopu.data.MatchSummary
import dev.kiritoxd.miaopu.data.Team
import dev.kiritoxd.miaopu.data.isLive
import dev.kiritoxd.miaopu.data.isTerminal
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
internal data class WidgetTeam(val name: String, val logoUrl: String?, val score: String?)

/** Only public schedule data is persisted; no account or comment data. */
@Serializable
internal data class WidgetMatch(
    val id: String,
    val businessId: String,
    val name: String,
    val competition: String,
    val status: String,
    val statusCode: String?,
    val startsAt: Long,
    val timeLabel: String,
    val teams: List<WidgetTeam>,
    val outBizType: String?,
    val outBizNo: String?,
    val uniqueKey: String,
) {
    val identity: String get() = uniqueKey.takeIf { it.isNotBlank() && it != id } ?: "$businessId:$id"
    val sportLabel: String get() = EsportCatalog.byBusinessId(businessId)?.shortTitle.orEmpty()
    val live: Boolean get() = toModel().isLive
    val terminal: Boolean get() = toModel().isTerminal
    val hasRatings: Boolean get() = !outBizType.isNullOrBlank() && !outBizNo.isNullOrBlank()

    fun toModel() = MatchSummary(
        id = id,
        esport = requireNotNull(EsportCatalog.byBusinessId(businessId)),
        name = name,
        introduction = competition,
        status = status,
        statusCode = statusCode,
        startTimeMillis = startsAt,
        startTimeLabel = timeLabel,
        teams = teams.map { Team("", it.name, it.logoUrl, it.score, false) },
        outBizType = outBizType,
        outBizNo = outBizNo,
        uniqueKey = uniqueKey,
        scoreCountText = null,
        featuredPlayer = null,
    )
}

internal fun MatchSummary.toWidgetMatch() = WidgetMatch(
    id, esport.businessId, name, introduction, status, statusCode, startTimeMillis,
    startTimeLabel, teams.take(2).map { WidgetTeam(it.name, it.logoUrl, it.score) },
    outBizType, outBizNo, uniqueKey,
)

/** Live first, then upcoming, then recent results. Old seasons never fill an empty card. */
internal fun selectWidgetMatches(matches: List<WidgetMatch>, now: Long, limit: Int = 3): List<WidgetMatch> {
    val hour = 60 * 60 * 1000L
    val candidates = matches.filter {
        EsportCatalog.byBusinessId(it.businessId) != null &&
            it.startsAt in (now - 48 * hour)..(now + 7 * 24 * hour) &&
            (!it.live || it.startsAt >= now - 12 * hour)
    }.distinctBy(WidgetMatch::identity)
    val live = candidates.filter(WidgetMatch::live).sortedBy(WidgetMatch::startsAt)
    val next = candidates.filter { !it.live && !it.terminal && it.startsAt >= now }
        .sortedBy(WidgetMatch::startsAt)
    val recent = candidates.filter { it.terminal && it.startsAt <= now }.sortedByDescending(WidgetMatch::startsAt)
    return (live + next + recent).take(limit.coerceAtLeast(0))
}

internal fun WidgetMatch.scoreLabel(): String = when {
    !live && !terminal -> "VS"
    teams.size == 2 -> "${teams[0].score?.takeIf(String::isNotBlank) ?: "—"} : ${teams[1].score?.takeIf(String::isNotBlank) ?: "—"}"
    else -> status.ifBlank { "暂无比分" }
}

internal fun WidgetMatch.dateTimeLabel(now: Long): String {
    if (startsAt <= 0L) return timeLabel.ifBlank { "时间待定" }
    val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
    val pattern = if (dayFormat.format(Date(startsAt)) == dayFormat.format(Date(now))) "HH:mm" else "MM-dd HH:mm"
    return SimpleDateFormat(pattern, Locale.ROOT).format(Date(startsAt))
}

/** Calendar days in the device timezone, including DST and month/year boundaries. */
internal fun WidgetMatch.startDayOffset(now: Long): Int {
    fun dayIndex(timestamp: Long): Long {
        val local = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
        return java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(java.util.Calendar.YEAR), local.get(java.util.Calendar.MONTH), local.get(java.util.Calendar.DAY_OF_MONTH))
        }.timeInMillis / 86_400_000L
    }
    return if (startsAt > 0) (dayIndex(startsAt) - dayIndex(now)).toInt() else 0
}

internal fun WidgetMatch.clockLabel(): String = if (startsAt > 0) {
    SimpleDateFormat("HH:mm", Locale.ROOT).format(Date(startsAt))
} else timeLabel.ifBlank { "待定" }

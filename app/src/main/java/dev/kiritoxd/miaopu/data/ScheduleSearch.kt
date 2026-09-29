package dev.kiritoxd.miaopu.data

import java.util.Locale

fun Schedule.searchSchedule(query: String, checkActive: () -> Unit = {}): Schedule {
    val terms = query
        .trim()
        .lowercase(Locale.ROOT)
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)
    if (terms.isEmpty()) return this

    val filteredDays = days.mapNotNull { day ->
        checkActive()
        val dayText = "${day.date} ${day.label}".lowercase(Locale.ROOT)
        day.copy(
            matches = day.matches.filter { match ->
                checkActive()
                match.includesSearchTerms(terms, dayText)
            },
        ).takeIf { it.matches.isNotEmpty() }
    }
    val retainedMatchIds = filteredDays.flatMap(ScheduleDay::matches).mapTo(hashSetOf(), MatchSummary::id)
    return copy(
        anchorMatchId = anchorMatchId?.takeIf { it in retainedMatchIds },
        days = filteredDays,
    )
}

private fun MatchSummary.includesSearchTerms(terms: List<String>, dayText: String): Boolean {
    val searchableText = buildString {
        append(dayText)
        append(' ')
        append(esport.title)
        append(' ')
        append(esport.shortTitle)
        append(' ')
        append(name)
        append(' ')
        append(introduction)
        append(' ')
        append(status)
        teams.forEach { team ->
            append(' ')
            append(team.name)
            team.description?.let { description ->
                append(' ')
                append(description)
            }
        }
        featuredPlayer?.let { player ->
            append(' ')
            append(player.name)
        }
    }.lowercase(Locale.ROOT)
    return terms.all(searchableText::contains)
}

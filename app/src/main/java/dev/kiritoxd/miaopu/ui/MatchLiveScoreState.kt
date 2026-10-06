package dev.kiritoxd.miaopu.ui

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.awaitCancellation
import androidx.lifecycle.repeatOnLifecycle
import dev.kiritoxd.miaopu.data.Esport
import dev.kiritoxd.miaopu.data.MatchSummary
import dev.kiritoxd.miaopu.data.matchdetail.MatchDetailAdapter
import dev.kiritoxd.miaopu.data.matchdetail.MatchLiveScore
import dev.kiritoxd.miaopu.data.matchdetail.pollMatchLiveScore

@Composable
internal fun rememberMatchLiveScore(match: MatchSummary): MatchLiveScore? {
    var score by remember(match.uniqueKey) { mutableStateOf<MatchLiveScore?>(null) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(match.uniqueKey, lifecycle) {
        if (match.esport != Esport.CS2 && match.esport != Esport.VALORANT) return@LaunchedEffect
        val adapter = MatchDetailAdapter()
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            try {
                pollMatchLiveScore(load = { adapter.liveScore(match.id, it) }, publish = { score = it })
                awaitCancellation()
            } finally {
                score = null
            }
        }
    }
    return score
}

internal fun MatchSummary.withLiveScore(live: MatchLiveScore?): MatchSummary {
    if (live == null || live.matchId != id) return this
    return copy(
        statusCode = live.status,
        status = live.statusText ?: status,
        teams = teams.map { team ->
            val score = when (team.id) {
                live.homeTeamId -> live.homeScore
                live.awayTeamId -> live.awayScore
                else -> null
            }
            if (score == null) team else team.copy(score = score, bigScore = score)
        },
    )
}

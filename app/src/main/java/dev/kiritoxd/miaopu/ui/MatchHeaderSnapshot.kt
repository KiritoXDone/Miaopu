package dev.kiritoxd.miaopu.ui

import dev.kiritoxd.miaopu.data.matchdetail.MatchAllScores
import dev.kiritoxd.miaopu.data.matchdetail.MatchStats

internal data class MatchHeaderSnapshot(val summary: MatchAllScores?, val hasStatistics: Boolean)

/** Resolve optional sections together before the reader can scroll through the detail body. */
internal fun matchHeaderSnapshot(
    summary: LoadState<MatchAllScores>,
    statistics: LoadState<MatchStats>?,
    hasStatistics: Boolean,
): MatchHeaderSnapshot? {
    if (summary is LoadState.Loading || statistics == null || statistics is LoadState.Loading) return null
    return MatchHeaderSnapshot((summary as? LoadState.Ready)?.value?.takeIf { it.hasScores }, hasStatistics)
}

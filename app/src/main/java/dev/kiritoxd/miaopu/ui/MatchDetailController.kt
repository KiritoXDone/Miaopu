package dev.kiritoxd.miaopu.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.kiritoxd.miaopu.data.Esport
import dev.kiritoxd.miaopu.data.AdapterResult
import dev.kiritoxd.miaopu.data.RatingTarget
import dev.kiritoxd.miaopu.data.matchdetail.MatchDetailSource
import dev.kiritoxd.miaopu.data.MatchSummary
import dev.kiritoxd.miaopu.data.RatingStage
import dev.kiritoxd.miaopu.data.StageRatingDetail
import dev.kiritoxd.miaopu.data.matchdetail.MatchAllScores
import dev.kiritoxd.miaopu.data.matchdetail.MatchDetailAdapter
import dev.kiritoxd.miaopu.data.matchdetail.MatchStats
import dev.kiritoxd.miaopu.data.matchdetail.StatsMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Independent requests keep stats available even when a rating tree is missing. */
internal class MatchDetailController(
    private val scope: CoroutineScope,
    private val stageLoader: suspend (MatchSummary, RatingStage) -> AdapterResult<StageRatingDetail>,
    private val adapter: MatchDetailSource = MatchDetailAdapter(),
) {
    private var match: MatchSummary? = null
    private var summaryJob: Job? = null
    private var statsJob: Job? = null
    private var stageJob: Job? = null
    private var generation = 0
    private var summaryGeneration = 0
    private var statsGeneration = 0
    private var stageGeneration = 0
    var summary: LoadState<MatchAllScores> by mutableStateOf(LoadState.Loading)
        private set
    var hasStatistics by mutableStateOf(false)
        private set
    var statistics: LoadState<MatchStats>? by mutableStateOf(null)
        private set
    var stage: LoadState<StageRatingDetail> by mutableStateOf(LoadState.Loading)
        private set
    var maps: List<StatsMap> by mutableStateOf(emptyList())
        private set
    var selectedMapId: String? by mutableStateOf(null)
        private set
    private var stageKey: RatingStage? = null
    private val stageCache = MatchDataCache<RatingStage, StageRatingDetail>()
    private val statsCache = MatchDataCache<String, MatchStats>()

    private fun clearCaches() {
        stageCache.clear()
        statsCache.clear()
    }

    fun bind(next: MatchSummary) {
        if (match?.uniqueKey == next.uniqueKey) return
        clearCaches()
        generation++
        summaryJob?.cancel()
        statsJob?.cancel()
        stageJob?.cancel()
        match = next
        summary = LoadState.Loading
        statistics = null
        hasStatistics = false
        maps = emptyList()
        selectedMapId = null
        stage = LoadState.Loading
        stageKey = null
        loadSummary()
    }

    fun close() {
        clearCaches()
        generation++
        summaryJob?.cancel()
        statsJob?.cancel()
        stageJob?.cancel()
        match = null
    }

    fun refresh() {
        clearCaches()
        loadSummary()
        if (statistics != null) loadStats()
        stageKey?.let { loadStage(it, retry = true) }
    }

    fun updateTargets(transform: (RatingTarget) -> RatingTarget) {
        stageCache.clear()
        val value = (stage as? LoadState.Ready)?.value ?: return
        stage = LoadState.Ready(value.copy(targets = value.targets.map(transform),
            groups = value.groups.map { it.copy(targets = it.targets.map(transform)) }))
    }

    fun latestTarget(target: RatingTarget): RatingTarget? {
        val value = (stage as? LoadState.Ready)?.value ?: return null
        return (value.targets.asSequence() + value.groups.asSequence().flatMap { it.targets.asSequence() })
            .firstOrNull { it.outBizNo == target.outBizNo && it.outBizType == target.outBizType }
    }

    fun loadSummary() {
        val current = match ?: return
        val token = ++summaryGeneration
        val matchToken = generation
        summaryJob?.cancel()
        summary = LoadState.Loading
        summaryJob = scope.launch {
            val result = when (current.esport) {
                Esport.LOL, Esport.KOG -> adapter.playerScores(current.id, current.esport)
                else -> adapter.allScores(current.id, "common_match")
            }
            if (token == summaryGeneration && matchToken == generation) summary = result.detailState()
        }
    }

    fun loadStage(next: RatingStage, retry: Boolean = false) {
        val current = match ?: return
        if (!retry && stageKey == next && stageJob?.isActive == true) return
        stageKey = next
        stageJob?.cancel()
        val token = ++stageGeneration
        val matchToken = generation
        if (!retry) stageCache[next]?.let {
            stage = LoadState.Ready(it)
            return
        }
        stage = LoadState.Loading
        stageJob = scope.launch {
            val result = stageLoader(current, next)
            if (token == stageGeneration && matchToken == generation) {
                result.data?.let { stageCache.put(next, it) }
                stage = result.detailState()
            }
        }
    }

    fun ensureStats() {
        if (statistics == null) loadStats()
    }

    fun loadStats(mapId: String? = selectedMapId) {
        val current = match ?: return
        statsJob?.cancel()
        val token = ++statsGeneration
        val matchToken = generation
        val requestedMapId = mapId ?: "0"
        statsCache[requestedMapId]?.let {
            selectedMapId = requestedMapId
            applyStats(it)
            statistics = LoadState.Ready(it)
            return
        }
        statistics = LoadState.Loading
        selectedMapId = mapId
        statsJob = scope.launch {
            var requestedId = mapId ?: "0"
            var result = adapter.stats(current.id, requestedId)
            if (token != statsGeneration || matchToken != generation) return@launch
            val discovered = result.data
            if (discovered != null && discovered.maps.isNotEmpty() && discovered.maps.none { it.id == requestedId }) {
                // CS2 may have no overall tab. Fetch a real map, never show the empty bo=0 table.
                requestedId = discovered.defaultMapId ?: discovered.maps.first().id
                result = adapter.stats(current.id, requestedId)
            }
            if (token != statsGeneration || matchToken != generation) return@launch
            maps = result.data?.maps ?: discovered?.maps ?: maps
            selectedMapId = requestedId
            result.data?.let { data ->
                statsCache.put(requestedId, data)
                applyStats(data)
            }
            statistics = result.detailState()
        }
    }
    private fun applyStats(data: MatchStats) {
        maps = data.maps
        hasStatistics = data.teams.any { team ->
            team.players.any { row -> row.drop(1).any { it.text.isNotBlank() && it.text != "—" } }
        }
    }

}

private fun <T> AdapterResult<T>.detailState(): LoadState<T> = data?.let { LoadState.Ready(it) }
    ?: LoadState.Failed(error?.message ?: "暂时无法读取比赛数据", error?.retryable ?: true)

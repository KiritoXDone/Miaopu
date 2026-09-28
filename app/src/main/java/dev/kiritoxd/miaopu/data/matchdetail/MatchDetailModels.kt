package dev.kiritoxd.miaopu.data.matchdetail

/** Public match statistics preserve the provider's labels and display values. */
data class MatchStats(val maps: List<StatsMap>, val defaultMapId: String?, val teams: List<StatsTeam>)
data class StatsMap(val id: String, val name: String)
data class StatsCell(val text: String, val imageUrl: String? = null)
data class StatsTeam(
    val name: String,
    val logoUrl: String?,
    val score: String?,
    val columns: List<String>,
    val players: List<List<StatsCell>>,
)
data class MatchAllScores(val teams: List<ScoredTeam>)
data class ScoredTeam(val name: String, val logoUrl: String?, val players: List<PlayerAllScore>)
data class PlayerAllScore(val name: String, val score: String, val dayColor: String?, val nightColor: String?)

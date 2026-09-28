package dev.kiritoxd.miaopu.data.matchdetail

import org.json.JSONArray
import org.json.JSONObject

internal object MatchDetailParser {
    fun allScores(body: String): MatchAllScores {
        val result = result(body)
        val basics = result.optJSONArray("memberBasicInfos") ?: JSONArray()
        val scores = result.optJSONArray("memberScoreInfos") ?: JSONArray()
        return MatchAllScores((0 until maxOf(basics.length(), scores.length())).map { index ->
            val basic = basics.optJSONObject(index)
            val players = mutableListOf<PlayerAllScore>()
            // Groups can contain substitutes; don't truncate or zip unequal team rosters.
            fun collect(array: JSONArray) {
                for (i in 0 until array.length()) {
                    when (val entry = array.opt(i)) {
                        is JSONArray -> collect(entry)
                        is JSONObject -> entry.text("memberName")?.let { name ->
                            players += PlayerAllScore(name, entry.text("memberAllAvgScore") ?: "—",
                                entry.text("scoreDayColor"), entry.text("scoreNightColor"))
                        }
                    }
                }
            }
            scores.optJSONArray(index)?.let(::collect)
            ScoredTeam(basic?.text("memberName").orEmpty(), basic?.text("memberLogo"), players)
        })
    }

    fun playerScores(body: String): MatchAllScores {
        val root = JSONObject(body)
        check(root.optInt("code") == 1) { "暂时无法读取全场评分" }
        val data = root.optJSONObject("data") ?: return MatchAllScores(emptyList())
        val info = data.optJSONObject("matchInfo")
        val teams = data.optJSONArray("teamScoreInfo")?.objects().orEmpty()
        return MatchAllScores(teams.sortedBy { if (it.optBoolean("home")) 0 else 1 }.mapIndexed { index, team ->
            val side = if (team.has("home")) { if (team.optBoolean("home")) 1 else 2 } else index + 1
            val players = team.optJSONArray("playerInfo")?.objects().orEmpty().mapNotNull { player ->
                val name = player.text("playerName") ?: return@mapNotNull null
                val score = player.text("playerScore")?.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 && it <= 10 }
                PlayerAllScore(name, score?.let { String.format(java.util.Locale.ROOT, "%.1f", it) } ?: "—", null, null)
            }
            ScoredTeam(info?.text("team${side}_name").orEmpty(), info?.text("team${side}_logo"), players)
        })
    }

    fun stats(body: String): MatchStats {
        val blocks = result(body).optJSONArray("stats")?.objects()
            ?: error("比赛数据格式已变化")
        val mapBlock = blocks.firstOrNull { it.text("componentCode") == "bo_components" }
        val maps = mapBlock?.optJSONObject("headingInfo")?.optJSONArray("tableDataInfo")?.objects()
            .orEmpty().mapNotNull { cell ->
                val id = cell.text("requestValue") ?: return@mapNotNull null
                StatsMap(id, if (id == "0") "全场" else cell.text("showName") ?: id)
            }.distinctBy { it.id }
        val defaultMap = mapBlock?.takeUnless { it.isNull("defaultAnchor") }
            ?.optInt("defaultAnchor", -1)?.let { maps.getOrNull(it)?.id }
        val scores = blocks.filter { it.text("componentCode") == "single_horizontal_display" }
            .flatMap { it.optJSONArray("bodyInfo")?.objects().orEmpty() }
            .associate { row -> row.text("memberPosition") to
                row.optJSONArray("bodyDataInfo")?.optJSONObject(1)?.text("showName") }
        val teams = blocks.filter { it.text("componentCode") == "list_display_components" }.mapNotNull { block ->
            val heading = block.optJSONObject("headingInfo") ?: return@mapNotNull null
            val columns = heading.optJSONArray("tableDataInfo")?.objects().orEmpty()
            if (columns.isEmpty()) return@mapNotNull null
            val players = block.optJSONArray("bodyInfo")?.objects().orEmpty().map { row ->
                val cells = row.optJSONArray("bodyDataInfo")
                // A missing value is unknown, never zero. Keep column alignment intact.
                columns.indices.map { index ->
                    val cell = cells?.optJSONObject(index)
                    StatsCell(cell?.text("showName") ?: "—", cell?.text("logo"))
                }
            }
            StatsTeam(columns.first().text("showName").orEmpty(), columns.first().text("logo"),
                scores[block.text("belongingCamp") ?: heading.text("memberPosition")],
                columns.map { it.text("showName") ?: "—" }, players)
        }
        return MatchStats(maps, defaultMap, teams)
    }

    private fun result(body: String): JSONObject {
        val root = JSONObject(body)
        check(root.optBoolean("success") && root.optInt("status", 200) == 200) { "暂时无法读取比赛数据" }
        return root.optJSONObject("result") ?: error("比赛数据暂未提供")
    }

    private fun JSONObject.text(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)
    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull(::optJSONObject)
}

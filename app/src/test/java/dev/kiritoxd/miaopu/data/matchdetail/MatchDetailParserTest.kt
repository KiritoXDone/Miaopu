package dev.kiritoxd.miaopu.data.matchdetail

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class MatchDetailParserTest {
    @Test fun valorantPreservesAllColumnsWithoutInventingRating() {
        val data = MatchDetailParser.stats(fixture("val-overall"))
        assertEquals(listOf("0", "2", "1"), data.maps.map { it.id })
        assertEquals("全场", data.maps.first().name)
        assertEquals("0", data.defaultMapId)
        val home = data.teams.first()
        assertEquals(listOf("JDG", "K/D/A", "首杀/首死", "ACS", "ADR", "HS%", "多杀数"), home.columns)
        assertEquals(listOf("BerLIN", "25/30/8", "3/2", "198", "133", "35%", "4次"), home.players.first().map { it.text })
        assertEquals(5, home.players.size)
        assertEquals("2", data.teams[1].score)
        assertTrue(home.players.first().first().imageUrl!!.startsWith("https://"))
    }

    @Test fun csBo3PreservesEveryMapAndItsActualScore() {
        val expected = mapOf(1 to listOf("13", "7"), 2 to listOf("11", "13"), 3 to listOf("13", "8"))
        for ((id, scores) in expected) {
            val data = MatchDetailParser.stats(fixture("cs-bo3-$id"))
            assertEquals(listOf("3", "2", "1"), data.maps.map { it.id })
            assertEquals(listOf("核子危机", "炼狱小镇", "阿努比斯"), data.maps.map { it.name })
            assertEquals(scores, data.teams.map { it.score })
            data.teams.forEach { team ->
                assertEquals(listOf("K/D", "KAST", "ADR", "Rating"), team.columns.drop(1))
                assertEquals(5, team.players.size)
                assertTrue(team.players.all { it.last().text.toDoubleOrNull() != null })
            }
        }
    }

    @Test fun csKeepsActualRatingAndDoesNotInventAnOverallTab() {
        val data = MatchDetailParser.stats(fixture("cs-map"))
        assertEquals(listOf("2", "1"), data.maps.map { it.id })
        assertEquals(listOf("FUT", "K/D", "KAST", "ADR", "Rating"), data.teams.first().columns)
        assertTrue(data.teams.first().players.first().last().text.toDouble() >= 0)
    }

    @Test fun mapColumnsRetainReturnedOrderAndAgentCells() {
        val root = JSONObject(fixture("val-map"))
        val block = root.getJSONObject("result").getJSONArray("stats").getJSONObject(2)
        val names = block.getJSONObject("headingInfo").getJSONArray("tableDataInfo")
        val data = MatchDetailParser.stats(root.toString())
        assertEquals((0 until names.length()).map { names.getJSONObject(it).getString("showName") }, data.teams.first().columns)
        assertEquals(names.length(), data.teams.first().players.first().size)
    }

    @Test fun missingStatsStayUnknownAndDoNotShiftColumns() {
        val root = JSONObject(fixture("val-overall"))
        val row = root.getJSONObject("result").getJSONArray("stats").getJSONObject(2)
            .getJSONArray("bodyInfo").getJSONObject(0).getJSONArray("bodyDataInfo")
        row.put(1, JSONObject.NULL)
        row.getJSONObject(3).put("showName", "")
        val cells = MatchDetailParser.stats(root.toString()).teams.first().players.first()
        assertEquals("—", cells[1].text)
        assertEquals("3/2", cells[2].text)
        assertEquals("—", cells[3].text)
    }

    @Test fun summaryKeepsSubstitutesAndUnevenTeams() {
        val root = JSONObject(fixture("scores"))
        val groups = root.getJSONObject("result").getJSONArray("memberScoreInfos")
        groups.getJSONArray(0).getJSONArray(0).put(JSONObject().put("memberName", "substitute"))
        val data = MatchDetailParser.allScores(root.toString())
        assertEquals(6, data.teams[0].players.size)
        assertEquals(5, data.teams[1].players.size)
        assertEquals("—", data.teams[0].players.first { it.name == "substitute" }.score)
    }

    @Test fun summaryIsHiddenWithoutAnyReturnedScore() {
        assertFalse(MatchDetailParser.allScores("""{"success":true,"result":{}}""").hasScores)
        val root = JSONObject(fixture("scores"))
        val groups = root.getJSONObject("result").getJSONArray("memberScoreInfos")
        fun removeScores(array: JSONArray) {
            for (index in 0 until array.length()) when (val entry = array.opt(index)) {
                is JSONArray -> removeScores(entry)
                is JSONObject -> entry.remove("memberAllAvgScore")
            }
        }
        removeScores(groups)
        assertFalse(MatchDetailParser.allScores(root.toString()).hasScores)
        assertTrue(MatchDetailParser.allScores(fixture("scores")).hasScores)
    }

    @Test fun lolSummaryUsesHomeAwayTeamsAndActualPlayerScores() {
        val data = MatchDetailParser.playerScores(fixture("lol-scores"))
        assertTrue(data.hasScores)
        assertEquals(listOf("IG", "JDG"), data.teams.map { it.name })
        assertEquals(listOf(5, 5), data.teams.map { it.players.size })
        assertEquals("9.7", data.teams.first().players.single { it.name == "Rookie" }.score)
        val root = JSONObject(fixture("lol-scores"))
        val teams = root.getJSONObject("data").getJSONArray("teamScoreInfo")
        val first = teams.getJSONObject(0)
        teams.put(0, teams.getJSONObject(1))
        teams.put(1, first)
        assertEquals(listOf("IG", "JDG"), MatchDetailParser.playerScores(root.toString()).teams.map { it.name })
    }

    @Test fun kogSummaryRetainsSubstitutesAndMissingDataStaysHidden() {
        val data = MatchDetailParser.playerScores(fixture("kog-scores"))
        assertEquals(listOf("马来西亚", "中国"), data.teams.map { it.name })
        assertEquals(listOf(6, 6), data.teams.map { it.players.size })
        assertTrue(data.hasScores)
        assertFalse(MatchDetailParser.playerScores("""{"code":1,"data":null}""").hasScores)
        assertFalse(MatchDetailParser.playerScores("""{"code":1,"data":{"teamScoreInfo":[]}}""").hasScores)
        val invalid = MatchAllScores(listOf(ScoredTeam("unknown", null,
            listOf("—", "0", "NaN", "11", "-1").map { PlayerAllScore("player", it, null, null) })))
        assertFalse(invalid.hasScores)
    }

    @Test(expected = IllegalStateException::class)
    fun playerSummaryRejectsServiceFailure() {
        MatchDetailParser.playerScores("""{"code":0,"data":null}""")
    }

    @Test fun emptyStatsAreValid() {
        val data = MatchDetailParser.stats("""{"success":true,"result":{"stats":[]}}""")
        assertTrue(data.maps.isEmpty())
        assertTrue(data.teams.isEmpty())
    }

    @Test(expected = IllegalStateException::class)
    fun rejectsServiceErrors() {
        MatchDetailParser.stats("""{"success":false,"result":{"stats":[]}}""")
    }

    @Test(expected = IllegalStateException::class)
    fun rejectsChangedStatsEnvelope() {
        MatchDetailParser.stats("""{"success":true,"result":{}}""")
    }

    private fun fixture(name: String) = javaClass.getResource("/matchdetail/$name.json")!!.readText()
}

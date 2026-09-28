package dev.kiritoxd.miaopu.ui

import dev.kiritoxd.miaopu.data.*
import dev.kiritoxd.miaopu.data.matchdetail.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class MatchDetailControllerTest {
    @Test fun scheduleMatchFormatIsNotUsedAsRatingBusinessType() = runBlocking {
        val parsed = HupuJsonParser.schedule("""{"result":{"dayGameData":[{"matchData":[{
            "matchId":"cs-bo3","matchName":"Aurora vs Vitality","matchType":"against",
            "matchStatus":"COMPLETED","againstInfo":{"memberInfos":[
                {"memberName":"Aurora"},{"memberName":"Vitality"}]}
        }]}]}}""", Esport.CS2).days.single().matches.single()
        var requested: Pair<String, String>? = null
        val source = object : MatchDetailSource {
            override suspend fun allScores(matchId: String, businessType: String): AdapterResult<MatchAllScores> {
                requested = matchId to businessType
                return scoreResult(matchId)
            }
            override suspend fun playerScores(matchId: String, game: Esport) = scoreResult(matchId)
            override suspend fun stats(matchId: String, mapId: String) = stats(mapId)
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            controller(scope, source).bind(parsed)
            assertEquals("against", parsed.matchType)
            assertEquals("cs-bo3" to "common_match", requested)
        } finally { scope.cancel() }
    }

    @Test fun lolAndKogUseTheirPlayerScoreSource() = runBlocking {
        val calls = mutableListOf<Pair<String, Esport>>()
        val source = object : MatchDetailSource {
            override suspend fun allScores(matchId: String, businessType: String): AdapterResult<MatchAllScores> = error("Wrong source")
            override suspend fun playerScores(matchId: String, game: Esport): AdapterResult<MatchAllScores> {
                calls += matchId to game
                return scoreResult(game.title)
            }
            override suspend fun stats(matchId: String, mapId: String) = stats(mapId)
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val controller = controller(scope, source)
            controller.bind(match("lol-id").copy(esport = Esport.LOL, matchType = "against"))
            controller.bind(match("kog-id").copy(esport = Esport.KOG, matchType = "against"))
            assertEquals(listOf("lol-id" to Esport.LOL, "kog-id" to Esport.KOG), calls)
        } finally { scope.cancel() }
    }

    @Test fun refreshingSummaryDiscardsPreviousRequest() = runBlocking {
        val first = CompletableDeferred<AdapterResult<MatchAllScores>>()
        var calls = 0
        val source = source(scores = {
            if (++calls == 1) withContext(NonCancellable) { first.await() } else scoreResult("new")
        })
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val controller = controller(scope, source)
            controller.bind(match("a"))
            controller.refresh()
            first.complete(scoreResult("old"))
            assertEquals("new", (controller.summary as LoadState.Ready).value.teams.single().name)
        } finally { scope.cancel() }
    }

    @Test fun csDiscoveryFetchesAnAvailableMapWithoutShowingFalseOverall() = runBlocking {
        val requested = mutableListOf<String>()
        val source = source(stats = { _, map ->
            requested += map
            AdapterResult.success("fixture", MatchStats(listOf(StatsMap("2", "Dust2"), StatsMap("1", "Anubis")), null, emptyList()))
        })
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val controller = controller(scope, source)
            controller.bind(match("cs"))
            controller.ensureStats()
            assertEquals(listOf("0", "2"), requested)
            assertEquals("2", controller.selectedMapId)
            assertTrue(controller.statistics is LoadState.Ready)
        } finally { scope.cancel() }
    }

    @Test fun csBo3LoadsReturnedMapIdsInTheirOriginalOrder() = runBlocking {
        val requested = mutableListOf<String>()
        val source = source(stats = { _, id ->
            requested += id
            AdapterResult.success("fixture", MatchDetailParser.stats(
                javaClass.getResource("/matchdetail/cs-bo3-$id.json")!!.readText()))
        })
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val controller = controller(scope, source)
            controller.bind(match("cs-bo3"))
            controller.ensureStats()
            assertEquals(listOf("0", "3"), requested)
            assertEquals(listOf("3", "2", "1"), controller.maps.map { it.id })
            for (id in listOf("1", "2", "3")) {
                controller.loadStats(id)
                assertEquals(id, controller.selectedMapId)
                assertEquals(5, (controller.statistics as LoadState.Ready).value.teams.first().players.size)
            }
        } finally { scope.cancel() }
    }

    @Test fun switchingMatchesDiscardsLateSummaryResults() = runBlocking {
        val first = CompletableDeferred<AdapterResult<MatchAllScores>>()
        val second = CompletableDeferred<AdapterResult<MatchAllScores>>()
        val source = source(scores = { id -> withContext(NonCancellable) { if (id == "a") first.await() else second.await() } })
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val controller = controller(scope, source)
            controller.bind(match("a"))
            controller.bind(match("b"))
            second.complete(scoreResult("B"))
            first.complete(scoreResult("A"))
            assertEquals("B", (controller.summary as LoadState.Ready).value.teams.single().name)
        } finally { scope.cancel() }
    }

    @Test fun rapidMapChangesDoNotPublishAnOlderTable() = runBlocking {
        val first = CompletableDeferred<AdapterResult<MatchStats>>()
        val second = CompletableDeferred<AdapterResult<MatchStats>>()
        val source = source(stats = { _, id -> withContext(NonCancellable) { if (id == "1") first.await() else second.await() } })
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val controller = controller(scope, source)
            controller.bind(match("cs"))
            controller.loadStats("1")
            controller.loadStats("2")
            second.complete(stats("2"))
            first.complete(stats("1"))
            assertEquals("2", controller.selectedMapId)
            assertEquals("2", (controller.statistics as LoadState.Ready).value.defaultMapId)
        } finally { scope.cancel() }
    }

    @Test fun statisticsRemainIndependentOfRatingFailure() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val controller = controller(scope, source(scores = {
                AdapterResult.failure("fixture", AdapterStatus.NOT_CONFIGURED, "无评分", false)
            }, stats = { _, id -> stats(id) }))
            controller.bind(match("cs"))
            controller.ensureStats()
            assertTrue(controller.summary is LoadState.Failed)
            assertTrue(controller.statistics is LoadState.Ready)
        } finally { scope.cancel() }
    }

    @Test fun statisticsTabRequiresActualPlayerDataAndResetsForNextMatch() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val source = source(stats = { matchId, _ ->
            AdapterResult.success("fixture", if (matchId == "cs") MatchDetailParser.stats(
                javaClass.getResource("/matchdetail/cs-bo3-3.json")!!.readText()
            ) else MatchStats(emptyList(), null, listOf(
                StatsTeam("LPL", null, null, listOf("选手", "K/D"),
                    listOf(listOf(StatsCell("player", null), StatsCell("—", null)))))))
        })
        try {
            val controller = controller(scope, source)
            controller.bind(match("cs"))
            assertFalse(controller.hasStatistics)
            controller.ensureStats()
            assertTrue(controller.hasStatistics)
            controller.bind(match("lpl"))
            assertFalse(controller.hasStatistics)
            controller.ensureStats()
            assertFalse(controller.hasStatistics)
        } finally { scope.cancel() }
    }

    private fun controller(scope: CoroutineScope, source: MatchDetailSource) = MatchDetailController(
        scope, { _, _ -> AdapterResult.success("fixture", StageRatingDetail("", null, null, emptyList(), emptyList())) }, source,
    )
    private fun source(
        scores: suspend (String) -> AdapterResult<MatchAllScores> = { scoreResult(it) },
        stats: suspend (String, String) -> AdapterResult<MatchStats> = { _, id -> stats(id) },
    ) = object : MatchDetailSource {
        override suspend fun allScores(matchId: String, businessType: String) = scores(matchId)
        override suspend fun playerScores(matchId: String, game: Esport) = scores(matchId)
        override suspend fun stats(matchId: String, mapId: String) = stats.invoke(matchId, mapId)
    }
    private fun scoreResult(name: String) = AdapterResult.success("fixture", MatchAllScores(listOf(ScoredTeam(name, null, emptyList()))))
    private fun stats(id: String) = AdapterResult.success("fixture", MatchStats(listOf(StatsMap(id, id)), id, emptyList()))
    private fun match(id: String) = MatchSummary(id, Esport.CS2, "比赛", "", "已结束", 0, "", emptyList(), null, null, null, null)
}

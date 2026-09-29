package dev.kiritoxd.miaopu.ui

import dev.kiritoxd.miaopu.data.CommentPage
import dev.kiritoxd.miaopu.data.HupuComment
import dev.kiritoxd.miaopu.data.matchdetail.MatchAllScores
import dev.kiritoxd.miaopu.data.matchdetail.MatchStats
import org.junit.Assert.*
import org.junit.Test

class StablePresentationTest {
    @Test fun headerWaitsForBothOptionalRequests() {
        val summary = LoadState.Ready(MatchAllScores(emptyList()))
        val stats = LoadState.Ready(MatchStats(emptyList(), null, emptyList()))
        assertNull(matchHeaderSnapshot(summary, null, false))
        assertNull(matchHeaderSnapshot(summary, LoadState.Loading, false))
        assertNull(matchHeaderSnapshot(LoadState.Loading, stats, false))
        assertEquals(MatchHeaderSnapshot(null, false), matchHeaderSnapshot(summary, stats, false))
    }

    @Test fun failedOptionalRequestsStillResolveHeader() {
        val failed = LoadState.Failed("offline", true)
        assertEquals(MatchHeaderSnapshot(null, false), matchHeaderSnapshot(failed, failed, false))
    }

    @Test fun commentSelectionTravelsWithItsPreparedOrder() {
        val old = comment("old", 1)
        val new = comment("new", 2)
        val page = CommentPage(listOf(old, new), 2, null, false, listOf(old))
        val hot = prepareCommentFeed(page, 0)
        val latest = prepareCommentFeed(page, 1)
        assertEquals(0, hot.order)
        assertEquals(listOf("old", "new"), hot.comments.map { it.id })
        assertEquals(1, latest.order)
        assertEquals(listOf("new", "old"), latest.comments.map { it.id })
        assertEquals(listOf("old", "new"), hot.comments.map { it.id })
    }

    private fun comment(id: String, time: Long) = HupuComment(
        id, "subject", id, null, id, "", null, 0, 0, publishTime = time,
    )
}

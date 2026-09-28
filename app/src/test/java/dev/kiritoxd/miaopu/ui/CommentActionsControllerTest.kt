package dev.kiritoxd.miaopu.ui

import dev.kiritoxd.miaopu.data.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CommentActionsControllerTest {
    private val comment = HupuComment("comment", "subject", "author", null, "text", "", null, 0, 12)
    private val target = RatingTarget(null, "lol_item", "77174", "player", null, null, emptyList(), null, 0.0, 0, 0, 0, true, true, null, emptyMap())
    private fun success() = AdapterResult.success("test", Unit)
    private fun failure(): AdapterResult<Unit> = AdapterResult.failure("test", AdapterStatus.TRANSIENT_FAILURE, "失败", true)

    @Test fun repeatedLikeIsBlockedAndFailureRestoresCount() = runBlocking {
        val result = CompletableDeferred<AdapterResult<Unit>>()
        var calls = 0
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val actions = CommentActionsController(scope, { _, _ -> calls++; result.await() }, { _, _, _ -> success() }, {})
        try {
            actions.toggleLike(comment)
            actions.toggleLike(comment)
            assertEquals(1, calls)
            assertEquals(CommentLikeState(true, 13, true), actions.like(comment))
            result.complete(failure())
            assertEquals(CommentLikeState(false, 12), actions.like(comment))
        } finally { scope.cancel() }
    }

    @Test fun cancelUsesLatestSelectedState() {
        val selected = mutableListOf<Boolean>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val actions = CommentActionsController(scope, { _, value -> selected += value; success() }, { _, _, _ -> success() }, {})
            actions.toggleLike(comment)
            actions.toggleLike(comment)
            assertEquals(listOf(true, false), selected)
            assertEquals(CommentLikeState(false, 12), actions.like(comment))
        } finally { scope.cancel() }
    }

    @Test fun failedReplyRetainsDraftAndSuccessfulRetryClearsIt() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var failed = true
        var refreshed = 0
        try {
            val actions = CommentActionsController(scope, { _, _ -> success() }, { _, parent, text ->
                assertEquals("comment", parent)
                assertEquals("回复内容", text)
                if (failed) failure() else success()
            }, {})
            actions.updateDraft(comment, "回复内容")
            actions.publish(target, comment) { refreshed++ }
            assertEquals("回复内容", actions.draft(comment).text)
            assertNotNull(actions.draft(comment).error)
            failed = false
            actions.publish(target, comment) { refreshed++ }
            assertEquals(ReplyDraft(), actions.draft(comment))
            assertEquals(1, refreshed)
        } finally { scope.cancel() }
    }

    @Test fun selectedChildIsThePublishParentAndDraftsStayIndependent() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val child = comment.copy(id = "child", author = "other", parentCommentId = comment.id)
        var sentParent: String? = null
        try {
            val actions = CommentActionsController(scope, { _, _ -> success() }, { _, parent, _ ->
                sentParent = parent
                success()
            }, {})
            actions.updateDraft(comment, "根评论草稿")
            actions.updateDraft(child, "回复子评论")
            actions.publish(target, child) { actions.recordThreadReply(comment) }
            assertEquals("child", sentParent)
            assertEquals("根评论草稿", actions.draft(comment).text)
            assertEquals("", actions.draft(child).text)
            assertEquals(1, actions.replyCount(comment))
        } finally { scope.cancel() }
    }

    @Test fun clearingCancelsPendingReplyWithoutRefreshingNewPage() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val result = CompletableDeferred<AdapterResult<Unit>>()
        var refreshed = false
        try {
            val actions = CommentActionsController(scope, { _, _ -> success() }, { _, _, _ -> result.await() }, {})
            actions.updateDraft(comment, "回复")
            actions.publish(target, comment) { refreshed = true }
            actions.clear()
            result.complete(success())
            assertFalse(refreshed)
            assertEquals(ReplyDraft(), actions.draft(comment))
        } finally { scope.cancel() }
    }
}

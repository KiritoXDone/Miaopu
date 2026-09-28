package dev.kiritoxd.miaopu.ui

import androidx.compose.runtime.*
import dev.kiritoxd.miaopu.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal data class CommentLikeState(val selected: Boolean, val count: Int, val pending: Boolean = false)
internal data class ReplyDraft(val text: String = "", val publishing: Boolean = false, val error: String? = null)

/** Keeps reply drafts and optimistic likes independent from comment pagination. */
internal class CommentActionsController(
    private val scope: CoroutineScope,
    private val setLight: suspend (HupuComment, Boolean) -> AdapterResult<Unit>,
    private val sendReply: suspend (RatingTarget, String, String) -> AdapterResult<Unit>,
    private val onMessage: (String) -> Unit,
) {
    private val likes = mutableStateMapOf<String, CommentLikeState>()
    private val replyCounts = mutableStateMapOf<String, Int>()
    private val drafts = mutableStateMapOf<String, ReplyDraft>()
    private val jobs = mutableListOf<Job>()
    private var generation = 0
    private fun key(comment: HupuComment) = "${comment.subjectId}:${comment.id}"
    fun like(comment: HupuComment) = likes[key(comment)] ?: CommentLikeState(comment.hasLight, comment.lightCount)
    fun replyCount(parent: HupuComment) = maxOf(parent.replyCount, replyCounts[key(parent)] ?: 0)
    fun recordThreadReply(parent: HupuComment) { replyCounts[key(parent)] = replyCount(parent) + 1 }
    fun draft(parent: HupuComment) = drafts[key(parent)] ?: ReplyDraft()
    fun updateDraft(parent: HupuComment, text: String) {
        if (!draft(parent).publishing) drafts[key(parent)] = ReplyDraft(text.take(500))
    }

    fun toggleLike(comment: HupuComment) {
        if (comment.id.isBlank() || comment.subjectId.isBlank()) { onMessage("这条评论暂不支持点赞"); return }
        val previous = like(comment)
        if (previous.pending) return
        val id = key(comment)
        val token = generation
        val selected = !previous.selected
        likes[id] = CommentLikeState(selected, (previous.count + if (selected) 1 else -1).coerceAtLeast(0), true)
        jobs.removeAll { it.isCompleted }
        jobs += scope.launch {
            val result = setLight(comment, selected)
            if (token != generation) return@launch
            if (result.status == AdapterStatus.SUCCESS) likes[id] = likes.getValue(id).copy(pending = false)
            else { likes[id] = previous; onMessage(result.error?.message ?: "点赞操作失败") }
        }
    }

    fun publish(target: RatingTarget, parent: HupuComment, onSuccess: () -> Unit) {
        val current = draft(parent)
        if (current.publishing || current.text.isBlank() || parent.id.isBlank() || !target.canComment) return
        val id = key(parent)
        val token = generation
        drafts[id] = current.copy(publishing = true, error = null)
        jobs.removeAll { it.isCompleted }
        jobs += scope.launch {
            val result = sendReply(target, parent.id, current.text)
            if (token != generation) return@launch
            if (result.status == AdapterStatus.SUCCESS) {
                drafts[id] = ReplyDraft()
                replyCounts[id] = replyCount(parent) + 1
                onSuccess()
            } else drafts[id] = current.copy(error = result.error?.message ?: "回复发送失败")
        }
    }

    fun clear() {
        generation++
        jobs.forEach(Job::cancel)
        jobs.clear()
        likes.clear()
        drafts.clear()
        replyCounts.clear()
    }
}

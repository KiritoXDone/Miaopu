package dev.kiritoxd.miaopu.ui

import dev.kiritoxd.miaopu.data.CommentPage
import dev.kiritoxd.miaopu.data.HupuComment
import dev.kiritoxd.miaopu.data.listKey
import dev.kiritoxd.miaopu.data.mergeCommentsByHeat

internal data class PreparedCommentFeed(val order: Int, val page: CommentPage, val comments: List<HupuComment>)

internal fun prepareCommentFeed(page: CommentPage, order: Int): PreparedCommentFeed = PreparedCommentFeed(
    order, page,
    if (order == 0) mergeCommentsByHeat(page.hottestComments, page.comments, page.hottestComments.map { it.id })
    else page.comments.distinctBy { it.listKey }.sortedByDescending { it.publishTime ?: Long.MIN_VALUE },
)

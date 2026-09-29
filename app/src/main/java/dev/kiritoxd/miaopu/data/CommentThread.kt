package dev.kiritoxd.miaopu.data

internal data class CommentThreadRow(
    val comment: HupuComment,
    val depth: Int,
    val ancestorContinues: List<Boolean>,
    val hasNextSibling: Boolean,
)

/** Stable depth-first order; missing parents and malformed cycles never hide a comment. */
internal fun commentThreadRows(root: HupuComment, replies: List<HupuComment>, checkActive: () -> Unit = {}): List<CommentThreadRow> {
    checkActive()
    val unique = replies.filter { checkActive(); it.id != root.id }.distinctBy { it.id }
    val byId = unique.associateBy { it.id } + (root.id to root)
    val children = unique.groupBy { reply ->
        checkActive()
        reply.parentCommentId?.takeIf { it in byId && it != reply.id } ?: root.id
    }
    val visited = mutableSetOf(root.id)
    val result = mutableListOf<CommentThreadRow>()
    data class Pending(val comment: HupuComment, val depth: Int, val ancestors: List<Boolean>, val next: Boolean)
    val stack = ArrayDeque<Pending>()
    fun enqueue(items: List<HupuComment>, depth: Int, ancestors: List<Boolean>) {
        items.asReversed().forEachIndexed { reverseIndex, comment ->
            checkActive()
            stack.addLast(Pending(comment, depth, ancestors, reverseIndex != 0))
        }
    }
    fun drain() {
        while (stack.isNotEmpty()) {
            checkActive()
            val row = stack.removeLast()
            if (!visited.add(row.comment.id)) continue
            val comment = row.comment.copy(parentAuthor = row.comment.parentAuthor
                ?: byId[row.comment.parentCommentId]?.author)
            result += CommentThreadRow(comment, row.depth, row.ancestors, row.next)
            enqueue(children[comment.id].orEmpty(), row.depth + 1, row.ancestors + row.next)
        }
    }
    enqueue(children[root.id].orEmpty(), 1, emptyList())
    drain()
    unique.forEach { checkActive(); if (it.id !in visited) { enqueue(listOf(it), 1, emptyList()); drain() } }
    return result
}

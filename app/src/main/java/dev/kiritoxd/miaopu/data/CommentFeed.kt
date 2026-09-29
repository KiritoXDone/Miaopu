package dev.kiritoxd.miaopu.data

internal val HupuComment.listKey: String
    get() = id.ifBlank { "${subjectId}:${author}:${content}" }

/** Keeps the official hottest order, then appends paginated comments without visible reordering. */
fun mergeCommentsByHeat(
    existing: List<HupuComment>,
    incoming: List<HupuComment>,
    officialHotOrder: List<String> = emptyList(),
): List<HupuComment> {
    val unique = linkedMapOf<String, HupuComment>()
    (existing + incoming).forEach { comment ->
        val key = comment.listKey
        unique.putIfAbsent(key, comment)
    }
    val hottest = officialHotOrder.mapNotNull { id -> unique.remove(id) }
    return hottest + unique.values
}

/** Returns the replied-to user only when this is a reply to another child reply. */
fun HupuComment.nestedReplyTarget(rootCommentId: String): String? =
    if (parentCommentId != null && parentCommentId != rootCommentId) {
        when {
            parentDeleted || parentCanSee == false -> "已删除内容"
            else -> parentAuthor
        }
    } else {
        null
    }

package dev.kiritoxd.miaopu.data

import org.junit.Assert.*
import org.junit.Test

class CommentListKeyTest {
    private fun comment(id: String, text: String) =
        HupuComment(id, "subject", "author", null, text, "", null, 0, 0)

    @Test fun identifiedCommentKeepsKeyAfterLikeOrContentUpdates() {
        val original = comment("42", "before")
        assertEquals(original.listKey, original.copy(content = "after", lightCount = 7).listKey)
    }

    @Test fun missingIdsKeepDistinctCommentsAndDeduplicateRepeatedPages() {
        val first = comment("", "first")
        val second = comment("", "second")
        val merged = mergeCommentsByHeat(listOf(first), listOf(first, second))
        assertEquals(listOf(first, second), merged)
        assertEquals(2, merged.map { it.listKey }.distinct().size)
    }
}

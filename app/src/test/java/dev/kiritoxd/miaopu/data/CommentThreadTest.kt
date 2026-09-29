package dev.kiritoxd.miaopu.data

import org.junit.Assert.*
import org.junit.Test

class CommentThreadTest {
    @Test fun cancelledPreparationStopsBeforeReturningRows() {
        var checks = 0
        try {
            commentThreadRows(comment("root"), List(100) { comment("$it", "root") }) {
                if (++checks == 10) throw java.util.concurrent.CancellationException()
            }
            fail("Expected cancellation")
        } catch (_: java.util.concurrent.CancellationException) {
            assertEquals(10, checks)
        }
    }

    private fun comment(id: String, parent: String? = null) = HupuComment(id, "subject", id, null, id, "", null, 0, 0, parentCommentId = parent)
    @Test fun buildsDepthFirstBranchesWithActualRecipients() {
        val root = comment("root")
        val rows = commentThreadRows(root, listOf(comment("a", "root"), comment("b", "root"), comment("c", "a"), comment("d", "c")))
        assertEquals(listOf("a", "c", "d", "b"), rows.map { it.comment.id })
        assertEquals(listOf(1, 2, 3, 1), rows.map { it.depth })
        assertEquals("c", rows[2].comment.parentAuthor)
        assertEquals(listOf(true, false), rows[2].ancestorContinues)
        assertTrue(rows[0].hasNextSibling)
        assertFalse(rows.last().hasNextSibling)
    }
    @Test fun missingParentsCyclesAndDuplicatesRemainVisibleOnce() {
        val rows = commentThreadRows(comment("root"), listOf(comment("a", "b"), comment("b", "a"), comment("lost", "unknown"), comment("lost", "unknown")))
        assertEquals(setOf("a", "b", "lost"), rows.map { it.comment.id }.toSet())
        assertEquals(3, rows.size)
        assertNull(rows.first { it.comment.id == "lost" }.comment.parentAuthor)
    }
}

package dev.kiritoxd.miaopu.data

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CommentWritePayloadsTest {
    @Test fun lightKeyContainsBothIdentifiers() {
        val root = JSONObject(CommentWritePayloads.light("2977581849", "372187670"))
        assertEquals(1, root.length())
        val key = root.getJSONObject("commentKey")
        assertEquals("2977581849", key.getString("commentId"))
        assertEquals("372187670", key.getString("subjectId"))
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsMissingSubjectBeforeSending() { CommentWritePayloads.light("123", "") }

    @Test fun replyUsesRootParentAndBusinessKey() {
        val target = RatingTarget(null, "lol_item", "77174", "player", null, null, emptyList(), null, 0.0, 0, 0, 0, true, true, null, emptyMap())
        val root = JSONObject(CommentWritePayloads.reply(target, "2986472970", " 回复内容 "))
        assertEquals("2986472970", root.getString("parentCommentId"))
        assertEquals("回复内容", root.getString("content"))
        assertEquals("77174", root.getJSONObject("outBizKey").getString("outBizNo"))
        assertEquals(0, root.getJSONArray("images").length())
        assertEquals(0, root.getJSONArray("ancillaryContents").length())
    }
}

package dev.kiritoxd.miaopu.data

import org.json.JSONArray
import org.json.JSONObject

/** Business fields verified against successful comment requests. */
internal object CommentWritePayloads {
    fun reply(target: RatingTarget, parentId: String, content: String, images: List<String> = emptyList()): String {
        require(parentId.isNotBlank())
        require(content.isNotBlank() || images.isNotEmpty())
        return JSONObject().put("outBizKey", JSONObject()
            .put("outBizType", target.outBizType).put("outBizNo", target.outBizNo))
            .put("parentCommentId", parentId).put("content", content.trim())
            .put("images", JSONArray(images)).put("ancillaryContents", JSONArray()).toString()
    }

    fun light(commentId: String, subjectId: String): String {
        require(commentId.isNotBlank() && subjectId.isNotBlank())
        return JSONObject().put("commentKey", JSONObject()
            .put("commentId", commentId).put("subjectId", subjectId)).toString()
    }
}

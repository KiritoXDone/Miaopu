package dev.kiritoxd.miaopu.data

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object HupuUrls {
    const val SCHEDULE_BASE = "https://match-api.hupu.com"
    const val SCORE_BASE = "https://games.mobileapi.hupu.com/1/8.2.99"
    const val COMMENT_LIGHT_BASE = "https://games.mobileapi.hupu.com/9/8.0.99"
    const val DETAIL_BASE = "https://offline-download.hupu.com/online/prod/310016/detail.html"
    private const val PASSPORT_BASE = "https://passport.hupu.com/v2/login"

    val cookieOrigins = listOf(
        "https://hupu.com",
        "https://passport.hupu.com",
        "https://offline-download.hupu.com",
        "https://games.mobileapi.hupu.com",
        "https://match-api.hupu.com",
        "https://bbs.mobileapi.hupu.com",
    )

    fun detailUrl(outBizType: String, outBizNo: String): String =
        "$DETAIL_BASE?outBizType=${encode(outBizType)}&outBizNo=${encode(outBizNo)}&isCheckInfo=1"

    fun loginUrl(returnUrl: String = DETAIL_BASE): String {
        val encoded = encode(returnUrl)
        return "$PASSPORT_BASE?phone=1&jumpurl=$encoded&from=$encoded#/"
    }

    fun isTrustedWebUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        if (uri.scheme != "https") return false
        val host = uri.host?.lowercase() ?: return false
        return host == "hupu.com" ||
            host.endsWith(".hupu.com") ||
            host == "hoopchina.com.cn" ||
            host.endsWith(".hoopchina.com.cn")
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}

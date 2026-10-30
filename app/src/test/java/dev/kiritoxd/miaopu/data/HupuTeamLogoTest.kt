package dev.kiritoxd.miaopu.data

import dev.kiritoxd.miaopu.data.matchdetail.MatchDetailParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HupuTeamLogoTest {
    @Test
    fun `legacy CDN images use HTTPS without changing the path or query`() {
        assertEquals(HTTPS_LOGO, HupuUrls.imageUrl(HTTP_LOGO))
        assertEquals("$HTTPS_LOGO?size=80&v=2", HupuUrls.imageUrl("$HTTP_LOGO?size=80&v=2"))
        assertEquals(HTTPS_LOGO, HupuUrls.imageUrl(HTTPS_LOGO))
    }

    @Test
    fun `missing and unrelated image URLs keep their existing behavior`() {
        assertNull(HupuUrls.imageUrl(null))
        assertNull(HupuUrls.imageUrl(""))
        listOf(
            "http://example.com/logo.png",
            "http://i11.hoopchina.com.cn.example.com/logo.png",
            "http://i11.hoopchina.com.cn:8080/logo.png",
            "http://user@i11.hoopchina.com.cn/logo.png",
            "not a URL",
        ).forEach { assertEquals(it, HupuUrls.imageUrl(it)) }
    }

    @Test
    fun `NBA schedule supplies secure team logos to list detail and widget consumers`() {
        val schedule = HupuJsonParser.schedule("""
            {"result":{"dayGameData":[{"dayTime":"2026-10-30","matchData":[{
              "matchId":"nba-test","matchName":"NBA","againstInfo":{"memberInfos":[
                {"memberId":"1901000000501323","memberName":"湖人","memberLogo":"$HTTP_LOGO"},
                {"memberId":"1901000000501331","memberName":"勇士","memberLogo":"$HTTPS_LOGO"}
              ]},"scoreItemInfo":{"name":"球员","teamLogo":"$HTTP_LOGO"}
            }]}]}}
        """.trimIndent(), Esport.NBA)

        val match = schedule.days.single().matches.single()
        assertEquals(listOf("湖人", "勇士"), match.teams.map { it.name })
        assertEquals(listOf(HTTPS_LOGO, HTTPS_LOGO), match.teams.map { it.logoUrl })
        assertEquals(HTTPS_LOGO, match.featuredPlayer?.teamLogoUrl)
    }

    @Test
    fun `rating group and overall score team logos use HTTPS`() {
        val groups = HupuJsonParser.ratingGroups("""
            {"data":[{"rootNodeId":1,"groupName":"湖人","groupAttributes":[
              {"attributeKey":"logo","attributeValue":"$HTTP_LOGO"}
            ]}]}
        """.trimIndent())
        assertEquals(HTTPS_LOGO, groups.single().logoUrl)

        val scores = MatchDetailParser.allScores("""
            {"success":true,"result":{"memberBasicInfos":[
              {"memberName":"湖人","memberLogo":"$HTTP_LOGO"}
            ]}}
        """.trimIndent())
        assertEquals(HTTPS_LOGO, scores.teams.single().logoUrl)
    }

    private companion object {
        const val HTTP_LOGO = "http://i11.hoopchina.com.cn/all-backend/708BC902E00BBD439A1C892A0D58F381_1657174320036.png"
        const val HTTPS_LOGO = "https://i11.hoopchina.com.cn/all-backend/708BC902E00BBD439A1C892A0D58F381_1657174320036.png"
    }
}

package dev.kiritoxd.miaopu.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EsportCatalogTest {
    @Test fun savedOrderRetainsOnlySubscribedProjectsAndAppendsNewOnes() {
        assertEquals(listOf(Esport.VALORANT, Esport.LOL, Esport.KOG),
            EsportCatalog.orderedSubscriptions(setOf("lol", "kog", "val"), listOf("val", "unknown", "val", "cs2", "lol")))
        assertEquals(EsportCatalog.subscriptions(null).toList(), EsportCatalog.orderedSubscriptions(null, emptyList()))
        assertEquals(listOf(Esport.LOL), EsportCatalog.orderedSubscriptions(emptySet(), listOf("val")))
    }

    @Test
    fun defaultSubscriptionsContainEveryVerifiedScheduleProject() {
        assertEquals(
            listOf(Esport.LOL, Esport.KOG, Esport.CS2, Esport.VALORANT, Esport.PUBG),
            EsportCatalog.subscriptions(null).toList(),
        )
    }

    @Test
    fun savedSubscriptionsIgnoreUnknownIdsAndKeepCatalogOrder() {
        assertEquals(
            listOf(Esport.CS2, Esport.PUBG),
            EsportCatalog.subscriptions(setOf("pubg", "unknown", "cs2")).toList(),
        )
    }

    @Test
    fun emptyOrUnknownSelectionFallsBackToLol() {
        assertEquals(setOf(Esport.LOL), EsportCatalog.subscriptions(emptySet()))
        assertEquals(setOf(Esport.LOL), EsportCatalog.subscriptions(setOf("unknown")))
    }

    @Test
    fun sportsProjectsAreAvailableButRemainOptIn() {
        assertEquals(
            listOf(
                Esport.TENNIS,
                Esport.TABLE_TENNIS,
                Esport.BADMINTON,
                Esport.FORMULA_ONE,
                Esport.SNOOKER,
                Esport.VOLLEYBALL,
            ),
            EsportCatalog.all.filter { it.category == ScheduleCategory.SPORTS },
        )
        assertEquals(
            listOf(Esport.NBA, Esport.CBA, Esport.WNBA, Esport.CUBAL),
            EsportCatalog.all.filter { it.category == ScheduleCategory.BASKETBALL },
        )
        assertEquals(
            listOf(Esport.ENGLISH_PREMIER_LEAGUE, Esport.WORLD_CUP, Esport.FOOTBALL),
            EsportCatalog.all.filter { it.category == ScheduleCategory.FOOTBALL },
        )
        assertTrue(EsportCatalog.subscriptions(null).all { it.category == ScheduleCategory.ESPORTS })
        assertTrue(
            EsportCatalog.all
                .filterNot { it.category == ScheduleCategory.ESPORTS }
                .none(Esport::defaultSubscribed),
        )
        assertEquals("乒乓球", Esport.TABLE_TENNIS.shortTitle)
    }

    @Test
    fun everySportsProjectFiltersItsCommonScheduleSupplement() {
        val tennis = Esport.TENNIS.supplementalSources.single()
        val tableTennis = Esport.TABLE_TENNIS.supplementalSources.single()
        val badminton = Esport.BADMINTON.supplementalSources.single()

        assertTrue(tennis.includes("美国网球公开赛男单"))
        assertFalse(tennis.includes("羽毛球世锦赛男单"))
        assertTrue(tableTennis.includes("WTT瑞典大满贯女单"))
        assertFalse(tableTennis.includes("ATP辛辛那提站男单"))
        assertTrue(badminton.includes("羽毛球世锦赛男双决赛"))
        assertFalse(badminton.includes("WTT瑞典大满贯男单"))

        assertTrue(Esport.FORMULA_ONE.supplementalSources.single().includes("F1荷兰站正赛"))
        assertTrue(Esport.SNOOKER.supplementalSources.single().includes("斯诺克武汉公开赛8进4"))
        assertTrue(Esport.FOOTBALL.supplementalSources.single().includes("德国杯1/8决赛"))
        assertFalse(Esport.FOOTBALL.supplementalSources.single().includes("英超联赛"))
        assertFalse(Esport.FOOTBALL.supplementalSources.single().includes("足球世界杯决赛"))
        assertTrue(Esport.VOLLEYBALL.supplementalSources.single().includes("女排亚锦赛小组赛"))

        assertTrue(Esport.NBA.supplementalSources.single().includes("NBA常规赛"))
        assertFalse(Esport.NBA.supplementalSources.single().includes("WNBA常规赛"))
        assertEquals("chinabasketball", Esport.CBA.supplementalSources.first().businessId)
        assertTrue(Esport.CBA.supplementalSources.first().includes("任意中国篮球赛事"))
        assertTrue(Esport.WNBA.supplementalSources.single().includes("WNBA常规赛"))
        assertTrue(Esport.CUBAL.supplementalSources.single().includes("CUBAL总决赛"))
        assertTrue(Esport.ENGLISH_PREMIER_LEAGUE.supplementalSources.single().includes("英超联赛"))
        assertTrue(Esport.WORLD_CUP.supplementalSources.single().includes("世界杯决赛"))
        assertFalse(Esport.WORLD_CUP.supplementalSources.single().includes("男篮世界杯小组赛"))
    }
}

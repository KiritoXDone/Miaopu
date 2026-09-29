package dev.kiritoxd.miaopu.ui

import org.junit.Assert.*
import org.junit.Test

class MatchDataCacheTest {
    @Test fun expiresAtDeadlineAndClearInvalidatesResults() {
        var clock = 0L
        val cache = MatchDataCache<String, String>(now = { clock }, lifetimeMillis = 30)
        cache.put("map", "old")
        clock = 29
        assertEquals("old", cache["map"])
        clock = 30
        assertNull(cache["map"])
        cache.put("map", "new")
        cache.clear()
        assertNull(cache["map"])
    }

    @Test fun boundsEntriesAndReplacesDuplicateKeys() {
        val cache = MatchDataCache<String, Int>(now = { 0 }, capacity = 2)
        cache.put("a", 1)
        cache.put("b", 2)
        cache.put("a", 3)
        cache.put("c", 4)
        assertEquals(3, cache["a"])
        assertNull(cache["b"])
        assertEquals(4, cache["c"])
    }
}

package dev.kiritoxd.miaopu.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BottomBarStyleTest {
    @Test fun legacySettingKeepsItsExistingAppearance() {
        assertEquals(BottomBarStyle.CLASSIC, BottomBarStyle.restore(null, legacyFloating = false))
        assertEquals(BottomBarStyle.FLOATING_ICON, BottomBarStyle.restore(null, legacyFloating = true))
    }

    @Test fun savedStylesTakePrecedenceOverLegacySetting() {
        BottomBarStyle.entries.forEach { style ->
            assertEquals(style, BottomBarStyle.restore(style.name, legacyFloating = false))
            assertEquals(style, BottomBarStyle.restore(style.name, legacyFloating = true))
        }
    }

    @Test fun unknownSavedStyleFallsBackToTheLegacySetting() {
        assertEquals(BottomBarStyle.CLASSIC, BottomBarStyle.restore("UNKNOWN", legacyFloating = false))
        assertEquals(BottomBarStyle.FLOATING_ICON, BottomBarStyle.restore("UNKNOWN", legacyFloating = true))
    }
}

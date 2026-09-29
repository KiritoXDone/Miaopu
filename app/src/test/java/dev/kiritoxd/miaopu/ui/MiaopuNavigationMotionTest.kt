package dev.kiritoxd.miaopu.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MiaopuNavigationMotionTest {
    @Test fun backProgressTracksFingerAndReturnsToRestWithoutJump() {
        assertEquals(0f, navigationTranslationX(0f, 1080f, false), 0.001f)
        assertEquals(270f, navigationTranslationX(-0.25f, 1080f, false), 0.001f)
        assertEquals(1080f, navigationTranslationX(-1f, 1080f, false), 0.001f)
        assertEquals(0.108f, navigationTranslationX(-0.0001f, 1080f, false), 0.001f)
    }
    @Test fun coveredLayerParallaxesAndRtlMirrorsBothLayers() {
        for (depth in listOf(-1f, -0.5f, 0f, 0.5f, 1f)) {
            assertEquals(-navigationTranslationX(depth, 1080f, false), navigationTranslationX(depth, 1080f, true), 0.001f)
        }
        assertEquals(-270f, navigationTranslationX(1f, 1080f, false), 0.001f)
        assertEquals(-270f, navigationTranslationX(2f, 1080f, false), 0.001f)
    }
}

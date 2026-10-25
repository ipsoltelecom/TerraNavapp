package com.example.northstar.settings

import com.example.northstar.dash.map.*
import org.junit.Assert.*
import org.junit.Test

class MapBehaviorTest {
    @Test fun automaticThemeHandlesBothBoundariesAndMidnight() {
        for (hour in 0..23) {
            assertEquals(hour < 6 || hour >= 18, MapThemeMode.AUTO.isNight(hour))
            assertTrue(MapThemeMode.NIGHT.isNight(hour))
            assertFalse(MapThemeMode.DAY.isNight(hour))
        }
    }
    @Test fun motionHasHysteresisAndStopsWhenFixIsStaleOrInaccurate() {
        val p = MotionPerspective()
        assertFalse(p.update(1.8f, 0, 5f))
        assertTrue(p.update(2f, 0, 5f))
        assertTrue(p.update(1.5f, 0, 5f))
        assertFalse(p.update(0.9f, 0, 5f))
        assertTrue(p.update(10f, 0, 5f))
        assertFalse(p.update(10f, 5001, 5f))
        assertFalse(p.update(10f, 0, 40f))
        assertFalse(p.update(Float.NaN, 0, 5f))
    }
}

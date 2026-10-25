package com.example.northstar.dash.map

enum class MapThemeMode {
    DAY, NIGHT, AUTO;
    fun isNight(hour: Int): Boolean = when (this) {
        DAY -> false
        NIGHT -> true
        AUTO -> hour >= 18 || hour < 6
    }
}

/** Hysteresis avoids camera oscillation around walking speed; stale fixes flatten the map. */
class MotionPerspective {
    var moving = false
        private set
    fun update(speedMps: Float, ageMs: Long, accuracy: Float): Boolean {
        moving = if (!speedMps.isFinite() || !accuracy.isFinite() || accuracy > 30f || ageMs !in 0..5_000)
            false
        else if (moving) speedMps >= 1.0f else speedMps >= 2.0f
        return moving
    }
}

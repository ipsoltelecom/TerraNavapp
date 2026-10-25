package com.example.northstar.settings

import org.junit.Assert.*
import org.junit.Test

class AppOptionsTest {
    @Test fun invalidSavedOptionsRecoverToCompatibleDefaults() {
        for (value in listOf(null, "", "removed-option")) {
            assertEquals(StreamQuality.BALANCED, StreamQuality.fromStored(value))
            assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag(value))
        }
    }
    @Test fun savedSelectionsRoundTrip() {
        AppLanguage.entries.forEach { assertEquals(it, AppLanguage.fromTag(it.tag)) }
        StreamQuality.entries.forEach { assertEquals(it, StreamQuality.fromStored(it.name)) }
        assertEquals(setOf("en", "pt-BR"), AppLanguage.entries.map { it.tag }.toSet())
    }
    @Test fun defaultProfilePreservesValidatedStreamAndAlternativesStayBounded() {
        assertEquals(1_200_000, StreamQuality.BALANCED.bitrate)
        assertEquals(24, StreamQuality.BALANCED.movingFps)
        assertEquals(7, StreamQuality.BALANCED.idleFps)
        assertTrue(StreamQuality.ECONOMY.bitrate < StreamQuality.BALANCED.bitrate)
        assertTrue(StreamQuality.HIGH.bitrate > StreamQuality.BALANCED.bitrate)
        StreamQuality.entries.forEach {
            assertTrue(it.idleFps in 1..it.movingFps)
            assertTrue(it.movingFps <= 24)
        }
    }
}

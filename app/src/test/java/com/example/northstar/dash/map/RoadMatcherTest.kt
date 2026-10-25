package com.example.northstar.dash.map

import com.example.northstar.dash.nav.GeoPoint
import org.junit.Assert.*
import org.junit.Test

class RoadMatcherTest {
    private val points = listOf(GeoPoint(0.0,0.0),GeoPoint(0.0,0.01))
    private val here = GeoPoint(0.0,0.005)
    private fun road(tags: Map<String,String> = mapOf("maxspeed" to "60")) = RoadWay(1,points,tags)
    @Test fun parsesOnlyUnambiguousNumericLimits() {
        assertEquals(60,RoadMatcher.parseSpeed("60"))
        assertEquals(48,RoadMatcher.parseSpeed("30 mph"))
        listOf("BR:urban","signals","none","50;70","60 @ (wet)","0","999").forEach { assertNull(RoadMatcher.parseSpeed(it)) }
    }
    @Test fun matchesNearbyRoadButRejectsPoorOrStaleGps() {
        assertEquals(60,RoadMatcher.limit(listOf(road()),here,5f,90f,1000))
        assertNull(RoadMatcher.limit(listOf(road()),here,30f,90f,1000))
        assertNull(RoadMatcher.limit(listOf(road()),here,5f,90f,11000))
        assertNull(RoadMatcher.limit(listOf(road()),GeoPoint(0.001,0.005),5f,90f,1000))
    }
    @Test fun respectsDirectionAndConditionalUncertainty() {
        val r = road(mapOf("maxspeed:forward" to "60","maxspeed:backward" to "40"))
        assertEquals(60,RoadMatcher.limit(listOf(r),here,5f,90f,1000))
        assertEquals(40,RoadMatcher.limit(listOf(r),here,5f,270f,1000))
        assertNull(RoadMatcher.limit(listOf(r),here,5f,null,1000))
        assertNull(RoadMatcher.limit(listOf(road(mapOf("maxspeed" to "60","maxspeed:conditional" to "30 @ (wet)"))),here,5f,90f,1000))
    }
    @Test fun rejectsParallelUnknownRoad() {
        val parallel = RoadWay(2,points.map { GeoPoint(it.lat+0.00005,it.lng) },emptyMap())
        assertNull(RoadMatcher.limit(listOf(road(),parallel),here,8f,90f,1000))
    }
    @Test fun cacheMustBeFreshAndLocal() {
        val s = RoadSnapshot(center=here,fetchedAt=1000)
        assertTrue(s.covers(here,2000))
        assertFalse(s.covers(here,86_402_000))
        assertFalse(s.covers(GeoPoint(1.0,1.0),2000))
    }
}

package com.routewatcher.app.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutesApiClientTest {

    @Test
    fun parseSecondsField_parsesTrailingSFormat() {
        assertEquals(1371, RoutesApiClient.parseSecondsField("1371s"))
    }

    @Test
    fun parseSecondsField_returnsZeroForMalformedInput() {
        assertEquals(0, RoutesApiClient.parseSecondsField("not-a-duration"))
    }

    @Test
    fun decodePolyline_decodesGoogleReferenceExample() {
        val points = RoutesApiClient.decodePolyline("_p~iF~ps|U_ulLnnqC_mqNvxq`@")
        assertEquals(3, points.size)
        assertTrue(Math.abs(points[0].first - 38.5) < 0.001)
        assertTrue(Math.abs(points[0].second - (-120.2)) < 0.001)
    }
}
package com.routewatcher.app.data
import org.junit.Assert.assertEquals
import org.junit.Test


class RouteEntityTest {
    @Test
    fun offsetsList_parsesCommaSeparatedMinutes() {
        val route = RouteEntity(
            name = "Test",
            originAddress = "A",
            destinationAddress = "B",
            checkOffsetsMinutes = "60,30,10",
        )
        assertEquals(listOf(60, 30, 10), route.offsetsList())
    }

    @Test
    fun offsetsList_skipsBadEntries() {
        val route = RouteEntity(
            name = "Test",
            originAddress = "A",
            destinationAddress = "B",
            checkOffsetsMinutes = "30, , abc,10",
        )
        assertEquals(listOf(30, 10), route.offsetsList())
    }
}
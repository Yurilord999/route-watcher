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

    @Test
    fun directionsUrl_encodesSpacesAndSpecialCharacters() {
        val route = RouteEntity(
            name = "Test",
            originAddress = "Dresden Hauptbahnhof, Dresden",
            destinationAddress = "Frauenkirche Dresden, Dresden",
        )
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&origin=Dresden+Hauptbahnhof%2C+Dresden" +
                    "&destination=Frauenkirche+Dresden%2C+Dresden&travelmode=driving",
            route.directionsUrl(),
        )
    }
}
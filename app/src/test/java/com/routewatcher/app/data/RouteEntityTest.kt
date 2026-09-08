package com.routewatcher.app.data
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar


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

    @Test
    fun dayBitFor_mapsCalendarDayToCorrectBit() {
        val wednesday = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY) }
        assertEquals(RouteEntity.WEDNESDAY, RouteEntity.dayBitFor(wednesday))
    }

    @Test
    fun defaultActiveDays_isExactlyOneDayBit() {
        val allDayBits = setOf(
            RouteEntity.MONDAY, RouteEntity.TUESDAY, RouteEntity.WEDNESDAY, RouteEntity.THURSDAY,
            RouteEntity.FRIDAY, RouteEntity.SATURDAY, RouteEntity.SUNDAY,
        )
        val defaultRoute = RouteEntity(name = "Test", originAddress = "A", destinationAddress = "B")
        // Can't assert which day without the test flaking depending on when it runs -
        // just that it's today-only (a single valid day bit), not a multi-day preset.
        assertTrue(defaultRoute.activeDays in allDayBits)
    }
}
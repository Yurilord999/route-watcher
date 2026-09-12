package com.routewatcher.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.net.URLEncoder
import java.util.Calendar

// A single commute route (e.g. "home -> work")
@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val originAddress: String,
    val destinationAddress: String,
    val departureHour: Int = 8,
    val departureMinute: Int = 0,
    val checkOffsetsMinutes: String = "30",
    val delayThresholdMinutes: Int = 10,
    val activeDays: Int = dayBitFor(),

    // Only true when waypoints are set in custom route mode
    val enabled: Boolean = false,

    val isCustomRoute: Boolean = false,
    // Locked-in route set once the user picks a specific route on the map
    // Falls back to  origin/destination routing if null.
    val lockedRoutePolyline: String? = null,
    val lockedRouteSummary: String? = null,
    val lockedRouteWaypoints: String? = null, // 3 pairs of (lat,lng)
    val lockedRouteDurationMinutes: Int? = null,
    val lockedRouteDistanceText: String? = null,

) {
    fun offsetsList(): List<Int> =
        checkOffsetsMinutes.split(",").mapNotNull { it.trim().toIntOrNull() }

    fun lockedWaypointsList(): List<Pair<Double, Double>> =
        lockedRouteWaypoints
            ?.split(";")
            ?.mapNotNull { pair ->
                val parts = pair.split(",")
                val lat = parts.getOrNull(0)?.trim()?.toDoubleOrNull()
                val lng = parts.getOrNull(1)?.trim()?.toDoubleOrNull()
                if (lat != null && lng != null) lat to lng else null
            }
            ?: emptyList()

    // Builds a Google Maps "directions" web link from this routes origin/destination
    fun directionsUrl(): String {
        val origin = URLEncoder.encode(originAddress, "UTF-8")
        val destination = URLEncoder.encode(destinationAddress, "UTF-8")
        return "https://www.google.com/maps/dir/?api=1&origin=$origin&destination=$destination&travelmode=driving"
    }

    companion object {
        const val MONDAY = 1
        const val TUESDAY = 2
        const val WEDNESDAY = 4
        const val THURSDAY = 8
        const val FRIDAY = 16
        const val SATURDAY = 32
        const val SUNDAY = 64
        const val ALL_DAYS = MONDAY or TUESDAY or WEDNESDAY or THURSDAY or FRIDAY or SATURDAY or SUNDAY
        const val WEEKDAYS = MONDAY or TUESDAY or WEDNESDAY or THURSDAY or FRIDAY
        const val WEEKEND = SATURDAY or SUNDAY

        fun dayBitFor(calendar: Calendar = Calendar.getInstance()): Int = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> MONDAY
            Calendar.TUESDAY -> TUESDAY
            Calendar.WEDNESDAY -> WEDNESDAY
            Calendar.THURSDAY -> THURSDAY
            Calendar.FRIDAY -> FRIDAY
            Calendar.SATURDAY -> SATURDAY
            else -> SUNDAY
        }
    }
}
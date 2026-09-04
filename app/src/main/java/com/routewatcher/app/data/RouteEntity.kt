package com.routewatcher.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val activeDays: Int = 0b1111100,

    // Only true when waypoints are set in custom route mode
    val enabled: Boolean = false,

    val isCustomRoute: Boolean = false,
    // Locked-in route set once the user picks a specific route on the map
    // Falls back to  origin/destination routing if null.
    val lockedRoutePolyline: String? = null,
    val lockedRouteSummary: String? = null,
    val lockedRouteWaypoints: String? = null, // 3 pairs of (lat,lng)

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

    // TODO: AlarmScheduler should read offsetsList() + activeDays to compute each alarm's trigger time
}
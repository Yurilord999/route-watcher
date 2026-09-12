package com.routewatcher.app.viewmodel

import com.routewatcher.app.data.RouteEntity
import com.routewatcher.app.network.RouteOption
import com.routewatcher.app.network.RouteAlternative

// Add/edit form state, held in the ViewModel
data class RouteEditState(
    val id: Long = 0,
    val name: String = "",
    val origin: String = "",
    val destination: String = "",
    val hour: String = "8",
    val minute: String = "0",
    val offsets: String = "30",
    val threshold: String = "10",
    val activeDays: Int = RouteEntity.dayBitFor(),
    val enabled: Boolean = false,
    val isCustomRoute: Boolean = false,
    val lockedRoutePolyline: String? = null,
    val lockedRouteSummary: String? = null,
    val lockedRouteWaypoints: String? = null,
    val lockedRouteDurationMinutes: Int? = null,
    val lockedRouteDistanceText: String? = null,
    val originResolved: Boolean = false,
    val destinationResolved: Boolean = false,
    val alternatives: List<RouteAlternative> = emptyList(),
    val selectedAlternativeIndex: Int? = null,
) {
    val isNewRoute: Boolean get() = id == 0L
    val stopsCount: Int get() = if (isCustomRoute) decodeWaypoints(lockedRouteWaypoints).size else 0

    companion object {
        fun from(route: RouteEntity) = RouteEditState(
            id = route.id,
            name = route.name,
            origin = route.originAddress,
            destination = route.destinationAddress,
            hour = route.departureHour.toString(),
            minute = route.departureMinute.toString(),
            offsets = route.checkOffsetsMinutes,
            threshold = route.delayThresholdMinutes.toString(),
            activeDays = route.activeDays,
            enabled = route.enabled,
            isCustomRoute = route.isCustomRoute,
            lockedRoutePolyline = route.lockedRoutePolyline,
            lockedRouteSummary = route.lockedRouteSummary,
            lockedRouteWaypoints = route.lockedRouteWaypoints,
            lockedRouteDurationMinutes = route.lockedRouteDurationMinutes,
            lockedRouteDistanceText = route.lockedRouteDistanceText,
            originResolved = true,
            destinationResolved = true,
        )
    }
}

// Stops editor (while open)
data class StopsEditorState(
    val origin: String,
    val destination: String,
    val stops: List<Pair<Double, Double>> = emptyList(),
    val customRoute: RouteOption? = null,
    val isRecomputing: Boolean = false,
)

fun encodeWaypoints(waypoints: List<Pair<Double, Double>>): String =
    waypoints.joinToString(";") { "${it.first},${it.second}" }

// Turns saved waypoint string back into real coordinates
fun decodeWaypoints(raw: String?): List<Pair<Double, Double>> =
    raw
        ?.split(";")
        ?.mapNotNull { pair ->
            val parts = pair.split(",")
            val lat = parts.getOrNull(0)?.trim()?.toDoubleOrNull()
            val lng = parts.getOrNull(1)?.trim()?.toDoubleOrNull()
            if (lat != null && lng != null) lat to lng else null
        }
        ?: emptyList()
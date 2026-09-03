package com.routewatcher.app.viewmodel

import com.routewatcher.app.data.RouteEntity
import com.routewatcher.app.network.RouteOption

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
    val activeDays: Int = 0b1111100,
    val enabled: Boolean = false,
    val lockedRoutePolyline: String? = null,
    val lockedRouteSummary: String? = null,
    val lockedRouteWaypoints: String? = null,
) {
    val isNewRoute: Boolean get() = id == 0L

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
            lockedRoutePolyline = route.lockedRoutePolyline,
            lockedRouteSummary = route.lockedRouteSummary,
            lockedRouteWaypoints = route.lockedRouteWaypoints,
        )
    }
}

// State of the road picker screen (while open)
data class RoutePickerState(
    val origin: String,
    val destination: String,
    val routeOptions: List<RouteOption> = emptyList(),
    val isLoading: Boolean = true,
    val isCustomizing: Boolean = false,
    val stops: List<Pair<Double, Double>> = emptyList(),
    val customRoute: RouteOption? = null,
    val isRecomputing: Boolean = false,
)

fun encodeWaypoints(waypoints: List<Pair<Double, Double>>): String =
    waypoints.joinToString(";") { "${it.first},${it.second}" }
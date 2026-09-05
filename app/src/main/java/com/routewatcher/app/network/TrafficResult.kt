package com.routewatcher.app.network

import com.routewatcher.app.R

// Shared result shape for a single traffic check
data class TrafficResult(
    val success: Boolean,
    val normalDurationMinutes: Int = 0,
    val trafficDurationMinutes: Int = 0,
    val delayMinutes: Int = 0,
    val errorCode: TrafficErrorCode? = null,
)

// RoutesApiClient can't resolve strings.xml by itself. Returns this instead.
enum class TrafficErrorCode {
    NO_API_KEY,
    EMPTY_RESPONSE,
    NETWORK_ERROR,
    NO_ROUTE_RETURNED,
}

// Shared mapping so the wording lives in one place
// Whatever has context (NotificationHelper, SettingsScreen) resolves it to text via this
fun errorMessageRes(code: TrafficErrorCode?): Int = when (code) {
    TrafficErrorCode.NO_API_KEY -> R.string.error_no_api_key_set
    TrafficErrorCode.EMPTY_RESPONSE -> R.string.error_empty_response
    TrafficErrorCode.NETWORK_ERROR -> R.string.error_network
    TrafficErrorCode.NO_ROUTE_RETURNED -> R.string.error_no_route_returned
    null -> R.string.error_unknown
}
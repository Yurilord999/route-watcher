package com.routewatcher.app.network

// Shared result shape for a single traffic check
data class TrafficResult(
    val success: Boolean,
    val normalDurationMinutes: Int = 0,
    val trafficDurationMinutes: Int = 0,
    val delayMinutes: Int = 0,
    val errorMessage: String? = null,
)
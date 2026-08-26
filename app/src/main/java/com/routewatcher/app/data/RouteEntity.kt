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
    val enabled: Boolean = false,
) {
    fun offsetsList(): List<Int> =
        checkOffsetsMinutes.split(",").mapNotNull { it.trim().toIntOrNull() }

    // TODO: AlarmScheduler should read offsetsList() + activeDays to compute each alarm's trigger time
}
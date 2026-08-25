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
)
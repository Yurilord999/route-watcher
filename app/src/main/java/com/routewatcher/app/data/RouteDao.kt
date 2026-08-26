package com.routewatcher.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    // Live updating list, used to drive the route list screen directly
    @Query("SELECT * FROM routes ORDER BY name")
    fun observeAll(): Flow<List<RouteEntity>>

    // Insert if new, replace if the id already exists (covers both add and edit)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(route: RouteEntity): Long

    @Delete
    suspend fun delete(route: RouteEntity)

    // TODO: used by BootReceiver to reschedule alarms after reboot
    @Query("SELECT * FROM routes WHERE enabled = 1")
    suspend fun getAllEnabled(): List<RouteEntity>

    // TODO: used by TrafficCheckReceiver to look up the route which triggered an alarm
    @Query("SELECT * FROM routes WHERE id = :id")
    suspend fun getById(id: Long): RouteEntity?
}
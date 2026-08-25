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
}
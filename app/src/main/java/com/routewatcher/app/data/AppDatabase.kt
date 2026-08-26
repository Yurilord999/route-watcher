package com.routewatcher.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RouteEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun routeDao(): RouteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Shared database instance for the whole app process
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "routewatcher.db",
                )
                    // Pre-release schema change (added scheduling columns)
                    // Fine to recreate DB rather than write a real migration for now
                    // TODO: replace with real Migration objects please, can't be asked atm
                    // this wipes all saved routes on every future schema bump, which is fine
                    // now but would be a silent data loss bug later?
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
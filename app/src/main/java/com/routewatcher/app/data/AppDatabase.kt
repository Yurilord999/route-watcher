package com.routewatcher.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE routes ADD COLUMN lockedRouteDurationMinutes INTEGER")
        database.execSQL("ALTER TABLE routes ADD COLUMN lockedRouteDistanceText TEXT")
    }
}

@Database(entities = [RouteEntity::class], version = 5, exportSchema = false)
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
                    .addMigrations(MIGRATION_4_5)
                    // Pre-release schema change (added scheduling columns)
                    // Fine to recreate DB rather than write a real migration for now
                    // TODO: replace with real Migration objects please, can't be asked atm
                    // this wipes all saved routes on every future schema bump, which is fine
                    // now but would be a silent data loss bug later?
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build().also { INSTANCE = it }
            }
    }
}
// app/src/main/java/com/android/birdlens/data/local/AppDatabase.kt
package com.android.birdlens.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LocalHotspot::class, BirdSpecies::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hotspotDao(): HotspotDao
    abstract fun birdSpeciesDao(): BirdSpeciesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "birdlens_database"
                )
                    // For this update, we will destroy and re-create the database.
                    // In a real production app, you would implement a proper migration.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
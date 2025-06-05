package com.android.birdlens.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HotspotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(hotspots: List<LocalHotspot>)

    // Query hotspots within a certain lat/lng bounding box
    // This is a simple rectangular query. For more advanced spatial queries,
    // you might need to integrate a spatial extension or use more complex math.
    @Query("SELECT * FROM hotspots WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLng AND :maxLng")
    suspend fun getHotspotsInRegion(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): List<LocalHotspot>

    // Get hotspots by specific IDs
    @Query("SELECT * FROM hotspots WHERE loc_id IN (:locIds)")
    suspend fun getHotspotsByIds(locIds: List<String>): List<LocalHotspot>

    // Clear old hotspots (e.g., older than a certain timestamp)
    @Query("DELETE FROM hotspots WHERE last_updated_timestamp < :timestamp")
    suspend fun clearOldHotspots(timestamp: Long)

    @Query("DELETE FROM hotspots")
    suspend fun clearAll()

    @Query("SELECT * FROM hotspots")
    suspend fun getAllHotspots(): List<LocalHotspot>
}
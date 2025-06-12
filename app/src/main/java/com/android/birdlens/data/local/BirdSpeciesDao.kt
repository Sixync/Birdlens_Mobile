// app/src/main/java/com/android/birdlens/data/local/BirdSpeciesDao.kt
package com.android.birdlens.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BirdSpeciesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(species: List<BirdSpecies>)

    @Query("SELECT COUNT(*) FROM bird_species")
    suspend fun getSpeciesCount(): Int

    /**
     * Searches for birds by common or scientific name, case-insensitively.
     * Returns a limited list of matches.
     */
    @Query("SELECT * FROM bird_species WHERE commonName LIKE '%' || :query || '%' OR scientificName LIKE '%' || :query || '%' LIMIT 10")
    suspend fun searchByName(query: String): List<BirdSpecies>
}
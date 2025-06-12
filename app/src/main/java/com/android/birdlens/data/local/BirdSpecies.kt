// app/src/main/java/com/android/birdlens/data/local/BirdSpecies.kt
package com.android.birdlens.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bird_species")
data class BirdSpecies(
    @PrimaryKey val speciesCode: String,
    val commonName: String,
    val scientificName: String
)
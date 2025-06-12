// app/src/main/java/com/android/birdlens/data/repository/BirdSpeciesRepository.kt
package com.android.birdlens.data.repository

import android.content.Context
import android.util.Log
import com.android.birdlens.data.local.AppDatabase
import com.android.birdlens.data.local.BirdSpecies
import com.android.birdlens.data.model.ebird.EbirdRetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BirdSpeciesRepository(context: Context) {

    private val birdSpeciesDao = AppDatabase.getDatabase(context).birdSpeciesDao()
    private val ebirdApiService = EbirdRetrofitInstance.api

    companion object {
        private const val TAG = "BirdSpeciesRepository"
    }

    /**
     * Checks if the database is empty and, if so, fetches the entire eBird
     * species taxonomy and populates the local database.
     */
    suspend fun populateDatabaseIfEmpty() {
        withContext(Dispatchers.IO) {
            val count = birdSpeciesDao.getSpeciesCount()
            if (count == 0) {
                Log.i(TAG, "Bird species database is empty. Populating from eBird API...")
                try {
                    // Call API without species code to get the full taxonomy for the 'species' category
                    val response = ebirdApiService.getSpeciesTaxonomy(category = "species")
                    if (response.isSuccessful && response.body() != null) {
                        val taxonomyList = response.body()!!
                        val birdSpeciesList = taxonomyList
                            .filter { it.speciesCode.isNotBlank() && it.commonName.isNotBlank() } // Ensure data integrity
                            .map {
                                BirdSpecies(
                                    speciesCode = it.speciesCode,
                                    commonName = it.commonName,
                                    scientificName = it.scientificName
                                )
                            }
                        birdSpeciesDao.insertAll(birdSpeciesList)
                        Log.i(TAG, "Successfully populated database with ${birdSpeciesList.size} bird species.")
                    } else {
                        Log.e(TAG, "Failed to fetch eBird taxonomy: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception while populating bird species database", e)
                }
            } else {
                Log.d(TAG, "Bird species database is already populated with $count species.")
            }
        }
    }

    /**
     * Searches the local database for bird species matching the query.
     */
    suspend fun searchBirds(query: String): List<BirdSpecies> {
        return withContext(Dispatchers.IO) {
            birdSpeciesDao.searchByName(query)
        }
    }
}
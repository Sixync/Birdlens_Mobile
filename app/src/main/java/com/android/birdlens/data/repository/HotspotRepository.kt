// EXE201/app/src/main/java/com/android/birdlens/data/repository/HotspotRepository.kt
package com.android.birdlens.data.repository

import android.content.Context
import android.util.Log
import com.android.birdlens.data.local.AppDatabase
import com.android.birdlens.data.local.toEbirdNearbyHotspot
import com.android.birdlens.data.local.toLocalHotspot
import com.android.birdlens.data.model.ebird.EbirdNearbyHotspot
import com.android.birdlens.data.model.ebird.EbirdRetrofitInstance
import com.android.birdlens.utils.ErrorUtils
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.cos

class HotspotRepository(applicationContext: Context) {

    private val hotspotDao = AppDatabase.getDatabase(applicationContext).hotspotDao()
    private val ebirdApiService = EbirdRetrofitInstance.api

    private val CACHE_EXPIRY_MS = TimeUnit.DAYS.toMillis(1)
    companion object {
        private const val TAG = "HotspotRepository"
    }

    suspend fun getNearbyHotspots(
        center: LatLng,
        radiusKm: Int,
        currentBounds: LatLngBounds?,
        countryCodeFilter: String? = null,
        forceRefresh: Boolean = false
    ): List<EbirdNearbyHotspot> {
        return withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            val cacheExpiryTimestamp = currentTime - CACHE_EXPIRY_MS

            if (!forceRefresh && currentBounds != null) {
                val cachedHotspots = hotspotDao.getHotspotsInRegion(
                    minLat = currentBounds.southwest.latitude,
                    maxLat = currentBounds.northeast.latitude,
                    minLng = currentBounds.southwest.longitude,
                    maxLng = currentBounds.northeast.longitude
                ).filter { it.lastUpdatedTimestamp > cacheExpiryTimestamp }

                Log.d(TAG, "Found ${cachedHotspots.size} fresh hotspots in cache for current view bounds.")

                if (cachedHotspots.isNotEmpty()) {
                    val result = cachedHotspots.map { it.toEbirdNearbyHotspot() }
                    return@withContext if (countryCodeFilter != null) {
                        result.filter { it.countryCode == countryCodeFilter }
                    } else {
                        result
                    }
                }
                Log.d(TAG, "Cache miss or insufficient for current view. Will attempt network fetch if radius search differs.")
            }

            Log.d(TAG, "Fetching from network: lat=${center.latitude}, lng=${center.longitude}, dist=$radiusKm, forceRefresh=$forceRefresh")
            try {
                val response = ebirdApiService.getNearbyHotspots(
                    lat = center.latitude,
                    lng = center.longitude,
                    dist = radiusKm
                )

                if (response.isSuccessful && response.body() != null) {
                    val networkHotspots = response.body()!!
                    Log.d(TAG, "Network fetch successful: ${networkHotspots.size} hotspots.")

                    val localHotspotsToSave = networkHotspots.map { it.toLocalHotspot(System.currentTimeMillis()) }
                    hotspotDao.insertAll(localHotspotsToSave)
                    Log.d(TAG, "Saved ${localHotspotsToSave.size} hotspots to cache.")

                    return@withContext if (countryCodeFilter != null) {
                        networkHotspots.filter { it.countryCode == countryCodeFilter }
                    } else {
                        networkHotspots
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val extractedMessage = ErrorUtils.extractMessage(errorBody, "eBird API error ${response.code()}")
                    Log.e(TAG, "Network error fetching hotspots: ${response.code()} - ${response.message()}. Parsed: $extractedMessage. Full Body: $errorBody")
                    throw IOException(extractedMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching hotspots from network: ${e.localizedMessage}", e)
                if (e is IOException) throw e // Re-throw IOExceptions (including our custom ones)
                throw IOException("Failed to fetch hotspots: ${e.localizedMessage}", e) // Wrap other exceptions
            }
        }
    }

    suspend fun findSpeciesCode(birdName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val response = ebirdApiService.getSpeciesTaxonomy(speciesCodes = birdName, category = "species")
                if (response.isSuccessful && response.body()?.isNotEmpty() == true) {
                    val speciesCode = response.body()!!.first().speciesCode
                    Log.d(TAG, "Found species code '$speciesCode' for name '$birdName'")
                    speciesCode
                } else {
                    Log.w(TAG, "Could not find species code for '$birdName'. Response: ${response.code()}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception finding species code for '$birdName'", e)
                null
            }
        }
    }

    suspend fun getHotspotsForSpeciesInCountry(speciesCode: String, countryCode: String): List<EbirdNearbyHotspot> {
        return withContext(Dispatchers.IO) {
            try {
                val response = ebirdApiService.getRecentObservationsOfSpeciesInRegion(
                    regionCode = countryCode,
                    speciesCode = speciesCode,
                )

                if (response.isSuccessful && response.body() != null) {
                    val observations = response.body()!!
                    Log.d(TAG, "Found ${observations.size} observations for species '$speciesCode' in '$countryCode'.")
                    val hotspots = observations
                        .distinctBy { it.locId }
                        .map { obs ->
                            EbirdNearbyHotspot(
                                locId = obs.locId,
                                locName = obs.locName,
                                countryCode = countryCode,
                                subnational1Code = null,
                                subnational2Code = null,
                                lat = obs.lat,
                                lng = obs.lng,
                                latestObsDt = obs.obsDt,
                                numSpeciesAllTime = null
                            )
                        }
                    Log.d(TAG, "Created ${hotspots.size} unique hotspots for species '$speciesCode'.")
                    hotspots
                } else {
                    val errorBody = response.errorBody()?.string()
                    val extractedMessage = ErrorUtils.extractMessage(errorBody, "eBird API error ${response.code()}")
                    Log.e(TAG, "Network error fetching species observations: ${response.code()} - ${response.message()}. Parsed: $extractedMessage. Full Body: $errorBody")
                    throw IOException(extractedMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching species observations for '$speciesCode' in '$countryCode': ${e.localizedMessage}", e)
                if (e is IOException) throw e
                throw IOException("Failed to fetch species observations: ${e.localizedMessage}", e)
            }
        }
    }

    fun calculateBounds(center: LatLng, radiusKm: Double): LatLngBounds {
        val earthRadiusKm = 6371.0
        val latChange = Math.toDegrees(radiusKm / earthRadiusKm)
        val lngChange = Math.toDegrees(radiusKm / earthRadiusKm / kotlin.math.cos(Math.toRadians(center.latitude)))

        val southwest = LatLng(center.latitude - latChange, center.longitude - lngChange)
        val northeast = LatLng(center.latitude + latChange, center.longitude + lngChange)
        return LatLngBounds(southwest, northeast)
    }

    suspend fun clearAllHotspotsCache() {
        withContext(Dispatchers.IO) {
            hotspotDao.clearAll()
            Log.i(TAG, "Cleared all hotspot cache.")
        }
    }
}
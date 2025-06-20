// EXE201/app/src/main/java/com/android/birdlens/data/repository/HotspotRepository.kt
package com.android.birdlens.data.repository

import android.content.Context
import android.util.Log
import com.android.birdlens.data.local.AppDatabase
import com.android.birdlens.data.local.toEbirdNearbyHotspot
import com.android.birdlens.data.local.toLocalHotspot
import com.android.birdlens.data.model.VisitingTimesAnalysis
import com.android.birdlens.data.model.ebird.EbirdNearbyHotspot
import com.android.birdlens.data.model.ebird.EbirdRetrofitInstance
import com.android.birdlens.data.model.response.GenericApiResponse
import com.android.birdlens.data.network.RetrofitInstance
import com.android.birdlens.utils.ErrorUtils
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive // Import for isActive check
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.cos // Ensure this specific import if not already present

class HotspotRepository(private val applicationContext: Context) {

    val hotspotDao = AppDatabase.getDatabase(applicationContext).hotspotDao()
    private val ebirdApiService = EbirdRetrofitInstance.api

    private val CACHE_EXPIRY_MS = TimeUnit.DAYS.toMillis(1)
    companion object {
        private const val TAG = "HotspotRepository"
    }

    fun getEbirdApiService() = ebirdApiService

    // Logic: Add a new function to get details for a single hotspot, checking cache first.
    suspend fun getHotspotDetails(locId: String): EbirdNearbyHotspot? {
        return withContext(Dispatchers.IO) {
            val cached = hotspotDao.getHotspotsByIds(listOf(locId)).firstOrNull()
            if (cached != null) {
                Log.d(TAG, "Found hotspot details for $locId in cache.")
                return@withContext cached.toEbirdNearbyHotspot()
            }

            Log.d(TAG, "Hotspot details for $locId not in cache. Fetching from network.")
            try {
                val response = ebirdApiService.getHotspotInfo(locId)
                if (response.isSuccessful) {
                    response.body()
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch hotspot info for $locId from network", e)
                null
            }
        }
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

            if (!forceRefresh) {
                val minLat = currentBounds?.southwest?.latitude ?: (center.latitude - (radiusKm / 111.0))
                val maxLat = currentBounds?.northeast?.latitude ?: (center.latitude + (radiusKm / 111.0))
                val lngRadiusDegrees = radiusKm / (111.0 * cos(Math.toRadians(center.latitude)))
                val minLng = currentBounds?.southwest?.longitude ?: (center.longitude - lngRadiusDegrees)
                val maxLng = currentBounds?.northeast?.longitude ?: (center.longitude + lngRadiusDegrees)

                val cachedHotspots = hotspotDao.getHotspotsInRegion(minLat, maxLat, minLng, maxLng)
                    .filter { it.lastUpdatedTimestamp > cacheExpiryTimestamp }

                Log.d(TAG, "Found ${cachedHotspots.size} fresh hotspots in cache for region.")
                if (cachedHotspots.isNotEmpty()) {
                    val result = cachedHotspots.map { it.toEbirdNearbyHotspot() }
                    return@withContext if (countryCodeFilter != null) {
                        result.filter { it.countryCode == countryCodeFilter }
                    } else {
                        result
                    }
                }
                Log.d(TAG, "Cache miss or insufficient. Will attempt network fetch.")
            } else {
                Log.d(TAG, "Force refresh true. Skipping cache check for nearby hotspots.")
            }

            Log.d(TAG, "Fetching from network: lat=${center.latitude}, lng=${center.longitude}, dist=$radiusKm, forceRefresh=$forceRefresh")
            try {
                if (!kotlin.coroutines.coroutineContext.isActive) {
                    Log.i(TAG, "Coroutine cancelled before network call in getNearbyHotspots.")
                    throw CancellationException("Coroutine cancelled before network call in repository's getNearbyHotspots")
                }

                val response = ebirdApiService.getNearbyHotspots(
                    lat = center.latitude,
                    lng = center.longitude,
                    dist = radiusKm
                )

                if (!kotlin.coroutines.coroutineContext.isActive) {
                    Log.i(TAG, "Coroutine cancelled during/after network call in getNearbyHotspots.")
                    throw CancellationException("Coroutine cancelled during/after network call in repository's getNearbyHotspots")
                }

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
                if (e is CancellationException) {
                    Log.i(TAG, "Network fetch in getNearbyHotspots repository cancelled: ${e.message}")
                    throw e
                }
                val errorMessage = "Failed to fetch hotspots: ${e.localizedMessage ?: "Unknown network error"}"
                Log.e(TAG, "Exception fetching hotspots from network: $errorMessage", e)
                throw IOException(errorMessage, e)
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
                if (e is CancellationException) throw e
                Log.e(TAG, "Exception finding species code for '$birdName'", e)
                null
            }
        }
    }

    suspend fun getHotspotsForSpeciesInCountry(speciesCode: String, countryCode: String): List<EbirdNearbyHotspot> {
        return withContext(Dispatchers.IO) {
            try {
                if (!kotlin.coroutines.coroutineContext.isActive) throw CancellationException("Coroutine cancelled before network call in getHotspotsForSpeciesInCountry")
                val response = ebirdApiService.getRecentObservationsOfSpeciesInRegion(
                    regionCode = countryCode,
                    speciesCode = speciesCode,
                )
                if (!kotlin.coroutines.coroutineContext.isActive) throw CancellationException("Coroutine cancelled during/after network call in getHotspotsForSpeciesInCountry")

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
                if (e is CancellationException) {
                    Log.i(TAG, "Species hotspot fetch in repository cancelled: ${e.message}")
                    throw e
                }
                val errorMessage = "Failed to fetch species observations: ${e.localizedMessage ?: "Unknown network error"}"
                Log.e(TAG, "Exception fetching species observations for '$speciesCode' in '$countryCode': $errorMessage", e)
                throw IOException(errorMessage, e)
            }
        }
    }


    fun calculateBounds(center: LatLng, radiusKm: Double): LatLngBounds {
        val earthRadiusKm = 6371.0
        val latChange = Math.toDegrees(radiusKm / earthRadiusKm)
        val lngChange = Math.toDegrees(radiusKm / earthRadiusKm / cos(Math.toRadians(center.latitude)))

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

    suspend fun getVisitingTimesAnalysis(locId: String, speciesCode: String?): Response<GenericApiResponse<VisitingTimesAnalysis>> {
        val apiService = RetrofitInstance.getApiService(applicationContext)
        return apiService.getHotspotVisitingTimes(locId, speciesCode)
    }
}
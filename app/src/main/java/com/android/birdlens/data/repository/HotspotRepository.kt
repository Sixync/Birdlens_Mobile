// EXE201/app/src/main/java/com/android/birdlens/data/repository/HotspotRepository.kt
package com.android.birdlens.data.repository

import android.content.Context
import android.util.Log
import com.android.birdlens.data.local.AppDatabase
import com.android.birdlens.data.local.LocalHotspot
import com.android.birdlens.data.local.toEbirdNearbyHotspot
import com.android.birdlens.data.local.toLocalHotspot
import com.android.birdlens.data.model.ebird.EbirdNearbyHotspot
import com.android.birdlens.data.model.ebird.EbirdRetrofitInstance
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln

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
        currentBounds: LatLngBounds?, // Added to check if existing cache covers the new view
        countryCodeFilter: String? = null,
        forceRefresh: Boolean = false
    ): List<EbirdNearbyHotspot> {
        return withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            val cacheExpiryTimestamp = currentTime - CACHE_EXPIRY_MS

            if (!forceRefresh && currentBounds != null) {
                // Try to fetch from local cache first, ensuring the current view is somewhat covered
                // This is a simplified check; more sophisticated would be to check if 'currentBounds'
                // is mostly contained within a region for which we have fresh cache.
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


            // If forceRefresh, or cache didn't satisfy the current view, proceed to fetch by radius from network
            Log.d(TAG, "Fetching from network: lat=${center.latitude}, lng=${center.longitude}, dist=$radiusKm, forceRefresh=$forceRefresh")
            try {
                val response = ebirdApiService.getNearbyHotspots(
                    lat = center.latitude,
                    lng = center.longitude,
                    dist = radiusKm // eBird API takes distance in km
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
                    Log.e(TAG, "Network error fetching hotspots: ${response.code()} - ${response.message()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching hotspots from network: ${e.localizedMessage}", e)
                emptyList()
            }
        }
    }

    // Keep calculateBounds if used by ViewModel directly, or remove if ViewModel passes LatLngBounds
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
}
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

class HotspotRepository(applicationContext: Context) {

    private val hotspotDao = AppDatabase.getDatabase(applicationContext).hotspotDao()
    private val ebirdApiService = EbirdRetrofitInstance.api

    // Cache duration: 1 day
    private val CACHE_EXPIRY_MS = TimeUnit.DAYS.toMillis(1)
    companion object {
        private const val TAG = "HotspotRepository"
    }


    suspend fun getNearbyHotspots(
        center: LatLng,
        radiusKm: Int,
        countryCodeFilter: String? = null
    ): List<EbirdNearbyHotspot> {
        return withContext(Dispatchers.IO) {
            // 1. Define search region based on center and radius (approximate bounding box)
            val searchBounds = calculateBounds(center, radiusKm.toDouble())
            val currentTime = System.currentTimeMillis()
            val cacheExpiryTimestamp = currentTime - CACHE_EXPIRY_MS

            // 2. Try to fetch from local cache first
            val cachedHotspots = hotspotDao.getHotspotsInRegion(
                minLat = searchBounds.southwest.latitude,
                maxLat = searchBounds.northeast.latitude,
                minLng = searchBounds.southwest.longitude,
                maxLng = searchBounds.northeast.longitude
            ).filter { it.lastUpdatedTimestamp > cacheExpiryTimestamp }

            Log.d(TAG, "Found ${cachedHotspots.size} fresh hotspots in cache for region.")

            if (cachedHotspots.isNotEmpty()) {
                // Basic filtering by country code if provided, can be more sophisticated
                val result = cachedHotspots.map { it.toEbirdNearbyHotspot() }
                return@withContext if (countryCodeFilter != null) {
                    result.filter { it.countryCode == countryCodeFilter }
                } else {
                    result
                }
            }

            // 3. If cache is empty or stale for the region, fetch from network
            Log.d(TAG, "Cache miss or stale for region. Fetching from network: lat=${center.latitude}, lng=${center.longitude}, dist=$radiusKm")
            try {
                val response = ebirdApiService.getNearbyHotspots(
                    lat = center.latitude,
                    lng = center.longitude,
                    dist = radiusKm
                )

                if (response.isSuccessful && response.body() != null) {
                    val networkHotspots = response.body()!!
                    Log.d(TAG, "Network fetch successful: ${networkHotspots.size} hotspots.")
                    // Save to cache
                    val localHotspotsToSave = networkHotspots.map { it.toLocalHotspot(System.currentTimeMillis()) }
                    hotspotDao.insertAll(localHotspotsToSave)
                    Log.d(TAG, "Saved ${localHotspotsToSave.size} hotspots to cache.")

                    // Clean up old cache entries (optional, can be done periodically)
                    // hotspotDao.clearOldHotspots(cacheExpiryTimestamp)

                    return@withContext if (countryCodeFilter != null) {
                        networkHotspots.filter { it.countryCode == countryCodeFilter }
                    } else {
                        networkHotspots
                    }
                } else {
                    Log.e(TAG, "Network error fetching hotspots: ${response.code()} - ${response.message()}")
                    emptyList() // Or throw an exception
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching hotspots from network: ${e.localizedMessage}", e)
                emptyList() // Or throw an exception
            }
        }
    }

    // Simple approximation for bounding box calculation
    private fun calculateBounds(center: LatLng, radiusKm: Double): LatLngBounds {
        val earthRadiusKm = 6371.0
        val latChange = Math.toDegrees(radiusKm / earthRadiusKm)
        val lngChange = Math.toDegrees(radiusKm / earthRadiusKm / Math.cos(Math.toRadians(center.latitude)))

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
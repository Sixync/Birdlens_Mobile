// EXE201/app/src/main/java/com/android/birdlens/data/model/ebird/EbirdApiService.kt
package com.android.birdlens.data.model.ebird

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class EbirdFindResult(
    val code: String,
    val name: String,
    val sciName: String
)

interface EbirdApiService {
    /**
     * Fetches recent notable observations for a given region. This is ideal for an "Explore" feed.
     * API Key is added via EbirdApiKeyInterceptor.
     * Example: https://api.ebird.org/v2/data/obs/US/recent/notable
     */
    @GET("v2/data/obs/{regionCode}/recent/notable")
    suspend fun getNotableObservationsInRegion(
        @Path("regionCode") regionCode: String,
        @Query("maxResults") maxResults: Int = 10,
        @Query("detail") detail: String = "simple"
    ): Response<List<EbirdObservation>>

    /**
     * Fetches the eBird Taxonomy for given species codes.
     * API Key is added via EbirdApiKeyInterceptor.
     * Example: https://api.ebird.org/v2/ref/taxonomy/ebird?species=houspa,amerob&fmt=json
     * @param speciesCodes A comma-separated list of eBird species codes (e.g., "houspa,amerob"). If null, returns entire taxonomy.
     * @param fmt Format (json or csv).
     * @param locale Locale for common names (e.g., "en", "es_ES").
     * @param cat Category of taxa to return (e.g., "species", "issf", "hybrid").
     * @param version Taxonomy version year (e.g., "current" or "YYYY").
     */
    @GET("v2/ref/taxonomy/ebird")
    suspend fun getSpeciesTaxonomy(
        @Query("species") speciesCodes: String? = null, // Changed to nullable with default
        @Query("fmt") format: String = "json",
        @Query("locale") locale: String? = null,
        @Query("cat") category: String? = null,
        @Query("version") version: String? = null
    ): Response<List<EbirdTaxonomy>>
    @GET("v2/ref/hotspot/info/{locId}")
    suspend fun getHotspotInfo(@Path("locId") locId: String): Response<EbirdNearbyHotspot>
    /**
     * Fetches hotspots near a given geographic point.
     * API Key is added via EbirdApiKeyInterceptor.
     * Example: https://api.ebird.org/v2/ref/hotspot/geo?lat=20.3&lng=105.6&dist=10&fmt=json
     */
    @GET("v2/ref/hotspot/geo")
    suspend fun getNearbyHotspots(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("dist") dist: Int = 25,
        @Query("back") back: Int? = null,
        @Query("fmt") format: String = "json"
    ): Response<List<EbirdNearbyHotspot>>

    /**
     * Fetches recent bird observations for a specific hotspot.
     * API Key is added via EbirdApiKeyInterceptor.
     * Example: https://api.ebird.org/v2/product/obs/L109516/recent?back=10&fmt=json
     */
    @GET("v2/data/obs/{locId}/recent")
    suspend fun getRecentObservationsForHotspot(
        @Path("locId") locId: String,
        @Query("back") back: Int = 30,
        @Query("maxResults") maxResults: Int? = null,
        @Query("detail") detail: String = "simple",
        @Query("hotspot") hotspot: Boolean = true,
        @Query("includeProvisional") includeProvisional: Boolean = false,
        @Query("fmt") format: String = "json"
    ): Response<List<EbirdObservation>>

    /**
     * Fetches the list of all species codes recorded at a specific hotspot.
     * API Key is added via EbirdApiKeyInterceptor.
     * Example: https://api.ebird.org/v2/product/spplist/L109516
     * @param locId The eBird location ID (locId) of the hotspot.
     */
    @GET("v2/product/spplist/{locId}")
    suspend fun getSpeciesListForHotspot(
        @Path("locId") locId: String
    ): Response<List<String>> // Returns a simple list of species codes

    /**
     * Fetches recent observations of a specific species within a given region (e.g., country).
     * API Key is added via EbirdApiKeyInterceptor.
     * Example: https://api.ebird.org/v2/data/obs/VN/recent/houspa
     */
    @GET("v2/data/obs/{regionCode}/recent/{speciesCode}")
    suspend fun getRecentObservationsOfSpeciesInRegion(
        @Path("regionCode") regionCode: String,
        @Path("speciesCode") speciesCode: String,
        @Query("back") back: Int = 30, // Days back
        @Query("hotspot") hotspot: Boolean = true, // Only get observations from hotspots
        @Query("maxResults") maxResults: Int = 1000, // Limit results
        @Query("fmt") format: String = "json"
    ): Response<List<EbirdObservation>>
}
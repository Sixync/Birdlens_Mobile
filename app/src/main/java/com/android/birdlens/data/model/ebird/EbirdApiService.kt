// Birdlens_Mobile/app/src/main/java/com/android/birdlens/data/model/ebird/EbirdApiService.kt
package com.android.birdlens.data.model.ebird

// ...
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface EbirdApiService {
    /**
     * Fetches the eBird Taxonomy for a given species code.
     * API Key is added via EbirdApiKeyInterceptor.
     * Example: https://api.ebird.org/v2/ref/taxonomy/ebird?species=houspa&fmt=json
     * @param speciesCode The eBird species code (e.g., "houspa" for House Sparrow).
     * @param fmt Format (json or csv).
     * @param locale Locale for common names (e.g., "en", "es_ES").
     * @param cat Category of taxa to return (e.g., "species", "issf", "hybrid").
     * @param version Taxonomy version year (e.g., "current" or "YYYY").
     */
    @GET("v2/ref/taxonomy/ebird")
    suspend fun getSpeciesTaxonomy(
        @Query("species") speciesCode: String,
        @Query("fmt") format: String = "json",
        @Query("locale") locale: String? = null, // Optional: for localized common names
        @Query("cat") category: String? = null, // Optional: filter by category
        @Query("version") version: String? = null // Optional: specify taxonomy version
    ): Response<List<EbirdTaxonomy>> // eBird taxonomy can return an array even for a single species

    /**
     * Fetches hotspots near a given geographic point.
     * API Key is added via EbirdApiKeyInterceptor.
     * Example: https://api.ebird.org/v2/ref/hotspot/geo?lat=20.3&lng=105.6&dist=10&fmt=json
     * @param lat Latitude
     * @param lng Longitude
     * @param dist Distance in kilometers (default 1 to 50)
     * @param back Number of days back to look for observations (default 1 to 30)
     * @param fmt Format (json or csv)
     */
    @GET("v2/ref/hotspot/geo")
    suspend fun getNearbyHotspots(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("dist") dist: Int = 25, // Search radius in km
        @Query("back") back: Int? = null, // Days back for recent observations filter
        @Query("fmt") format: String = "json"
    ): Response<List<EbirdNearbyHotspot>>

    /**
     * Fetches recent bird observations for a specific hotspot.
     * API Key is added via EbirdApiKeyInterceptor.
     * Example: https://api.ebird.org/v2/product/obs/L109516/recent?back=10&fmt=json
     * @param locId The eBird location ID (locId) of the hotspot.
     * @param back Number of days back to retrieve observations (1-30, default 30).
     * @param maxResults Maximum number of results to return.
     * @param detail Level of detail for observations (simple or full, default simple).
     * @param hotspot Only return observations from hotspot locations (true or false, default false).
     *                This might seem redundant but can be useful if the locId is not strictly a hotspot.
     * @param includeProvisional Include provisional observations (true or false, default false).
     */
    @GET("v2/product/obs/{locId}/recent")
    suspend fun getRecentObservationsForHotspot(
        @Path("locId") locId: String,
        @Query("back") back: Int = 30,
        @Query("maxResults") maxResults: Int? = null, // eBird default is all for the period
        @Query("detail") detail: String = "simple",
        @Query("hotspot") hotspot: Boolean = true, // Ensure observations are from hotspot
        @Query("includeProvisional") includeProvisional: Boolean = false,
        @Query("fmt") format: String = "json"
    ): Response<List<EbirdObservation>>
}
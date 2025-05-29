package com.android.birdlens.data.model.ebird

import com.android.birdlens.data.model.ebird.EbirdTaxonomy
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface EbirdApiService {
    /**
     * Fetches taxonomic information for a given species.
     * API Key is added via EbirdApiKeyInterceptor.
     * Example: https://api.ebird.org/v2/ref/taxonomy/ebird?species=houspa&fmt=json
     */
    @GET("v2/ref/taxonomy/ebird")
    suspend fun getSpeciesTaxonomy(
        @Query("species") speciesCode: String,
        @Query("fmt") format: String = "json" // eBird API typically returns JSON by default for this
    ): Response<List<EbirdTaxonomy>> // The API returns a list, usually with one item for a specific species code
}
package com.android.birdlens.data.model.wiki

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WikiApiService {
    /**
     * Fetches page image from Wikipedia.
     * Example: https://en.wikipedia.org/w/api.php?action=query&titles=House%20Sparrow&prop=pageimages&format=json&pithumbsize=500
     */
    @GET("w/api.php")
    suspend fun getPageImage(
        @Query("action") action: String = "query",
        @Query("titles") titles: String,
        @Query("prop") prop: String = "pageimages",
        @Query("format") format: String = "json",
        @Query("pithumbsize") pithumbsize: Int = 500, // Preferred thumbnail size
        @Query("redirects") redirects: Int = 1 // Follow redirects
    ): Response<WikiQueryResponse>
}
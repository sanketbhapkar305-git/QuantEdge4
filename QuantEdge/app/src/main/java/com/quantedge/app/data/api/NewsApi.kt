package com.quantedge.app.data.api

import com.quantedge.app.data.model.GNewsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApi {

    /**
     * GNews API — free tier allows 100 requests/day
     * API key loaded from environment (not hardcoded)
     */
    @GET("v4/search")
    suspend fun getStockNews(
        @Query("q") query: String,
        @Query("lang") lang: String = "en",
        @Query("country") country: String = "in",
        @Query("max") max: Int = 10,
        @Query("apikey") apiKey: String
    ): Response<GNewsResponse>

    @GET("v4/top-headlines")
    suspend fun getMarketNews(
        @Query("topic") topic: String = "business",
        @Query("lang") lang: String = "en",
        @Query("country") country: String = "in",
        @Query("max") max: Int = 20,
        @Query("apikey") apiKey: String
    ): Response<GNewsResponse>
}

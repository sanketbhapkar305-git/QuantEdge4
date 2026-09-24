package com.quantedge.app.data.api

import com.quantedge.app.data.model.YahooChartResponse
import com.quantedge.app.data.model.YahooQuoteSummaryResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface YahooFinanceApi {

    /**
     * Get real-time chart data for a stock
     * For BSE stocks: symbol = "RELIANCE.BO"
     * For NSE stocks: symbol = "RELIANCE.NS"
     */
    @GET("v8/finance/chart/{symbol}")
    suspend fun getChart(
        @Path("symbol") symbol: String,
        @Query("interval") interval: String = "1d",
        @Query("range") range: String = "5y",
        @Query("includePrePost") includePrePost: Boolean = false,
        @Query("events") events: String = "div,splits"
    ): Response<YahooChartResponse>

    @GET("v8/finance/chart/{symbol}")
    suspend fun getIntradayChart(
        @Path("symbol") symbol: String,
        @Query("interval") interval: String = "5m",
        @Query("range") range: String = "1d"
    ): Response<YahooChartResponse>

    @GET("v10/finance/quoteSummary/{symbol}")
    suspend fun getQuoteSummary(
        @Path("symbol") symbol: String,
        @Query("modules") modules: String = "summaryDetail,defaultKeyStatistics,financialData,price,assetProfile"
    ): Response<YahooQuoteSummaryResponse>
}

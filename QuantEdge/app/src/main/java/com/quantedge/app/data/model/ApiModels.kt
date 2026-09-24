package com.quantedge.app.data.model

import com.google.gson.annotations.SerializedName

// Yahoo Finance Chart API Response
data class YahooChartResponse(
    @SerializedName("chart") val chart: YahooChart?
)

data class YahooChart(
    @SerializedName("result") val result: List<YahooChartResult>?,
    @SerializedName("error") val error: YahooError?
)

data class YahooChartResult(
    @SerializedName("meta") val meta: YahooMeta?,
    @SerializedName("timestamp") val timestamp: List<Long>?,
    @SerializedName("indicators") val indicators: YahooIndicators?
)

data class YahooMeta(
    @SerializedName("currency") val currency: String?,
    @SerializedName("symbol") val symbol: String?,
    @SerializedName("exchangeName") val exchangeName: String?,
    @SerializedName("regularMarketPrice") val regularMarketPrice: Double?,
    @SerializedName("previousClose") val previousClose: Double?,
    @SerializedName("regularMarketVolume") val regularMarketVolume: Long?,
    @SerializedName("regularMarketDayHigh") val regularMarketDayHigh: Double?,
    @SerializedName("regularMarketDayLow") val regularMarketDayLow: Double?,
    @SerializedName("regularMarketOpen") val regularMarketOpen: Double?,
    @SerializedName("fiftyTwoWeekHigh") val fiftyTwoWeekHigh: Double?,
    @SerializedName("fiftyTwoWeekLow") val fiftyTwoWeekLow: Double?,
    @SerializedName("regularMarketTime") val regularMarketTime: Long?,
    @SerializedName("chartPreviousClose") val chartPreviousClose: Double?
)

data class YahooIndicators(
    @SerializedName("quote") val quote: List<YahooQuote>?,
    @SerializedName("adjclose") val adjclose: List<YahooAdjClose>?
)

data class YahooQuote(
    @SerializedName("open") val open: List<Double?>?,
    @SerializedName("high") val high: List<Double?>?,
    @SerializedName("low") val low: List<Double?>?,
    @SerializedName("close") val close: List<Double?>?,
    @SerializedName("volume") val volume: List<Long?>?
)

data class YahooAdjClose(
    @SerializedName("adjclose") val adjclose: List<Double?>?
)

// Yahoo Finance Quote Summary API Response
data class YahooQuoteSummaryResponse(
    @SerializedName("quoteSummary") val quoteSummary: YahooQuoteSummary?
)

data class YahooQuoteSummary(
    @SerializedName("result") val result: List<YahooQuoteSummaryResult>?,
    @SerializedName("error") val error: YahooError?
)

data class YahooQuoteSummaryResult(
    @SerializedName("summaryDetail") val summaryDetail: YahooSummaryDetail?,
    @SerializedName("defaultKeyStatistics") val defaultKeyStatistics: YahooKeyStatistics?,
    @SerializedName("financialData") val financialData: YahooFinancialData?,
    @SerializedName("price") val price: YahooPrice?,
    @SerializedName("assetProfile") val assetProfile: YahooAssetProfile?
)

data class YahooSummaryDetail(
    @SerializedName("trailingPE") val trailingPE: YahooValue?,
    @SerializedName("forwardPE") val forwardPE: YahooValue?,
    @SerializedName("dividendYield") val dividendYield: YahooValue?,
    @SerializedName("marketCap") val marketCap: YahooValue?,
    @SerializedName("fiftyTwoWeekHigh") val fiftyTwoWeekHigh: YahooValue?,
    @SerializedName("fiftyTwoWeekLow") val fiftyTwoWeekLow: YahooValue?,
    @SerializedName("averageVolume") val averageVolume: YahooValue?,
    @SerializedName("volume") val volume: YahooValue?,
    @SerializedName("open") val open: YahooValue?,
    @SerializedName("previousClose") val previousClose: YahooValue?,
    @SerializedName("regularMarketPrice") val regularMarketPrice: YahooValue?
)

data class YahooKeyStatistics(
    @SerializedName("priceToBook") val priceToBook: YahooValue?,
    @SerializedName("bookValue") val bookValue: YahooValue?,
    @SerializedName("earningsPerShare") val earningsPerShare: YahooValue?,
    @SerializedName("revenueGrowth") val revenueGrowth: YahooValue?,
    @SerializedName("returnOnEquity") val returnOnEquity: YahooValue?
)

data class YahooFinancialData(
    @SerializedName("currentPrice") val currentPrice: YahooValue?,
    @SerializedName("targetHighPrice") val targetHighPrice: YahooValue?,
    @SerializedName("targetLowPrice") val targetLowPrice: YahooValue?,
    @SerializedName("targetMeanPrice") val targetMeanPrice: YahooValue?,
    @SerializedName("returnOnEquity") val returnOnEquity: YahooValue?,
    @SerializedName("debtToEquity") val debtToEquity: YahooValue?,
    @SerializedName("revenueGrowth") val revenueGrowth: YahooValue?,
    @SerializedName("grossMargins") val grossMargins: YahooValue?,
    @SerializedName("profitMargins") val profitMargins: YahooValue?,
    @SerializedName("totalRevenue") val totalRevenue: YahooValue?,
    @SerializedName("recommendationKey") val recommendationKey: String?
)

data class YahooPrice(
    @SerializedName("regularMarketPrice") val regularMarketPrice: YahooValue?,
    @SerializedName("regularMarketChange") val regularMarketChange: YahooValue?,
    @SerializedName("regularMarketChangePercent") val regularMarketChangePercent: YahooValue?,
    @SerializedName("regularMarketVolume") val regularMarketVolume: YahooValue?,
    @SerializedName("shortName") val shortName: String?,
    @SerializedName("longName") val longName: String?,
    @SerializedName("exchangeName") val exchangeName: String?,
    @SerializedName("marketCap") val marketCap: YahooValue?,
    @SerializedName("regularMarketDayHigh") val regularMarketDayHigh: YahooValue?,
    @SerializedName("regularMarketDayLow") val regularMarketDayLow: YahooValue?,
    @SerializedName("regularMarketOpen") val regularMarketOpen: YahooValue?
)

data class YahooAssetProfile(
    @SerializedName("sector") val sector: String?,
    @SerializedName("industry") val industry: String?,
    @SerializedName("longBusinessSummary") val longBusinessSummary: String?
)

data class YahooValue(
    @SerializedName("raw") val raw: Double?,
    @SerializedName("fmt") val fmt: String?
)

data class YahooError(
    @SerializedName("code") val code: String?,
    @SerializedName("description") val description: String?
)

// News API Response (GNews free tier)
data class GNewsResponse(
    @SerializedName("totalArticles") val totalArticles: Int?,
    @SerializedName("articles") val articles: List<GNewsArticle>?
)

data class GNewsArticle(
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("content") val content: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("source") val source: GNewsSource?,
    @SerializedName("publishedAt") val publishedAt: String?
)

data class GNewsSource(
    @SerializedName("name") val name: String?,
    @SerializedName("url") val url: String?
)

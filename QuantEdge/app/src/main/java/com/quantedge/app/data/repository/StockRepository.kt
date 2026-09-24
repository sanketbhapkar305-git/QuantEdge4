package com.quantedge.app.data.repository

import com.quantedge.app.data.api.StockCacheDao
import com.quantedge.app.data.api.WatchlistDao
import com.quantedge.app.data.api.PortfolioDao
import com.quantedge.app.data.api.YahooFinanceApi
import com.quantedge.app.data.model.CachedStockEntity
import com.quantedge.app.data.model.PortfolioEntity
import com.quantedge.app.data.model.WatchlistEntity
import com.quantedge.app.domain.model.HistoricalData
import com.quantedge.app.domain.model.MarketIndex
import com.quantedge.app.domain.model.PortfolioStock
import com.quantedge.app.domain.model.Stock
import com.quantedge.app.domain.model.WatchlistItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

@Singleton
class StockRepository @Inject constructor(
    private val yahooFinanceApi: YahooFinanceApi,
    private val watchlistDao: WatchlistDao,
    private val portfolioDao: PortfolioDao,
    private val stockCacheDao: StockCacheDao
) {
    // Cache TTL: 5 minutes for real-time data
    private val CACHE_TTL_MS = 5 * 60 * 1000L

    // Popular BSE/NSE stocks list
    val popularStocks = listOf(
        Pair("RELIANCE.NS", "Reliance Industries"),
        Pair("TCS.NS", "Tata Consultancy Services"),
        Pair("HDFCBANK.NS", "HDFC Bank"),
        Pair("INFY.NS", "Infosys"),
        Pair("ICICIBANK.NS", "ICICI Bank"),
        Pair("HINDUNILVR.NS", "Hindustan Unilever"),
        Pair("SBIN.NS", "State Bank of India"),
        Pair("BHARTIARTL.NS", "Bharti Airtel"),
        Pair("KOTAKBANK.NS", "Kotak Mahindra Bank"),
        Pair("LT.NS", "Larsen & Toubro"),
        Pair("AXISBANK.NS", "Axis Bank"),
        Pair("ASIANPAINT.NS", "Asian Paints"),
        Pair("MARUTI.NS", "Maruti Suzuki"),
        Pair("SUNPHARMA.NS", "Sun Pharmaceutical"),
        Pair("TITAN.NS", "Titan Company"),
        Pair("WIPRO.NS", "Wipro"),
        Pair("HCLTECH.NS", "HCL Technologies"),
        Pair("TECHM.NS", "Tech Mahindra"),
        Pair("BAJFINANCE.NS", "Bajaj Finance"),
        Pair("NESTLEIND.NS", "Nestle India"),
        Pair("ONGC.NS", "Oil & Natural Gas Corp"),
        Pair("POWERGRID.NS", "Power Grid Corp"),
        Pair("NTPC.NS", "NTPC Limited"),
        Pair("TATAMOTORS.NS", "Tata Motors"),
        Pair("DRREDDY.NS", "Dr. Reddy's Laboratories"),
        Pair("TATASTEEL.NS", "Tata Steel"),
        Pair("JSWSTEEL.NS", "JSW Steel"),
        Pair("CIPLA.NS", "Cipla"),
        Pair("ADANIPORTS.NS", "Adani Ports"),
        Pair("ULTRACEMCO.NS", "UltraTech Cement")
    )

    suspend fun getStock(symbol: String): Result<Stock> {
        // Check cache first
        val cached = stockCacheDao.getCachedStock(symbol)
        if (cached != null && (System.currentTimeMillis() - cached.cachedAt) < CACHE_TTL_MS) {
            return Result.Success(cached.toDomain())
        }

        return try {
            val response = yahooFinanceApi.getQuoteSummary(symbol)
            if (response.isSuccessful) {
                val result = response.body()?.quoteSummary?.result?.firstOrNull()
                if (result != null) {
                    val stock = result.toDomain(symbol)
                    stockCacheDao.cacheStock(stock.toEntity())
                    Result.Success(stock)
                } else {
                    // Fallback to chart API
                    getStockFromChart(symbol)
                }
            } else {
                Result.Error("API error: ${response.code()}")
            }
        } catch (e: Exception) {
            if (cached != null) Result.Success(cached.toDomain())
            else Result.Error("Network error: ${e.localizedMessage}", e)
        }
    }

    private suspend fun getStockFromChart(symbol: String): Result<Stock> {
        return try {
            val response = yahooFinanceApi.getChart(symbol, interval = "1d", range = "1mo")
            if (response.isSuccessful) {
                val meta = response.body()?.chart?.result?.firstOrNull()?.meta
                if (meta != null) {
                    val price = meta.regularMarketPrice ?: 0.0
                    val prevClose = meta.previousClose ?: 0.0
                    val change = price - prevClose
                    val changePercent = if (prevClose != 0.0) (change / prevClose) * 100 else 0.0
                    val symbolName = popularStocks.find { it.first == symbol }?.second ?: symbol
                    val stock = Stock(
                        symbol = symbol,
                        name = symbolName,
                        exchange = if (symbol.endsWith(".NS")) "NSE" else "BSE",
                        currentPrice = price,
                        previousClose = prevClose,
                        change = change,
                        changePercent = changePercent,
                        volume = meta.regularMarketVolume ?: 0L,
                        avgVolume = meta.regularMarketVolume ?: 0L,
                        marketCap = 0L,
                        high52Week = meta.fiftyTwoWeekHigh ?: 0.0,
                        low52Week = meta.fiftyTwoWeekLow ?: 0.0,
                        dayHigh = meta.regularMarketDayHigh ?: 0.0,
                        dayLow = meta.regularMarketDayLow ?: 0.0,
                        openPrice = meta.regularMarketOpen ?: 0.0,
                        sector = "Unknown",
                        industry = "Unknown",
                        peRatio = null,
                        eps = null,
                        dividendYield = null,
                        bookValue = null,
                        priceToBook = null,
                        roe = null,
                        debtToEquity = null,
                        revenueGrowth = null,
                        profitMargin = null
                    )
                    Result.Success(stock)
                } else {
                    Result.Error("No data found for $symbol")
                }
            } else {
                Result.Error("Failed to fetch data: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.localizedMessage}", e)
        }
    }

    suspend fun getHistoricalData(symbol: String, range: String = "5y"): Result<HistoricalData> {
        return try {
            val interval = when (range) {
                "1d" -> "5m"
                "5d", "1mo" -> "1h"
                else -> "1wk"
            }
            val response = yahooFinanceApi.getChart(symbol, interval = interval, range = range)
            if (response.isSuccessful) {
                val result = response.body()?.chart?.result?.firstOrNull()
                val timestamps = result?.timestamp ?: emptyList()
                val quote = result?.indicators?.quote?.firstOrNull()
                val historicalData = HistoricalData(
                    symbol = symbol,
                    timestamps = timestamps,
                    opens = quote?.open?.mapNotNull { it } ?: emptyList(),
                    highs = quote?.high?.mapNotNull { it } ?: emptyList(),
                    lows = quote?.low?.mapNotNull { it } ?: emptyList(),
                    closes = quote?.close?.mapNotNull { it } ?: emptyList(),
                    volumes = quote?.volume?.mapNotNull { it } ?: emptyList()
                )
                Result.Success(historicalData)
            } else {
                Result.Error("Failed to fetch history: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.localizedMessage}", e)
        }
    }

    suspend fun getMarketIndices(): Result<List<MarketIndex>> {
        val indices = listOf(
            "^BSESN" to "SENSEX",
            "^NSEI" to "NIFTY 50",
            "^NSEBANK" to "BANK NIFTY",
            "^CNXIT" to "NIFTY IT"
        )
        return try {
            val results = indices.mapNotNull { (symbol, name) ->
                try {
                    val res = yahooFinanceApi.getChart(symbol, interval = "1d", range = "5d")
                    val meta = res.body()?.chart?.result?.firstOrNull()?.meta
                    if (meta != null) {
                        val price = meta.regularMarketPrice ?: 0.0
                        val prev = meta.previousClose ?: 0.0
                        val change = price - prev
                        MarketIndex(
                            name = name,
                            value = price,
                            change = change,
                            changePercent = if (prev != 0.0) (change / prev) * 100 else 0.0
                        )
                    } else null
                } catch (e: Exception) { null }
            }
            Result.Success(results)
        } catch (e: Exception) {
            Result.Error("Failed to load indices", e)
        }
    }

    // Watchlist operations
    fun getWatchlist(): Flow<List<WatchlistItem>> =
        watchlistDao.getAllWatchlist().map { list ->
            list.map { WatchlistItem(it.symbol, it.name, it.exchange, it.addedAt) }
        }

    suspend fun addToWatchlist(symbol: String, name: String, exchange: String) {
        watchlistDao.addToWatchlist(WatchlistEntity(symbol, name, exchange))
    }

    suspend fun removeFromWatchlist(symbol: String) {
        watchlistDao.removeFromWatchlist(symbol)
    }

    suspend fun isInWatchlist(symbol: String): Boolean =
        watchlistDao.isInWatchlist(symbol)

    // Portfolio operations
    fun getPortfolio(): Flow<List<PortfolioEntity>> = portfolioDao.getAllPortfolio()

    suspend fun addToPortfolio(symbol: String, name: String, exchange: String, quantity: Int, buyPrice: Double) {
        portfolioDao.addToPortfolio(PortfolioEntity(symbol, name, exchange, quantity, buyPrice))
    }

    suspend fun removeFromPortfolio(symbol: String) {
        portfolioDao.removeFromPortfolio(symbol)
    }
}

// Extension functions for mapping
private fun com.quantedge.app.data.model.YahooQuoteSummaryResult.toDomain(symbol: String): Stock {
    val p = this.price
    val s = this.summaryDetail
    val k = this.defaultKeyStatistics
    val f = this.financialData
    val a = this.assetProfile
    val price = p?.regularMarketPrice?.raw ?: s?.regularMarketPrice?.raw ?: 0.0
    val prevClose = s?.previousClose?.raw ?: 0.0
    val change = p?.regularMarketChange?.raw ?: (price - prevClose)
    val changePct = p?.regularMarketChangePercent?.raw?.times(100) ?: if (prevClose != 0.0) (change / prevClose * 100) else 0.0
    return Stock(
        symbol = symbol,
        name = p?.longName ?: p?.shortName ?: symbol,
        exchange = p?.exchangeName ?: if (symbol.endsWith(".NS")) "NSE" else "BSE",
        currentPrice = price,
        previousClose = prevClose,
        change = change,
        changePercent = changePct,
        volume = p?.regularMarketVolume?.raw?.toLong() ?: s?.volume?.raw?.toLong() ?: 0L,
        avgVolume = s?.averageVolume?.raw?.toLong() ?: 0L,
        marketCap = p?.marketCap?.raw?.toLong() ?: s?.marketCap?.raw?.toLong() ?: 0L,
        high52Week = s?.fiftyTwoWeekHigh?.raw ?: 0.0,
        low52Week = s?.fiftyTwoWeekLow?.raw ?: 0.0,
        dayHigh = p?.regularMarketDayHigh?.raw ?: 0.0,
        dayLow = p?.regularMarketDayLow?.raw ?: 0.0,
        openPrice = p?.regularMarketOpen?.raw ?: s?.open?.raw ?: 0.0,
        sector = a?.sector ?: "Unknown",
        industry = a?.industry ?: "Unknown",
        peRatio = s?.trailingPE?.raw,
        eps = k?.earningsPerShare?.raw,
        dividendYield = s?.dividendYield?.raw?.times(100),
        bookValue = k?.bookValue?.raw,
        priceToBook = k?.priceToBook?.raw,
        roe = (f?.returnOnEquity?.raw ?: k?.returnOnEquity?.raw)?.times(100),
        debtToEquity = f?.debtToEquity?.raw,
        revenueGrowth = f?.revenueGrowth?.raw?.times(100),
        profitMargin = f?.profitMargins?.raw?.times(100)
    )
}

private fun CachedStockEntity.toDomain(): Stock = Stock(
    symbol = symbol, name = name, exchange = exchange,
    currentPrice = currentPrice, previousClose = previousClose,
    change = change, changePercent = changePercent,
    volume = volume, avgVolume = avgVolume, marketCap = marketCap,
    high52Week = high52Week, low52Week = low52Week,
    dayHigh = dayHigh, dayLow = dayLow, openPrice = openPrice,
    sector = sector, industry = industry,
    peRatio = peRatio.takeIf { it != 0.0 },
    eps = eps.takeIf { it != 0.0 },
    dividendYield = dividendYield.takeIf { it != 0.0 },
    bookValue = bookValue.takeIf { it != 0.0 },
    priceToBook = priceToBook.takeIf { it != 0.0 },
    roe = roe.takeIf { it != 0.0 },
    debtToEquity = debtToEquity.takeIf { it != 0.0 },
    revenueGrowth = revenueGrowth.takeIf { it != 0.0 },
    profitMargin = profitMargin.takeIf { it != 0.0 }
)

private fun Stock.toEntity(): CachedStockEntity = CachedStockEntity(
    symbol = symbol, name = name, exchange = exchange,
    currentPrice = currentPrice, previousClose = previousClose,
    change = change, changePercent = changePercent,
    volume = volume, avgVolume = avgVolume, marketCap = marketCap,
    high52Week = high52Week, low52Week = low52Week,
    dayHigh = dayHigh, dayLow = dayLow, openPrice = openPrice,
    sector = sector, industry = industry,
    peRatio = peRatio ?: 0.0, eps = eps ?: 0.0,
    dividendYield = dividendYield ?: 0.0, bookValue = bookValue ?: 0.0,
    priceToBook = priceToBook ?: 0.0, roe = roe ?: 0.0,
    debtToEquity = debtToEquity ?: 0.0,
    revenueGrowth = revenueGrowth ?: 0.0,
    profitMargin = profitMargin ?: 0.0
)

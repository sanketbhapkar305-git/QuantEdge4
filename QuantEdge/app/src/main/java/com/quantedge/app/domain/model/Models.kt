package com.quantedge.app.domain.model

data class Stock(
    val symbol: String,
    val name: String,
    val exchange: String, // BSE or NSE
    val currentPrice: Double,
    val previousClose: Double,
    val change: Double,
    val changePercent: Double,
    val volume: Long,
    val avgVolume: Long,
    val marketCap: Long,
    val high52Week: Double,
    val low52Week: Double,
    val dayHigh: Double,
    val dayLow: Double,
    val openPrice: Double,
    val sector: String,
    val industry: String,
    val peRatio: Double?,
    val eps: Double?,
    val dividendYield: Double?,
    val bookValue: Double?,
    val priceToBook: Double?,
    val roe: Double?,
    val debtToEquity: Double?,
    val revenueGrowth: Double?,
    val profitMargin: Double?,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isPositive: Boolean get() = change >= 0
    val changeFormatted: String get() = if (isPositive) "+%.2f%%".format(changePercent) else "%.2f%%".format(changePercent)
    val priceFormatted: String get() = "₹%.2f".format(currentPrice)
}

data class StockQuote(
    val symbol: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val volume: Long,
    val timestamp: Long
)

data class HistoricalData(
    val symbol: String,
    val timestamps: List<Long>,
    val opens: List<Double>,
    val highs: List<Double>,
    val lows: List<Double>,
    val closes: List<Double>,
    val volumes: List<Long>
)

data class NewsItem(
    val id: String,
    val title: String,
    val summary: String,
    val source: String,
    val url: String,
    val publishedAt: Long,
    val sentiment: SentimentScore,
    val relatedSymbols: List<String>
)

data class SentimentScore(
    val score: Float, // -1.0 (very negative) to 1.0 (very positive)
    val label: SentimentLabel,
    val confidence: Float
)

enum class SentimentLabel {
    VERY_BULLISH, BULLISH, NEUTRAL, BEARISH, VERY_BEARISH
}

data class TradingSignal(
    val symbol: String,
    val signalType: SignalType,
    val tradingMode: TradingMode,
    val confidence: Float, // 0.0 to 1.0
    val targetPrice: Double,
    val stopLoss: Double,
    val entryPrice: Double,
    val rationale: List<String>,
    val activeFactors: List<AnalysisFactor>,
    val timestamp: Long = System.currentTimeMillis()
) {
    val confidencePercent: Int get() = (confidence * 100).toInt()
    val riskRewardRatio: Double get() = (targetPrice - entryPrice) / (entryPrice - stopLoss)
}

enum class SignalType {
    STRONG_BUY, BUY, HOLD, SELL, STRONG_SELL
}

enum class TradingMode {
    SWING_TRADING, EQUITY_LONG_TERM
}

enum class AnalysisFactor(val displayName: String, val description: String) {
    TECHNICAL_RSI("RSI Analysis", "Relative Strength Index momentum signal"),
    TECHNICAL_MACD("MACD Signal", "Moving Average Convergence Divergence"),
    TECHNICAL_BOLLINGER("Bollinger Bands", "Price volatility and mean reversion"),
    TECHNICAL_MOVING_AVERAGE("Moving Averages", "SMA/EMA crossover signals"),
    TECHNICAL_VOLUME("Volume Analysis", "Volume trend and anomaly detection"),
    FUNDAMENTAL_PE("P/E Ratio", "Price-to-earnings valuation"),
    FUNDAMENTAL_ROE("Return on Equity", "Company profitability assessment"),
    FUNDAMENTAL_DEBT("Debt Analysis", "Balance sheet health check"),
    FUNDAMENTAL_REVENUE("Revenue Growth", "2-5 year revenue trend analysis"),
    FUNDAMENTAL_MARGINS("Profit Margins", "Gross and net margin trends"),
    NEWS_SENTIMENT("News Sentiment", "Real-time news sentiment analysis"),
    MARKET_BREADTH("Market Breadth", "Nifty/Sensex trend context"),
    SECTOR_MOMENTUM("Sector Momentum", "Sector rotation and momentum"),
    FII_DII_ACTIVITY("FII/DII Activity", "Institutional buying/selling patterns")
}

data class AnalysisConfig(
    val enabledFactors: Set<AnalysisFactor> = setOf(
        AnalysisFactor.TECHNICAL_RSI,
        AnalysisFactor.TECHNICAL_MACD,
        AnalysisFactor.TECHNICAL_MOVING_AVERAGE,
        AnalysisFactor.FUNDAMENTAL_PE,
        AnalysisFactor.NEWS_SENTIMENT
    ),
    val tradingMode: TradingMode = TradingMode.SWING_TRADING,
    val riskTolerance: RiskLevel = RiskLevel.MODERATE
)

enum class RiskLevel { LOW, MODERATE, HIGH }

data class PortfolioStock(
    val symbol: String,
    val name: String,
    val exchange: String,
    val quantity: Int,
    val buyPrice: Double,
    val currentPrice: Double
) {
    val investedValue: Double get() = quantity * buyPrice
    val currentValue: Double get() = quantity * currentPrice
    val profitLoss: Double get() = currentValue - investedValue
    val profitLossPercent: Double get() = (profitLoss / investedValue) * 100
    val isProfit: Boolean get() = profitLoss >= 0
}

data class WatchlistItem(
    val symbol: String,
    val name: String,
    val exchange: String,
    val addedAt: Long = System.currentTimeMillis()
)

data class MarketIndex(
    val name: String,
    val value: Double,
    val change: Double,
    val changePercent: Double,
    val isPositive: Boolean get() = change >= 0
)

package com.quantedge.app.domain.usecase

import com.quantedge.app.domain.model.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import javax.inject.Inject
import javax.inject.Singleton

/**
 * QuantEdge Analysis Engine
 *
 * Computes trading signals by combining:
 * - Technical indicators (RSI, MACD, Bollinger Bands, Moving Averages, Volume)
 * - Fundamental metrics (P/E, ROE, Debt, Revenue Growth, Profit Margins)
 * - News sentiment scoring
 * - Market breadth context
 *
 * Each factor contributes a score from -1.0 (very bearish) to +1.0 (very bullish).
 * Weighted aggregation produces final signal with confidence.
 */
@Singleton
class AnalysisEngine @Inject constructor() {

    private data class FactorScore(
        val factor: AnalysisFactor,
        val score: Float,      // -1.0 to 1.0
        val weight: Float,     // relative importance
        val rationale: String
    )

    fun analyzeStock(
        stock: Stock,
        historicalData: HistoricalData?,
        newsItems: List<NewsItem>,
        config: AnalysisConfig
    ): TradingSignal {
        val scores = mutableListOf<FactorScore>()

        val closes = historicalData?.closes ?: emptyList()

        // --- Technical Factors ---
        if (config.enabledFactors.contains(AnalysisFactor.TECHNICAL_RSI) && closes.size >= 14) {
            scores.add(scoreRSI(closes))
        }
        if (config.enabledFactors.contains(AnalysisFactor.TECHNICAL_MACD) && closes.size >= 26) {
            scores.add(scoreMacd(closes))
        }
        if (config.enabledFactors.contains(AnalysisFactor.TECHNICAL_BOLLINGER) && closes.size >= 20) {
            scores.add(scoreBollingerBands(stock.currentPrice, closes))
        }
        if (config.enabledFactors.contains(AnalysisFactor.TECHNICAL_MOVING_AVERAGE) && closes.size >= 50) {
            scores.add(scoreMovingAverages(stock.currentPrice, closes))
        }
        if (config.enabledFactors.contains(AnalysisFactor.TECHNICAL_VOLUME) && historicalData != null) {
            scores.add(scoreVolume(historicalData.volumes, stock.volume, stock.avgVolume))
        }

        // --- Fundamental Factors ---
        if (config.enabledFactors.contains(AnalysisFactor.FUNDAMENTAL_PE)) {
            stock.peRatio?.let { scores.add(scorePE(it, stock.sector)) }
        }
        if (config.enabledFactors.contains(AnalysisFactor.FUNDAMENTAL_ROE)) {
            stock.roe?.let { scores.add(scoreROE(it)) }
        }
        if (config.enabledFactors.contains(AnalysisFactor.FUNDAMENTAL_DEBT)) {
            stock.debtToEquity?.let { scores.add(scoreDebtToEquity(it)) }
        }
        if (config.enabledFactors.contains(AnalysisFactor.FUNDAMENTAL_REVENUE)) {
            stock.revenueGrowth?.let { scores.add(scoreRevenueGrowth(it)) }
        }
        if (config.enabledFactors.contains(AnalysisFactor.FUNDAMENTAL_MARGINS)) {
            stock.profitMargin?.let { scores.add(scoreProfitMargin(it)) }
        }

        // --- Sentiment Factor ---
        if (config.enabledFactors.contains(AnalysisFactor.NEWS_SENTIMENT) && newsItems.isNotEmpty()) {
            scores.add(scoreNewsSentiment(newsItems))
        }

        // --- Price Position Factor ---
        if (stock.high52Week > 0 && stock.low52Week > 0) {
            scores.add(score52WeekPosition(stock.currentPrice, stock.high52Week, stock.low52Week))
        }

        // Calculate weighted aggregate score
        val totalWeight = scores.sumOf { it.weight.toDouble() }.toFloat()
        val weightedScore = if (totalWeight > 0) {
            scores.sumOf { (it.score * it.weight).toDouble() }.toFloat() / totalWeight
        } else 0f

        // Apply risk multiplier
        val riskMultiplier = when (config.riskTolerance) {
            RiskLevel.LOW -> 0.8f
            RiskLevel.MODERATE -> 1.0f
            RiskLevel.HIGH -> 1.2f
        }

        // Mode modifier: equity long-term favours fundamentals
        val modeAdjustedScore = if (config.tradingMode == TradingMode.EQUITY_LONG_TERM) {
            val fundamentalScores = scores.filter { isFundamental(it.factor) }
            val techScores = scores.filter { isTechnical(it.factor) }
            val fundWeight = fundamentalScores.sumOf { it.weight.toDouble() }.toFloat()
            val techWeight = techScores.sumOf { it.weight.toDouble() }.toFloat()
            val fundScore = if (fundWeight > 0) fundamentalScores.sumOf { (it.score * it.weight).toDouble() }.toFloat() / fundWeight else 0f
            val techScore = if (techWeight > 0) techScores.sumOf { (it.score * it.weight).toDouble() }.toFloat() / techWeight else 0f
            (fundScore * 0.65f + techScore * 0.35f)
        } else {
            weightedScore // swing: equal weight
        }

        val finalScore = (modeAdjustedScore * riskMultiplier).coerceIn(-1f, 1f)
        val confidence = (abs(finalScore) * 0.7f + (scores.size.toFloat() / 14f) * 0.3f).coerceIn(0.2f, 0.95f)

        val signalType = when {
            finalScore >= 0.6f -> SignalType.STRONG_BUY
            finalScore >= 0.25f -> SignalType.BUY
            finalScore <= -0.6f -> SignalType.STRONG_SELL
            finalScore <= -0.25f -> SignalType.SELL
            else -> SignalType.HOLD
        }

        // Calculate targets based on ATR (Average True Range approximation)
        val atr = if (closes.size >= 14) calculateATR(historicalData) else stock.currentPrice * 0.025
        val atrMultiplier = when (config.tradingMode) {
            TradingMode.SWING_TRADING -> 3.0
            TradingMode.EQUITY_LONG_TERM -> 6.0
        }

        val targetPrice = when {
            signalType in listOf(SignalType.BUY, SignalType.STRONG_BUY) ->
                stock.currentPrice + atr * atrMultiplier
            signalType in listOf(SignalType.SELL, SignalType.STRONG_SELL) ->
                stock.currentPrice - atr * atrMultiplier
            else -> stock.currentPrice
        }

        val stopLoss = when {
            signalType in listOf(SignalType.BUY, SignalType.STRONG_BUY) ->
                stock.currentPrice - atr * 1.5
            signalType in listOf(SignalType.SELL, SignalType.STRONG_SELL) ->
                stock.currentPrice + atr * 1.5
            else -> stock.currentPrice * 0.95
        }

        val rationale = scores
            .sortedByDescending { abs(it.score) }
            .take(5)
            .map { it.rationale }

        return TradingSignal(
            symbol = stock.symbol,
            signalType = signalType,
            tradingMode = config.tradingMode,
            confidence = confidence,
            targetPrice = targetPrice,
            stopLoss = stopLoss,
            entryPrice = stock.currentPrice,
            rationale = rationale,
            activeFactors = scores.map { it.factor }
        )
    }

    // ---- RSI ----
    private fun scoreRSI(closes: List<Double>): FactorScore {
        val rsi = calculateRSI(closes, 14)
        val score = when {
            rsi < 25 -> 0.9f   // deeply oversold - strong buy
            rsi < 35 -> 0.6f   // oversold
            rsi < 45 -> 0.2f   // slightly oversold
            rsi > 75 -> -0.9f  // deeply overbought - strong sell
            rsi > 65 -> -0.6f  // overbought
            rsi > 55 -> -0.2f  // slightly overbought
            else -> 0f         // neutral zone
        }
        val label = when {
            rsi < 30 -> "oversold (${String.format("%.1f", rsi)})"
            rsi > 70 -> "overbought (${String.format("%.1f", rsi)})"
            else -> "neutral (${String.format("%.1f", rsi)})"
        }
        return FactorScore(AnalysisFactor.TECHNICAL_RSI, score, 1.4f, "RSI is $label")
    }

    // ---- MACD ----
    private fun scoreMacd(closes: List<Double>): FactorScore {
        val ema12 = calculateEMA(closes, 12)
        val ema26 = calculateEMA(closes, 26)
        val macd = ema12 - ema26
        val signal = calculateEMA(closes.takeLast(9), 9)
        val histogram = macd - signal
        val score = when {
            histogram > 0 && macd > 0 -> 0.8f
            histogram > 0 && macd < 0 -> 0.4f
            histogram < 0 && macd > 0 -> -0.4f
            else -> -0.8f
        }
        val direction = if (histogram > 0) "bullish crossover" else "bearish crossover"
        return FactorScore(AnalysisFactor.TECHNICAL_MACD, score, 1.3f, "MACD shows $direction signal")
    }

    // ---- Bollinger Bands ----
    private fun scoreBollingerBands(price: Double, closes: List<Double>): FactorScore {
        val last20 = closes.takeLast(20)
        val sma = last20.average()
        val std = sqrt(last20.map { (it - sma) * (it - sma) }.average())
        val upper = sma + 2 * std
        val lower = sma - 2 * std
        val percentB = if (upper != lower) (price - lower) / (upper - lower) else 0.5

        val score = when {
            percentB < 0.05 -> 0.9f  // below lower band - oversold
            percentB < 0.2 -> 0.5f
            percentB > 0.95 -> -0.9f // above upper band - overbought
            percentB > 0.8 -> -0.5f
            else -> 0f
        }
        val position = String.format("%.0f%%", percentB * 100)
        return FactorScore(AnalysisFactor.TECHNICAL_BOLLINGER, score, 1.1f,
            "Price at $position of Bollinger Band range")
    }

    // ---- Moving Averages ----
    private fun scoreMovingAverages(price: Double, closes: List<Double>): FactorScore {
        val sma20 = closes.takeLast(20).average()
        val sma50 = closes.takeLast(50).average()
        val sma200 = if (closes.size >= 200) closes.takeLast(200).average() else closes.average()

        var score = 0f
        val signals = mutableListOf<String>()

        if (price > sma20) { score += 0.2f; signals.add("above SMA20") } else { score -= 0.2f; signals.add("below SMA20") }
        if (price > sma50) { score += 0.3f; signals.add("above SMA50") } else { score -= 0.3f; signals.add("below SMA50") }
        if (price > sma200) { score += 0.4f; signals.add("above SMA200") } else { score -= 0.4f; signals.add("below SMA200") }
        if (sma50 > sma200) { score += 0.1f; signals.add("golden cross") } else { score -= 0.1f; signals.add("death cross") }

        return FactorScore(AnalysisFactor.TECHNICAL_MOVING_AVERAGE, score.coerceIn(-1f, 1f), 1.3f,
            "Price ${signals.joinToString(", ")}")
    }

    // ---- Volume Analysis ----
    private fun scoreVolume(volumes: List<Long>, currentVol: Long, avgVol: Long): FactorScore {
        val recentAvg = if (volumes.size >= 20) volumes.takeLast(20).average().toLong() else avgVol
        val volRatio = if (recentAvg > 0) currentVol.toDouble() / recentAvg else 1.0
        val score = when {
            volRatio > 2.5 -> 0.6f   // very high volume - strong conviction
            volRatio > 1.5 -> 0.3f
            volRatio < 0.5 -> -0.3f  // low volume - weak signal
            else -> 0.1f
        }
        val fmt = String.format("%.1fx", volRatio)
        return FactorScore(AnalysisFactor.TECHNICAL_VOLUME, score, 0.8f,
            "Volume at ${fmt} of 20-day average")
    }

    // ---- P/E Ratio ----
    private fun scorePE(pe: Double, sector: String): FactorScore {
        // Sector-adjusted fair P/E benchmarks for Indian markets
        val fairPE = when {
            sector.contains("Technology", ignoreCase = true) ||
            sector.contains("IT", ignoreCase = true) -> 30.0
            sector.contains("Finance", ignoreCase = true) ||
            sector.contains("Banking", ignoreCase = true) -> 18.0
            sector.contains("Consumer", ignoreCase = true) ||
            sector.contains("FMCG", ignoreCase = true) -> 35.0
            sector.contains("Pharma", ignoreCase = true) -> 25.0
            sector.contains("Energy", ignoreCase = true) -> 12.0
            sector.contains("Metal", ignoreCase = true) ||
            sector.contains("Steel", ignoreCase = true) -> 10.0
            else -> 20.0
        }
        val peRatio = pe / fairPE
        val score = when {
            pe < 0 -> -0.5f            // loss-making
            peRatio < 0.6 -> 0.8f     // deeply undervalued
            peRatio < 0.85 -> 0.4f    // undervalued
            peRatio > 2.0 -> -0.8f    // extremely overvalued
            peRatio > 1.4 -> -0.4f    // overvalued
            else -> 0f                 // fairly valued
        }
        return FactorScore(AnalysisFactor.FUNDAMENTAL_PE, score, 1.2f,
            "P/E of ${String.format("%.1f", pe)} vs sector benchmark of ${fairPE.toInt()}")
    }

    // ---- ROE ----
    private fun scoreROE(roe: Double): FactorScore {
        val score = when {
            roe >= 25 -> 0.9f
            roe >= 18 -> 0.6f
            roe >= 12 -> 0.3f
            roe >= 8 -> 0f
            roe >= 0 -> -0.3f
            else -> -0.8f
        }
        return FactorScore(AnalysisFactor.FUNDAMENTAL_ROE, score, 1.2f,
            "Return on Equity is ${String.format("%.1f", roe)}%")
    }

    // ---- Debt to Equity ----
    private fun scoreDebtToEquity(de: Double): FactorScore {
        val score = when {
            de < 0.1 -> 0.8f
            de < 0.3 -> 0.5f
            de < 0.5 -> 0.2f
            de < 1.0 -> 0f
            de < 1.5 -> -0.3f
            de < 2.0 -> -0.6f
            else -> -0.9f
        }
        return FactorScore(AnalysisFactor.FUNDAMENTAL_DEBT, score, 1.0f,
            "Debt/Equity ratio of ${String.format("%.2f", de)}")
    }

    // ---- Revenue Growth ----
    private fun scoreRevenueGrowth(growth: Double): FactorScore {
        val score = when {
            growth >= 25 -> 0.9f
            growth >= 15 -> 0.6f
            growth >= 10 -> 0.3f
            growth >= 5 -> 0.1f
            growth >= 0 -> -0.1f
            growth >= -5 -> -0.4f
            else -> -0.8f
        }
        return FactorScore(AnalysisFactor.FUNDAMENTAL_REVENUE, score, 1.1f,
            "Revenue growth of ${String.format("%.1f", growth)}% YoY")
    }

    // ---- Profit Margin ----
    private fun scoreProfitMargin(margin: Double): FactorScore {
        val score = when {
            margin >= 20 -> 0.8f
            margin >= 12 -> 0.5f
            margin >= 7 -> 0.2f
            margin >= 3 -> 0f
            margin >= 0 -> -0.3f
            else -> -0.8f
        }
        return FactorScore(AnalysisFactor.FUNDAMENTAL_MARGINS, score, 1.0f,
            "Net profit margin of ${String.format("%.1f", margin)}%")
    }

    // ---- News Sentiment ----
    private fun scoreNewsSentiment(newsItems: List<NewsItem>): FactorScore {
        val recentItems = newsItems.sortedByDescending { it.publishedAt }.take(10)
        val avgSentiment = recentItems.map { it.sentiment.score }.average().toFloat()
        val score = avgSentiment.coerceIn(-1f, 1f)
        val label = when {
            avgSentiment > 0.3 -> "positive"
            avgSentiment < -0.3 -> "negative"
            else -> "mixed"
        }
        return FactorScore(AnalysisFactor.NEWS_SENTIMENT, score, 0.9f,
            "Recent news sentiment is $label (${recentItems.size} articles)")
    }

    // ---- 52-Week Position ----
    private fun score52WeekPosition(price: Double, high52w: Double, low52w: Double): FactorScore {
        val range = high52w - low52w
        val position = if (range > 0) (price - low52w) / range else 0.5
        val score = when {
            position < 0.15 -> 0.7f   // near 52-week low - potential reversal
            position < 0.30 -> 0.3f
            position > 0.90 -> -0.5f  // near 52-week high - potential resistance
            position > 0.75 -> -0.2f
            else -> 0.1f
        }
        val pct = String.format("%.0f%%", position * 100)
        return FactorScore(AnalysisFactor.SECTOR_MOMENTUM, score, 0.7f,
            "Price at $pct of 52-week range (Low: ₹${String.format("%.0f", low52w)}, High: ₹${String.format("%.0f", high52w)})")
    }

    // ---- Math Helpers ----
    private fun calculateRSI(closes: List<Double>, period: Int): Double {
        if (closes.size < period + 1) return 50.0
        val changes = closes.zipWithNext { a, b -> b - a }
        val gains = changes.map { max(0.0, it) }
        val losses = changes.map { max(0.0, -it) }

        var avgGain = gains.take(period).average()
        var avgLoss = losses.take(period).average()

        for (i in period until changes.size) {
            avgGain = (avgGain * (period - 1) + gains[i]) / period
            avgLoss = (avgLoss * (period - 1) + losses[i]) / period
        }

        return if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + avgGain / avgLoss))
    }

    private fun calculateEMA(closes: List<Double>, period: Int): Double {
        if (closes.isEmpty()) return 0.0
        val k = 2.0 / (period + 1)
        var ema = closes.take(period).average()
        for (i in period until closes.size) {
            ema = closes[i] * k + ema * (1 - k)
        }
        return ema
    }

    private fun calculateATR(data: HistoricalData?, period: Int = 14): Double {
        if (data == null || data.highs.size < 2) return 0.0
        val trueRanges = (1 until min(data.highs.size, period + 1)).map { i ->
            val high = data.highs[i]
            val low = data.lows[i]
            val prevClose = data.closes[i - 1]
            maxOf(high - low, abs(high - prevClose), abs(low - prevClose))
        }
        return if (trueRanges.isEmpty()) 0.0 else trueRanges.average()
    }

    private fun isFundamental(factor: AnalysisFactor) = factor.name.startsWith("FUNDAMENTAL")
    private fun isTechnical(factor: AnalysisFactor) = factor.name.startsWith("TECHNICAL")
}

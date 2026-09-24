package com.quantedge.app.domain.usecase

import com.quantedge.app.data.model.GNewsArticle
import com.quantedge.app.domain.model.NewsItem
import com.quantedge.app.domain.model.SentimentLabel
import com.quantedge.app.domain.model.SentimentScore
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * On-device sentiment analysis engine.
 * Uses a keyword/phrase lexicon tuned for Indian financial news.
 * No external NLP API required — runs entirely on device.
 */
@Singleton
class SentimentAnalyzer @Inject constructor() {

    private val bullishPhrases = mapOf(
        // Strong positive (weight 3)
        "record profit" to 3f, "all time high" to 3f, "beats estimate" to 3f,
        "strong buy" to 3f, "upgrade" to 3f, "dividend" to 2f,
        "buyback" to 2.5f, "acquisition" to 1.5f, "strategic partnership" to 2f,
        // Medium positive (weight 2)
        "profit growth" to 2f, "revenue growth" to 2f, "expansion" to 1.5f,
        "new order" to 2f, "order win" to 2.5f, "contract" to 1.5f,
        "market share" to 1.5f, "outperform" to 2f, "rally" to 1.5f,
        "surge" to 1.5f, "jump" to 1.5f, "soar" to 2f, "bullish" to 1.5f,
        "positive outlook" to 2f, "guidance raised" to 2.5f, "beat" to 2f,
        "exceed" to 1.5f, "recovery" to 1.5f, "turnaround" to 2f,
        "merger" to 1.5f, "ipo" to 1.5f, "bonus" to 1.5f,
        // Mild positive (weight 1)
        "growth" to 1f, "increase" to 1f, "gain" to 1f, "rise" to 1f,
        "improve" to 1f, "positive" to 1f, "strong" to 1f
    )

    private val bearishPhrases = mapOf(
        // Strong negative (weight 3)
        "record loss" to 3f, "all time low" to 3f, "misses estimate" to 3f,
        "strong sell" to 3f, "downgrade" to 3f, "fraud" to 3f,
        "bankruptcy" to 3f, "default" to 3f, "scam" to 3f, "sebi notice" to 2.5f,
        "income tax raid" to 3f, "ed probe" to 3f,
        // Medium negative (weight 2)
        "profit decline" to 2f, "revenue decline" to 2f, "losses" to 2f,
        "miss" to 1.5f, "weak" to 1.5f, "bearish" to 1.5f, "sell off" to 2f,
        "crash" to 2.5f, "plunge" to 2f, "drop" to 1.5f, "fall" to 1.5f,
        "slump" to 2f, "guidance cut" to 2.5f, "recall" to 1.5f,
        "controversy" to 2f, "concern" to 1.5f, "warning" to 2f,
        "penalty" to 2f, "fine" to 1.5f, "lawsuit" to 2f,
        // Mild negative (weight 1)
        "decline" to 1f, "decrease" to 1f, "loss" to 1f, "risk" to 1f,
        "pressure" to 1f, "challenge" to 1f, "negative" to 1f
    )

    fun analyze(text: String): SentimentScore {
        val lowerText = text.lowercase()
        var bullishScore = 0f
        var bearishScore = 0f

        bullishPhrases.forEach { (phrase, weight) ->
            val count = countOccurrences(lowerText, phrase)
            bullishScore += count * weight
        }
        bearishPhrases.forEach { (phrase, weight) ->
            val count = countOccurrences(lowerText, phrase)
            bearishScore += count * weight
        }

        val totalScore = bullishScore - bearishScore
        val magnitude = bullishScore + bearishScore
        val normalizedScore = if (magnitude > 0) {
            (totalScore / magnitude).coerceIn(-1f, 1f)
        } else 0f

        // Confidence based on how many signals were found
        val confidence = (magnitude / 10f).coerceIn(0.3f, 0.95f)

        val label = when {
            normalizedScore >= 0.5f -> SentimentLabel.VERY_BULLISH
            normalizedScore >= 0.15f -> SentimentLabel.BULLISH
            normalizedScore <= -0.5f -> SentimentLabel.VERY_BEARISH
            normalizedScore <= -0.15f -> SentimentLabel.BEARISH
            else -> SentimentLabel.NEUTRAL
        }

        return SentimentScore(normalizedScore, label, confidence)
    }

    fun parseArticles(articles: List<GNewsArticle>, relatedSymbols: List<String>): List<NewsItem> {
        return articles.mapNotNull { article ->
            val title = article.title ?: return@mapNotNull null
            val combinedText = "$title. ${article.description ?: ""} ${article.content ?: ""}"
            val sentiment = analyze(combinedText)
            val publishedAt = parseDate(article.publishedAt)
            NewsItem(
                id = UUID.randomUUID().toString(),
                title = title,
                summary = article.description ?: "",
                source = article.source?.name ?: "Unknown",
                url = article.url ?: "",
                publishedAt = publishedAt,
                sentiment = sentiment,
                relatedSymbols = relatedSymbols
            )
        }
    }

    private fun countOccurrences(text: String, phrase: String): Int {
        var count = 0
        var idx = 0
        while (true) {
            idx = text.indexOf(phrase, idx)
            if (idx == -1) break
            count++
            idx += phrase.length
        }
        return count
    }

    private fun parseDate(dateStr: String?): Long {
        if (dateStr == null) return System.currentTimeMillis()
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
            sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}

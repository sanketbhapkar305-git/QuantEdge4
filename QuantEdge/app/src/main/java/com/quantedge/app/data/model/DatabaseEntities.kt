package com.quantedge.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val exchange: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "portfolio")
data class PortfolioEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val exchange: String,
    val quantity: Int,
    val buyPrice: Double,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_stocks")
@TypeConverters(StringListConverter::class)
data class CachedStockEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val exchange: String,
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
    val peRatio: Double,
    val eps: Double,
    val dividendYield: Double,
    val bookValue: Double,
    val priceToBook: Double,
    val roe: Double,
    val debtToEquity: Double,
    val revenueGrowth: Double,
    val profitMargin: Double,
    val cachedAt: Long = System.currentTimeMillis()
)

class StringListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        gson.fromJson(value, object : TypeToken<List<String>>() {}.type)
}

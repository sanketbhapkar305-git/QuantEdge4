package com.quantedge.app.data.api

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quantedge.app.data.model.CachedStockEntity
import com.quantedge.app.data.model.PortfolioEntity
import com.quantedge.app.data.model.StringListConverter
import com.quantedge.app.data.model.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getAllWatchlist(): Flow<List<WatchlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWatchlist(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE symbol = :symbol")
    suspend fun removeFromWatchlist(symbol: String)

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE symbol = :symbol)")
    suspend fun isInWatchlist(symbol: String): Boolean
}

@Dao
interface PortfolioDao {
    @Query("SELECT * FROM portfolio ORDER BY addedAt DESC")
    fun getAllPortfolio(): Flow<List<PortfolioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToPortfolio(item: PortfolioEntity)

    @Query("DELETE FROM portfolio WHERE symbol = :symbol")
    suspend fun removeFromPortfolio(symbol: String)

    @Query("SELECT EXISTS(SELECT 1 FROM portfolio WHERE symbol = :symbol)")
    suspend fun isInPortfolio(symbol: String): Boolean
}

@Dao
interface StockCacheDao {
    @Query("SELECT * FROM cached_stocks WHERE symbol = :symbol")
    suspend fun getCachedStock(symbol: String): CachedStockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cacheStock(stock: CachedStockEntity)

    @Query("DELETE FROM cached_stocks WHERE cachedAt < :expiryTime")
    suspend fun clearExpiredCache(expiryTime: Long)

    @Query("SELECT * FROM cached_stocks ORDER BY cachedAt DESC LIMIT 50")
    suspend fun getRecentlyCached(): List<CachedStockEntity>
}

@Database(
    entities = [WatchlistEntity::class, PortfolioEntity::class, CachedStockEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(StringListConverter::class)
abstract class QuantEdgeDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun portfolioDao(): PortfolioDao
    abstract fun stockCacheDao(): StockCacheDao

    companion object {
        const val DATABASE_NAME = "quantedge.db"
    }
}

package com.quantedge.app.di

import android.content.Context
import androidx.room.Room
import com.google.gson.GsonBuilder
import com.quantedge.app.data.api.Database
import com.quantedge.app.data.api.NewsApi
import com.quantedge.app.data.api.PortfolioDao
import com.quantedge.app.data.api.QuantEdgeDatabase
import com.quantedge.app.data.api.StockCacheDao
import com.quantedge.app.data.api.WatchlistDao
import com.quantedge.app.data.api.YahooFinanceApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.NONE
            })
            .build()
    }

    @Provides
    @Singleton
    @Named("yahoo")
    fun provideYahooRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val gson = GsonBuilder().setLenient().create()
        return Retrofit.Builder()
            .baseUrl("https://query1.finance.yahoo.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    @Named("gnews")
    fun provideGNewsRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://gnews.io/api/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideYahooFinanceApi(@Named("yahoo") retrofit: Retrofit): YahooFinanceApi =
        retrofit.create(YahooFinanceApi::class.java)

    @Provides
    @Singleton
    fun provideNewsApi(@Named("gnews") retrofit: Retrofit): NewsApi =
        retrofit.create(NewsApi::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): QuantEdgeDatabase =
        Room.databaseBuilder(context, QuantEdgeDatabase::class.java, QuantEdgeDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideWatchlistDao(db: QuantEdgeDatabase): WatchlistDao = db.watchlistDao()

    @Provides
    @Singleton
    fun providePortfolioDao(db: QuantEdgeDatabase): PortfolioDao = db.portfolioDao()

    @Provides
    @Singleton
    fun provideStockCacheDao(db: QuantEdgeDatabase): StockCacheDao = db.stockCacheDao()
}

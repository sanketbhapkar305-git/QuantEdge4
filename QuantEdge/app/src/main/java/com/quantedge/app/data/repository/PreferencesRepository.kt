package com.quantedge.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.quantedge.app.domain.model.AnalysisConfig
import com.quantedge.app.domain.model.AnalysisFactor
import com.quantedge.app.domain.model.RiskLevel
import com.quantedge.app.domain.model.TradingMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "quantedge_prefs")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val KEY_NEWS_API_KEY = stringPreferencesKey("news_api_key")
    private val KEY_TRADING_MODE = stringPreferencesKey("trading_mode")
    private val KEY_RISK_LEVEL = stringPreferencesKey("risk_level")
    private val KEY_ENABLED_FACTORS = stringSetPreferencesKey("enabled_factors")

    val analysisConfig: Flow<AnalysisConfig> = context.dataStore.data.map { prefs ->
        val tradingMode = runCatching {
            TradingMode.valueOf(prefs[KEY_TRADING_MODE] ?: TradingMode.SWING_TRADING.name)
        }.getOrDefault(TradingMode.SWING_TRADING)

        val riskLevel = runCatching {
            RiskLevel.valueOf(prefs[KEY_RISK_LEVEL] ?: RiskLevel.MODERATE.name)
        }.getOrDefault(RiskLevel.MODERATE)

        val enabledFactors = prefs[KEY_ENABLED_FACTORS]?.mapNotNull { name ->
            runCatching { AnalysisFactor.valueOf(name) }.getOrNull()
        }?.toSet() ?: setOf(
            AnalysisFactor.TECHNICAL_RSI,
            AnalysisFactor.TECHNICAL_MACD,
            AnalysisFactor.TECHNICAL_MOVING_AVERAGE,
            AnalysisFactor.FUNDAMENTAL_PE,
            AnalysisFactor.NEWS_SENTIMENT
        )

        AnalysisConfig(
            enabledFactors = enabledFactors,
            tradingMode = tradingMode,
            riskTolerance = riskLevel
        )
    }

    val newsApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_NEWS_API_KEY] ?: ""
    }

    suspend fun saveAnalysisConfig(config: AnalysisConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TRADING_MODE] = config.tradingMode.name
            prefs[KEY_RISK_LEVEL] = config.riskTolerance.name
            prefs[KEY_ENABLED_FACTORS] = config.enabledFactors.map { it.name }.toSet()
        }
    }

    suspend fun saveNewsApiKey(apiKey: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NEWS_API_KEY] = apiKey
        }
    }

    suspend fun toggleFactor(factor: AnalysisFactor, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_ENABLED_FACTORS]?.toMutableSet() ?: mutableSetOf(
                AnalysisFactor.TECHNICAL_RSI.name,
                AnalysisFactor.TECHNICAL_MACD.name,
                AnalysisFactor.TECHNICAL_MOVING_AVERAGE.name,
                AnalysisFactor.FUNDAMENTAL_PE.name,
                AnalysisFactor.NEWS_SENTIMENT.name
            )
            if (enabled) current.add(factor.name) else current.remove(factor.name)
            prefs[KEY_ENABLED_FACTORS] = current
        }
    }
}

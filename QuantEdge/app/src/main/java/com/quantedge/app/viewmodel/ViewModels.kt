package com.quantedge.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quantedge.app.data.api.NewsApi
import com.quantedge.app.data.repository.PreferencesRepository
import com.quantedge.app.data.repository.Result
import com.quantedge.app.data.repository.StockRepository
import com.quantedge.app.domain.model.*
import com.quantedge.app.domain.usecase.AnalysisEngine
import com.quantedge.app.domain.usecase.SentimentAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = false,
    val marketIndices: List<MarketIndex> = emptyList(),
    val topGainers: List<Stock> = emptyList(),
    val topLosers: List<Stock> = emptyList(),
    val watchlist: List<Stock> = emptyList(),
    val recentSignals: List<TradingSignal> = emptyList(),
    val error: String? = null,
    val lastRefresh: Long = 0L
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    private val preferencesRepository: PreferencesRepository,
    private val analysisEngine: AnalysisEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    init {
        loadDashboard()
        startAutoRefresh()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            // Load market indices
            when (val result = stockRepository.getMarketIndices()) {
                is Result.Success -> _uiState.update { it.copy(marketIndices = result.data) }
                is Result.Error -> _uiState.update { it.copy(error = result.message) }
                else -> {}
            }
            // Load popular stocks subset for dashboard
            val popularSubset = stockRepository.popularStocks.take(15)
            val loaded = popularSubset.mapNotNull { (symbol, _) ->
                when (val r = stockRepository.getStock(symbol)) {
                    is Result.Success -> r.data
                    else -> null
                }
            }
            val gainers = loaded.sortedByDescending { it.changePercent }.take(5)
            val losers = loaded.sortedBy { it.changePercent }.take(5)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    topGainers = gainers,
                    topLosers = losers,
                    lastRefresh = System.currentTimeMillis()
                )
            }
        }
    }

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                delay(60_000L) // refresh every minute during market hours
                loadDashboard()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}

data class StockDetailUiState(
    val isLoading: Boolean = false,
    val stock: Stock? = null,
    val historicalData: HistoricalData? = null,
    val chartRange: String = "1mo",
    val news: List<NewsItem> = emptyList(),
    val signal: TradingSignal? = null,
    val isInWatchlist: Boolean = false,
    val analysisConfig: AnalysisConfig = AnalysisConfig(),
    val isAnalyzing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class StockDetailViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    private val preferencesRepository: PreferencesRepository,
    private val analysisEngine: AnalysisEngine,
    private val sentimentAnalyzer: SentimentAnalyzer,
    private val newsApi: NewsApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockDetailUiState())
    val uiState: StateFlow<StockDetailUiState> = _uiState.asStateFlow()

    private var currentSymbol: String = ""

    init {
        viewModelScope.launch {
            preferencesRepository.analysisConfig.collectLatest { config ->
                _uiState.update { it.copy(analysisConfig = config) }
            }
        }
    }

    fun loadStock(symbol: String) {
        currentSymbol = symbol
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val isInWatchlist = stockRepository.isInWatchlist(symbol)
            _uiState.update { it.copy(isInWatchlist = isInWatchlist) }

            when (val result = stockRepository.getStock(symbol)) {
                is Result.Success -> {
                    _uiState.update { it.copy(stock = result.data, isLoading = false) }
                    loadHistory(symbol, _uiState.value.chartRange)
                    loadNewsAndAnalyze(result.data)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                else -> {}
            }
        }
    }

    fun setChartRange(range: String) {
        _uiState.update { it.copy(chartRange = range) }
        loadHistory(currentSymbol, range)
    }

    private fun loadHistory(symbol: String, range: String) {
        viewModelScope.launch {
            when (val result = stockRepository.getHistoricalData(symbol, range)) {
                is Result.Success -> {
                    _uiState.update { it.copy(historicalData = result.data) }
                    _uiState.value.stock?.let { analyzeStock(it) }
                }
                else -> {}
            }
        }
    }

    private fun loadNewsAndAnalyze(stock: Stock) {
        viewModelScope.launch {
            try {
                val apiKey = preferencesRepository.newsApiKey
                // News loading happens when API key is provided; otherwise skip
                analyzeStock(stock)
            } catch (e: Exception) {
                analyzeStock(stock)
            }
        }
    }

    fun analyzeStock(stock: Stock) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true) }
            val config = _uiState.value.analysisConfig
            val historicalData = _uiState.value.historicalData
            val news = _uiState.value.news
            val signal = analysisEngine.analyzeStock(stock, historicalData, news, config)
            _uiState.update { it.copy(signal = signal, isAnalyzing = false) }
        }
    }

    fun toggleWatchlist() {
        viewModelScope.launch {
            val stock = _uiState.value.stock ?: return@launch
            if (_uiState.value.isInWatchlist) {
                stockRepository.removeFromWatchlist(stock.symbol)
                _uiState.update { it.copy(isInWatchlist = false) }
            } else {
                stockRepository.addToWatchlist(stock.symbol, stock.name, stock.exchange)
                _uiState.update { it.copy(isInWatchlist = true) }
            }
        }
    }

    fun toggleFactor(factor: AnalysisFactor, enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.toggleFactor(factor, enabled)
            _uiState.value.stock?.let { analyzeStock(it) }
        }
    }
}

data class SearchUiState(
    val query: String = "",
    val results: List<Stock> = emptyList(),
    val isSearching: Boolean = false,
    val recentSearches: List<String> = emptyList()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val stockRepository: StockRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        if (query.length >= 2) searchStocks(query)
        else _uiState.update { it.copy(results = emptyList()) }
    }

    private fun searchStocks(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // debounce
            _uiState.update { it.copy(isSearching = true) }
            val matching = stockRepository.popularStocks
                .filter { (symbol, name) ->
                    name.contains(query, ignoreCase = true) ||
                    symbol.contains(query, ignoreCase = true)
                }
                .take(10)
            val loaded = matching.mapNotNull { (symbol, _) ->
                when (val r = stockRepository.getStock(symbol)) {
                    is Result.Success -> r.data
                    else -> null
                }
            }
            _uiState.update { it.copy(results = loaded, isSearching = false) }
        }
    }
}

data class SettingsUiState(
    val analysisConfig: AnalysisConfig = AnalysisConfig(),
    val newsApiKey: String = "",
    val saved: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.analysisConfig.collectLatest { config ->
                _uiState.update { it.copy(analysisConfig = config) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.newsApiKey.collectLatest { key ->
                _uiState.update { it.copy(newsApiKey = key) }
            }
        }
    }

    fun setTradingMode(mode: TradingMode) {
        viewModelScope.launch {
            val config = _uiState.value.analysisConfig.copy(tradingMode = mode)
            preferencesRepository.saveAnalysisConfig(config)
        }
    }

    fun setRiskLevel(level: RiskLevel) {
        viewModelScope.launch {
            val config = _uiState.value.analysisConfig.copy(riskTolerance = level)
            preferencesRepository.saveAnalysisConfig(config)
        }
    }

    fun toggleFactor(factor: AnalysisFactor, enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.toggleFactor(factor, enabled)
        }
    }

    fun saveNewsApiKey(key: String) {
        viewModelScope.launch {
            preferencesRepository.saveNewsApiKey(key)
            _uiState.update { it.copy(saved = true) }
            delay(2000)
            _uiState.update { it.copy(saved = false) }
        }
    }
}

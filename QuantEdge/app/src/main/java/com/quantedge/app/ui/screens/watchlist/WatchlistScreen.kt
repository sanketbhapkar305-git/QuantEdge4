package com.quantedge.app.ui.screens.watchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quantedge.app.data.repository.Result
import com.quantedge.app.data.repository.StockRepository
import com.quantedge.app.domain.model.Stock
import com.quantedge.app.ui.components.*
import com.quantedge.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchlistUiState(
    val isLoading: Boolean = false,
    val stocks: List<Stock> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val stockRepository: StockRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WatchlistUiState())
    val uiState: StateFlow<WatchlistUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            stockRepository.getWatchlist().collectLatest { watchlistItems ->
                _uiState.update { it.copy(isLoading = true) }
                val loaded = watchlistItems.mapNotNull { item ->
                    when (val r = stockRepository.getStock(item.symbol)) {
                        is Result.Success -> r.data
                        else -> null
                    }
                }
                _uiState.update { it.copy(stocks = loaded, isLoading = false) }
            }
        }
    }

    fun removeFromWatchlist(symbol: String) {
        viewModelScope.launch {
            stockRepository.removeFromWatchlist(symbol)
        }
    }
}

@Composable
fun WatchlistScreen(
    onStockClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column {
            QuantEdgeTopBar(title = "Watchlist", onBack = onBack)

            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = QuantBlue
                )
            }

            if (uiState.stocks.isEmpty() && !uiState.isLoading) {
                EmptyWatchlist()
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                    items(uiState.stocks, key = { it.symbol }) { stock ->
                        SwipeToDismissStockCard(
                            stock = stock,
                            onStockClick = { onStockClick(stock.symbol) },
                            onRemove = { viewModel.removeFromWatchlist(stock.symbol) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissStockCard(
    stock: Stock,
    onStockClick: () -> Unit,
    onRemove: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRemove()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = QuantRed
                )
            }
        },
        content = {
            StockCard(
                stock = stock,
                onClick = onStockClick,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    )
}

@Composable
private fun EmptyWatchlist() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.BookmarkBorder,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Your watchlist is empty",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Add stocks from the screener or stock detail page",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

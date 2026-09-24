package com.quantedge.app.ui.screens.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quantedge.app.domain.model.Stock
import com.quantedge.app.ui.components.*
import com.quantedge.app.ui.theme.*
import com.quantedge.app.viewmodel.SearchViewModel

@Composable
fun ScreenerScreen(
    onStockClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column {
            QuantEdgeTopBar(
                title = "Stock Screener",
                onBack = onBack
            )

            // Search Input
            SearchInputField(
                query = uiState.query,
                onQueryChange = { viewModel.onQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (uiState.isSearching) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = QuantBlue
                )
            }

            if (uiState.query.isEmpty()) {
                // Show search hints
                SearchHints(
                    onHintClick = { viewModel.onQueryChange(it) }
                )
            } else {
                // Show search results
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    if (uiState.results.isEmpty() && !uiState.isSearching) {
                        item {
                            EmptySearchState(
                                query = uiState.query,
                                modifier = Modifier.padding(32.dp)
                            )
                        }
                    }
                    items(uiState.results) { stock ->
                        StockSearchCard(
                            stock = stock,
                            onClick = { onStockClick(stock.symbol) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchInputField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = {
            Text("Search by name or symbol (e.g. RELIANCE, TCS...)", color = TextMuted)
        },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = SurfaceVariantDark,
            unfocusedContainerColor = SurfaceVariantDark,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedIndicatorColor = QuantBlue,
            unfocusedIndicatorColor = BorderDark,
            cursorColor = QuantBlue
        ),
        shape = RoundedCornerShape(16.dp),
        singleLine = true
    )
}

@Composable
private fun SearchHints(onHintClick: (String) -> Unit) {
    val popularSearches = listOf(
        "RELIANCE" to "Reliance Industries",
        "TCS" to "Tata Consultancy Services",
        "INFY" to "Infosys",
        "HDFCBANK" to "HDFC Bank",
        "SBIN" to "State Bank of India",
        "BAJFINANCE" to "Bajaj Finance",
        "TATAMOTORS" to "Tata Motors",
        "WIPRO" to "Wipro",
        "SUNPHARMA" to "Sun Pharma",
        "MARUTI" to "Maruti Suzuki"
    )

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Popular Stocks",
            style = MaterialTheme.typography.titleSmall,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(12.dp))
        popularSearches.forEach { (symbol, name) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onHintClick(symbol) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = QuantBlue.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(symbol, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text(name, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            HorizontalDivider(color = DividerDark.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun StockSearchCard(
    stock: Stock,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(QuantPurpleSubtle),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stock.symbol.take(2).trimEnd('.'),
                    style = MaterialTheme.typography.labelMedium,
                    color = QuantBlueLight,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stock.symbol.replace(".NS", "").replace(".BO", ""),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(4.dp), color = SurfaceVariantDark) {
                        Text(
                            text = stock.exchange,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = QuantBlue
                        )
                    }
                }
                Text(
                    text = stock.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (stock.sector != "Unknown") {
                    Text(
                        text = stock.sector,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stock.priceFormatted,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                val changeColor = if (stock.isPositive) QuantGreen else QuantRed
                Text(
                    text = "${if (stock.isPositive) "▲" else "▼"} ${stock.changeFormatted}",
                    style = MaterialTheme.typography.bodySmall,
                    color = changeColor
                )
            }
        }
    }
}

@Composable
private fun EmptySearchState(query: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "No results for \"$query\"",
            style = MaterialTheme.typography.titleSmall,
            color = TextSecondary
        )
        Text(
            "Try a different symbol or company name",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

package com.quantedge.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quantedge.app.domain.model.Stock
import com.quantedge.app.ui.components.*
import com.quantedge.app.ui.theme.*
import com.quantedge.app.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    onStockClick: (String) -> Unit,
    onScreenerClick: () -> Unit,
    onWatchlistClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header
            item {
                DashboardHeader(onSettingsClick = onSettingsClick, onWatchlistClick = onWatchlistClick)
            }

            // Search Bar
            item {
                SearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    onSearchClick = onScreenerClick
                )
            }

            // Market Indices
            if (uiState.marketIndices.isNotEmpty()) {
                item {
                    SectionHeader(title = "Market Overview", icon = Icons.Default.TrendingUp)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.marketIndices) { index ->
                            MarketIndexCard(
                                index = index,
                                modifier = Modifier.width(160.dp)
                            )
                        }
                    }
                }
            }

            // Quick Actions
            item {
                QuickActionsRow(
                    onScreenerClick = onScreenerClick,
                    onWatchlistClick = onWatchlistClick,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Top Gainers
            if (uiState.topGainers.isNotEmpty()) {
                item {
                    SectionHeader(title = "Top Gainers 🚀", icon = Icons.Default.TrendingUp)
                }
                items(uiState.topGainers.take(5)) { stock ->
                    StockCard(
                        stock = stock,
                        onClick = { onStockClick(stock.symbol) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // Top Losers
            if (uiState.topLosers.isNotEmpty()) {
                item {
                    SectionHeader(title = "Top Losers 📉", icon = Icons.Default.TrendingDown)
                }
                items(uiState.topLosers.take(5)) { stock ->
                    StockCard(
                        stock = stock,
                        onClick = { onStockClick(stock.symbol) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // Loading State
            if (uiState.isLoading) {
                items(5) {
                    LoadingShimmer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // Error
            if (uiState.error != null) {
                item {
                    ErrorCard(
                        message = uiState.error!!,
                        onRetry = { viewModel.loadDashboard() },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Footer
            if (uiState.lastRefresh > 0) {
                item {
                    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    Text(
                        text = "Last updated: ${sdf.format(Date(uiState.lastRefresh))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    onSettingsClick: () -> Unit,
    onWatchlistClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SurfaceDark, BackgroundDark)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Quant",
                            style = MaterialTheme.typography.displayMedium,
                            color = QuantBlue,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Edge",
                            style = MaterialTheme.typography.displayMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = "Smart Stock Intelligence",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        letterSpacing = 1.5.sp
                    )
                }
                Row {
                    IconButton(onClick = onWatchlistClick) {
                        Icon(
                            imageVector = Icons.Default.Bookmarks,
                            contentDescription = "Watchlist",
                            tint = QuantBlue
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            // Live Market Status
            LiveMarketBadge()
        }
    }
}

@Composable
private fun LiveMarketBadge() {
    val isMarketOpen = isMarketCurrentlyOpen()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isMarketOpen) QuantGreenSubtle else SurfaceVariantDark
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isMarketOpen) QuantGreen else TextMuted)
            )
            Text(
                text = if (isMarketOpen) "MARKET OPEN" else "MARKET CLOSED",
                style = MaterialTheme.typography.labelSmall,
                color = if (isMarketOpen) QuantGreen else TextMuted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun SearchBar(modifier: Modifier = Modifier, onSearchClick: () -> Unit) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onSearchClick),
        color = SurfaceVariantDark,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Search BSE / NSE stocks...",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = QuantBlue,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

@Composable
private fun QuickActionsRow(
    onScreenerClick: () -> Unit,
    onWatchlistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionButton(
            icon = Icons.Default.FilterList,
            label = "Screener",
            color = QuantBlue,
            bgColor = QuantPurpleSubtle,
            onClick = onScreenerClick,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = Icons.Default.Star,
            label = "Watchlist",
            color = QuantGold,
            bgColor = QuantGoldSubtle,
            onClick = onWatchlistClick,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = Icons.Default.TrendingUp,
            label = "Top Signals",
            color = QuantGreen,
            bgColor = QuantGreenSubtle,
            onClick = onScreenerClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    bgColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .height(72.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = QuantRedSubtle),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Unable to load data", style = MaterialTheme.typography.titleSmall, color = QuantRed)
            Spacer(Modifier.height(4.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRetry) {
                Text("Retry", color = QuantBlue)
            }
        }
    }
}

private fun isMarketCurrentlyOpen(): Boolean {
    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
    val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val minute = cal.get(java.util.Calendar.MINUTE)
    val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
    val isWeekday = dayOfWeek in java.util.Calendar.MONDAY..java.util.Calendar.FRIDAY
    val afterOpen = hour > 9 || (hour == 9 && minute >= 15)
    val beforeClose = hour < 15 || (hour == 15 && minute <= 30)
    return isWeekday && afterOpen && beforeClose
}

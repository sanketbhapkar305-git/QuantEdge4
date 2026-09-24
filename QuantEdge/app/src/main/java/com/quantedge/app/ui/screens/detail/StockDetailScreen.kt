package com.quantedge.app.ui.screens.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quantedge.app.domain.model.*
import com.quantedge.app.ui.components.*
import com.quantedge.app.ui.theme.*
import com.quantedge.app.viewmodel.StockDetailViewModel

@Composable
fun StockDetailScreen(
    symbol: String,
    onBack: () -> Unit,
    viewModel: StockDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAnalysisPanel by remember { mutableStateOf(false) }

    LaunchedEffect(symbol) { viewModel.loadStock(symbol) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        if (uiState.isLoading) {
            LoadingContent()
        } else if (uiState.stock != null) {
            val stock = uiState.stock!!
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Top Bar
                item {
                    DetailTopBar(
                        stock = stock,
                        isInWatchlist = uiState.isInWatchlist,
                        onBack = onBack,
                        onToggleWatchlist = { viewModel.toggleWatchlist() }
                    )
                }

                // Price Hero Section
                item {
                    PriceHeroSection(stock = stock)
                }

                // Chart Range Selector
                item {
                    ChartRangeSelector(
                        selectedRange = uiState.chartRange,
                        onRangeSelect = { viewModel.setChartRange(it) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Mini Price Chart (Canvas-based sparkline)
                uiState.historicalData?.let { hist ->
                    item {
                        PriceSparklineChart(
                            closes = hist.closes,
                            isPositive = stock.isPositive,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .padding(horizontal = 16.dp)
                        )
                    }
                }

                // Trading Signal Card
                item {
                    Spacer(Modifier.height(12.dp))
                    if (uiState.isAnalyzing) {
                        AnalyzingIndicator(modifier = Modifier.padding(horizontal = 16.dp))
                    } else {
                        uiState.signal?.let { signal ->
                            SignalBadge(
                                signal = signal,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            ConfidenceBar(
                                confidence = signal.confidence,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                // Price Targets
                uiState.signal?.let { signal ->
                    item {
                        PriceTargetsCard(
                            signal = signal,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Analysis Rationale
                uiState.signal?.let { signal ->
                    if (signal.rationale.isNotEmpty()) {
                        item {
                            RationaleCard(
                                rationale = signal.rationale,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                // Analysis Factor Controls (Checkboxes)
                item {
                    Spacer(Modifier.height(12.dp))
                    AnalysisFactorToggle(
                        expanded = showAnalysisPanel,
                        onToggle = { showAnalysisPanel = !showAnalysisPanel }
                    )
                    AnimatedVisibility(
                        visible = showAnalysisPanel,
                        enter = expandVertically() + fadeIn()
                    ) {
                        AnalysisFactorPanel(
                            config = uiState.analysisConfig,
                            onToggleFactor = { factor, enabled ->
                                viewModel.toggleFactor(factor, enabled)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // Fundamentals Card
                item {
                    FundamentalsCard(
                        stock = stock,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Key Statistics
                item {
                    KeyStatisticsGrid(
                        stock = stock,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // News items placeholder
                uiState.news.let { newsList ->
                    if (newsList.isNotEmpty()) {
                        item {
                            SectionDivider(title = "Recent News")
                        }
                        items(newsList.take(5)) { news ->
                            NewsCard(
                                news = news,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        } else if (uiState.error != null) {
            ErrorState(message = uiState.error!!, onRetry = { viewModel.loadStock(symbol) })
        }
    }
}

@Composable
private fun DetailTopBar(
    stock: Stock,
    isInWatchlist: Boolean,
    onBack: () -> Unit,
    onToggleWatchlist: () -> Unit
) {
    Surface(color = BackgroundDark) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stock.symbol.replace(".NS", "").replace(".BO", ""),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = stock.exchange,
                    style = MaterialTheme.typography.bodySmall,
                    color = QuantBlue
                )
            }
            IconButton(onClick = onToggleWatchlist) {
                Icon(
                    imageVector = if (isInWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = if (isInWatchlist) "Remove from watchlist" else "Add to watchlist",
                    tint = if (isInWatchlist) QuantGold else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun PriceHeroSection(stock: Stock) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SurfaceDark, BackgroundDark)
                )
            )
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = stock.name,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stock.priceFormatted,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val color = if (stock.isPositive) QuantGreen else QuantRed
                val bg = if (stock.isPositive) QuantGreenSubtle else QuantRedSubtle
                Surface(shape = RoundedCornerShape(8.dp), color = bg) {
                    Text(
                        text = "${if (stock.isPositive) "▲" else "▼"} ${stock.changeFormatted}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.titleSmall,
                        color = color,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "₹${String.format("%.2f", stock.change)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (stock.isPositive) QuantGreen else QuantRed
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stock.sector,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            Spacer(Modifier.height(12.dp))
            // Day Range
            DayRangeBar(
                current = stock.currentPrice,
                low = stock.dayLow,
                high = stock.dayHigh
            )
        }
    }
}

@Composable
private fun DayRangeBar(current: Double, low: Double, high: Double) {
    if (low <= 0 || high <= 0 || low >= high) return
    val progress = ((current - low) / (high - low)).toFloat().coerceIn(0f, 1f)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("L: ₹${String.format("%.0f", low)}", style = MaterialTheme.typography.bodySmall, color = QuantRed)
            Text("Day Range", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Text("H: ₹${String.format("%.0f", high)}", style = MaterialTheme.typography.bodySmall, color = QuantGreen)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.horizontalGradient(colors = listOf(QuantRed, QuantOrange, QuantGreen))
                )
        ) {
            Box(
                modifier = Modifier
                    .offset(x = (progress * 100).dp - 4.dp)
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White)
                    .align(Alignment.CenterStart)
            )
        }
    }
}

@Composable
private fun ChartRangeSelector(
    selectedRange: String,
    onRangeSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ranges = listOf("1d", "5d", "1mo", "3mo", "1y", "5y")
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ranges.forEach { range ->
            val selected = range == selectedRange
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (selected) QuantBlue else SurfaceVariantDark,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onRangeSelect(range) }
            ) {
                Text(
                    text = range.uppercase(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) Color.Black else TextSecondary,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun PriceSparklineChart(
    closes: List<Double>,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    if (closes.isEmpty()) return
    val color = if (isPositive) QuantGreen else QuantRed
    val bgColor = if (isPositive) QuantGreenSubtle else QuantRedSubtle

    androidx.compose.foundation.Canvas(modifier = modifier.background(bgColor, RoundedCornerShape(16.dp))) {
        val minVal = closes.min()
        val maxVal = closes.max()
        val range = maxVal - minVal
        if (range == 0.0) return@Canvas

        val width = size.width
        val height = size.height
        val stepX = width / (closes.size - 1)

        val path = androidx.compose.ui.graphics.Path()
        val fillPath = androidx.compose.ui.graphics.Path()

        closes.forEachIndexed { i, close ->
            val x = i * stepX
            val y = height - ((close - minVal) / range * height * 0.85f + height * 0.075f).toFloat()
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(width, height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.25f), color.copy(alpha = 0f))
            )
        )
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
        )
    }
}

@Composable
private fun AnalyzingIndicator(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = QuantPurpleSubtle),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = QuantPurple,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "Analyzing with active factors...",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun PriceTargetsCard(signal: TradingSignal, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Price Targets",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            PriceTargetRow("Entry Price", signal.entryPrice, QuantBlue)
            Spacer(Modifier.height(8.dp))
            PriceTargetRow("Target Price", signal.targetPrice, QuantGreen)
            Spacer(Modifier.height(8.dp))
            PriceTargetRow("Stop Loss", signal.stopLoss, QuantRed)
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = DividerDark)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Risk/Reward", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    Text(
                        text = "1 : ${String.format("%.2f", signal.riskRewardRatio)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (signal.riskRewardRatio >= 2) QuantGreen else QuantOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Mode", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    Text(
                        text = signal.tradingMode.name.replace("_", " "),
                        style = MaterialTheme.typography.titleSmall,
                        color = QuantBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RationaleCard(rationale: List<String>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Signal Rationale",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            rationale.forEachIndexed { i, reason ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(QuantBlue)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
                if (i < rationale.lastIndex) Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun AnalysisFactorToggle(expanded: Boolean, onToggle: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onToggle),
        color = SurfaceVariantDark,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = QuantPurple,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Analysis Factors",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun AnalysisFactorPanel(
    config: AnalysisConfig,
    onToggleFactor: (AnalysisFactor, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Technical Factors
            Text(
                text = "TECHNICAL",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            AnalysisFactor.values().filter { it.name.startsWith("TECHNICAL") }.forEach { factor ->
                FactorCheckbox(
                    factor = factor,
                    enabled = config.enabledFactors.contains(factor),
                    onToggle = { onToggleFactor(factor, it) },
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            // Fundamental Factors
            Text(
                text = "FUNDAMENTAL",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            AnalysisFactor.values().filter { it.name.startsWith("FUNDAMENTAL") }.forEach { factor ->
                FactorCheckbox(
                    factor = factor,
                    enabled = config.enabledFactors.contains(factor),
                    onToggle = { onToggleFactor(factor, it) },
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            // Other Factors
            Text(
                text = "MARKET & SENTIMENT",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            AnalysisFactor.values().filter {
                !it.name.startsWith("TECHNICAL") && !it.name.startsWith("FUNDAMENTAL")
            }.forEach { factor ->
                FactorCheckbox(
                    factor = factor,
                    enabled = config.enabledFactors.contains(factor),
                    onToggle = { onToggleFactor(factor, it) },
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun FundamentalsCard(stock: Stock, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Fundamentals",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                FundamentalItem("P/E Ratio", stock.peRatio?.let { String.format("%.1f", it) } ?: "N/A", modifier = Modifier.weight(1f))
                FundamentalItem("EPS", stock.eps?.let { "₹${String.format("%.2f", it)}" } ?: "N/A", modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                FundamentalItem("ROE", stock.roe?.let { "${String.format("%.1f", it)}%" } ?: "N/A", modifier = Modifier.weight(1f))
                FundamentalItem("D/E Ratio", stock.debtToEquity?.let { String.format("%.2f", it) } ?: "N/A", modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                FundamentalItem("Rev Growth", stock.revenueGrowth?.let { "${String.format("%.1f", it)}%" } ?: "N/A", modifier = Modifier.weight(1f))
                FundamentalItem("Profit Margin", stock.profitMargin?.let { "${String.format("%.1f", it)}%" } ?: "N/A", modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                FundamentalItem("Book Value", stock.bookValue?.let { "₹${String.format("%.2f", it)}" } ?: "N/A", modifier = Modifier.weight(1f))
                FundamentalItem("P/B Ratio", stock.priceToBook?.let { String.format("%.2f", it) } ?: "N/A", modifier = Modifier.weight(1f))
            }
            if (stock.dividendYield != null) {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    FundamentalItem("Div Yield", "${String.format("%.2f", stock.dividendYield)}%", modifier = Modifier.weight(1f))
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FundamentalItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun KeyStatisticsGrid(stock: Stock, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Market Data",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem("52W High", "₹${String.format("%,.0f", stock.high52Week)}", modifier = Modifier.weight(1f))
                StatItem("52W Low", "₹${String.format("%,.0f", stock.low52Week)}", modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem("Volume", formatVolume(stock.volume), modifier = Modifier.weight(1f))
                StatItem("Avg Volume", formatVolume(stock.avgVolume), modifier = Modifier.weight(1f))
            }
            if (stock.marketCap > 0) {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatItem("Market Cap", formatMarketCap(stock.marketCap), modifier = Modifier.weight(1f))
                    StatItem("Sector", stock.sector.take(15), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Text(value, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun NewsCard(news: NewsItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = news.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                SentimentChip(label = news.sentiment.label)
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(news.source, style = MaterialTheme.typography.bodySmall, color = QuantBlue)
                Text(
                    formatTime(news.publishedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun SectionDivider(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = DividerDark)
        Text(
            text = "  $title  ",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = DividerDark)
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(80.dp))
        repeat(6) {
            LoadingShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (it == 0) 120.dp else 70.dp)
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = QuantRed,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("Failed to load stock", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = QuantBlue)
        ) {
            Text("Retry", color = Color.Black)
        }
    }
}

private fun formatVolume(vol: Long): String = when {
    vol >= 10_000_000 -> "${String.format("%.1f", vol / 10_000_000.0)}Cr"
    vol >= 100_000 -> "${String.format("%.1f", vol / 100_000.0)}L"
    vol >= 1_000 -> "${String.format("%.1f", vol / 1_000.0)}K"
    else -> vol.toString()
}

private fun formatMarketCap(cap: Long): String = when {
    cap >= 1_000_000_000_000 -> "₹${String.format("%.1f", cap / 1_000_000_000_000.0)}T"
    cap >= 10_000_000_000 -> "₹${String.format("%.1f", cap / 10_000_000_000.0)}KCr"
    cap >= 100_000_000 -> "₹${String.format("%.1f", cap / 100_000_000.0)}Cr"
    else -> "₹${cap / 100_000}L"
}

private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

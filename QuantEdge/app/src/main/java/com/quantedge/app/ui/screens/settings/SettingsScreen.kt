package com.quantedge.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quantedge.app.domain.model.*
import com.quantedge.app.ui.components.*
import com.quantedge.app.ui.theme.*
import com.quantedge.app.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var newsKeyInput by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.newsApiKey) {
        if (newsKeyInput.isEmpty() && uiState.newsApiKey.isNotEmpty()) {
            newsKeyInput = uiState.newsApiKey
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        LazyColumn(contentPadding = PaddingValues(bottom = 48.dp)) {
            item {
                QuantEdgeTopBar(title = "Settings", onBack = onBack)
            }

            // Trading Mode Section
            item {
                SettingsSectionHeader("Trading Mode", Icons.Default.ShowChart)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TradingMode.values().forEach { mode ->
                        val selected = uiState.analysisConfig.tradingMode == mode
                        ModeCard(
                            mode = mode,
                            selected = selected,
                            onClick = { viewModel.setTradingMode(mode) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Risk Level Section
            item {
                SettingsSectionHeader("Risk Tolerance", Icons.Default.Shield)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RiskLevel.values().forEach { level ->
                        val selected = uiState.analysisConfig.riskTolerance == level
                        RiskChip(
                            level = level,
                            selected = selected,
                            onClick = { viewModel.setRiskLevel(level) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Analysis Factors Section
            item {
                SettingsSectionHeader("Analysis Factors", Icons.Default.Tune)
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // Technical group
                    FactorGroupHeader("⚡ TECHNICAL INDICATORS")
                    AnalysisFactor.values().filter { it.name.startsWith("TECHNICAL") }.forEach { factor ->
                        FactorCheckbox(
                            factor = factor,
                            enabled = uiState.analysisConfig.enabledFactors.contains(factor),
                            onToggle = { viewModel.toggleFactor(factor, it) },
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    // Fundamental group
                    FactorGroupHeader("📊 FUNDAMENTAL ANALYSIS")
                    AnalysisFactor.values().filter { it.name.startsWith("FUNDAMENTAL") }.forEach { factor ->
                        FactorCheckbox(
                            factor = factor,
                            enabled = uiState.analysisConfig.enabledFactors.contains(factor),
                            onToggle = { viewModel.toggleFactor(factor, it) },
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    // Market/sentiment group
                    FactorGroupHeader("🌐 MARKET & SENTIMENT")
                    AnalysisFactor.values().filter {
                        !it.name.startsWith("TECHNICAL") && !it.name.startsWith("FUNDAMENTAL")
                    }.forEach { factor ->
                        FactorCheckbox(
                            factor = factor,
                            enabled = uiState.analysisConfig.enabledFactors.contains(factor),
                            onToggle = { viewModel.toggleFactor(factor, it) },
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                }
            }

            // News API Key
            item {
                SettingsSectionHeader("News API (Optional)", Icons.Default.Newspaper)
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "GNews API Key",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                "Get a free key at gnews.io for real-time news sentiment. 100 requests/day on free tier.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = newsKeyInput,
                                onValueChange = { newsKeyInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Enter API key...", color = TextMuted) },
                                visualTransformation = if (showApiKey) VisualTransformation.None
                                                       else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showApiKey = !showApiKey }) {
                                        Icon(
                                            imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = TextSecondary
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = QuantBlue,
                                    unfocusedBorderColor = BorderDark,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    cursorColor = QuantBlue
                                ),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.saveNewsApiKey(newsKeyInput) },
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(containerColor = QuantBlue),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (uiState.saved) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Saved!", color = androidx.compose.ui.graphics.Color.Black)
                                } else {
                                    Text("Save Key", color = androidx.compose.ui.graphics.Color.Black)
                                }
                            }
                        }
                    }
                }
            }

            // About Section
            item {
                SettingsSectionHeader("About", Icons.Default.Info)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        AboutRow("App", "QuantEdge v1.0.0")
                        HorizontalDivider(color = DividerDark, modifier = Modifier.padding(vertical = 8.dp))
                        AboutRow("Data Source", "Yahoo Finance API (Free)")
                        HorizontalDivider(color = DividerDark, modifier = Modifier.padding(vertical = 8.dp))
                        AboutRow("Analysis Engine", "On-device ML-like scoring")
                        HorizontalDivider(color = DividerDark, modifier = Modifier.padding(vertical = 8.dp))
                        AboutRow("Markets", "BSE & NSE (India)")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "⚠️ Disclaimer: QuantEdge is for informational purposes only. Not financial advice. Always do your own research before investing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = QuantOrange,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = QuantBlue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ModeCard(
    mode: TradingMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (title, subtitle, icon) = when (mode) {
        TradingMode.SWING_TRADING -> Triple("Swing", "Days–Weeks", Icons.Default.SwapHoriz)
        TradingMode.EQUITY_LONG_TERM -> Triple("Long Term", "Months–Years", Icons.Default.AccountBalance)
    }
    Card(
        modifier = modifier
            .height(88.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .then(
                if (selected) Modifier.border(1.5.dp, QuantBlue, RoundedCornerShape(14.dp))
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) QuantPurpleSubtle else SurfaceVariantDark
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) QuantBlue else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) QuantBlue else TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun RiskChip(
    level: RiskLevel,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (level) {
        RiskLevel.LOW -> Pair(QuantGreen, "Low")
        RiskLevel.MODERATE -> Pair(QuantOrange, "Moderate")
        RiskLevel.HIGH -> Pair(QuantRed, "High")
    }
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) color.copy(alpha = 0.15f) else SurfaceVariantDark,
        border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, color)
                 else androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) color else TextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun FactorGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = QuantBlue,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 6.dp, top = 2.dp)
    )
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

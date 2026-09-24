package com.quantedge.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantedge.app.domain.model.*
import com.quantedge.app.ui.theme.*

@Composable
fun StockCard(
    stock: Stock,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Stock Logo Placeholder
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    QuantBlue.copy(alpha = 0.3f),
                                    QuantPurple.copy(alpha = 0.1f)
                                )
                            )
                        ),
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
                Column {
                    Text(
                        text = stock.symbol.replace(".NS", "").replace(".BO", ""),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = stock.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 160.dp)
                    )
                    Text(
                        text = stock.exchange,
                        style = MaterialTheme.typography.labelSmall,
                        color = QuantBlue.copy(alpha = 0.7f)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stock.priceFormatted,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                val changeColor = if (stock.isPositive) QuantGreen else QuantRed
                val changeIcon = if (stock.isPositive) "▲" else "▼"
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (stock.isPositive) QuantGreenSubtle else QuantRedSubtle
                ) {
                    Text(
                        text = "$changeIcon ${stock.changeFormatted}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = changeColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun SignalBadge(
    signal: TradingSignal,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val (color, bgColor, label) = when (signal.signalType) {
        SignalType.STRONG_BUY -> Triple(QuantGreen, QuantGreenSubtle, "STRONG BUY")
        SignalType.BUY -> Triple(QuantGreen.copy(alpha = 0.85f), QuantGreenSubtle, "BUY")
        SignalType.HOLD -> Triple(QuantOrange, QuantOrangeSubtle, "HOLD")
        SignalType.SELL -> Triple(QuantRed.copy(alpha = 0.85f), QuantRedSubtle, "SELL")
        SignalType.STRONG_SELL -> Triple(QuantRed, QuantRedSubtle, "STRONG SELL")
    }

    if (compact) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = bgColor,
            modifier = modifier
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.headlineSmall,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = signal.tradingMode.name.replace("_", " "),
                        style = MaterialTheme.typography.bodySmall,
                        color = color.copy(alpha = 0.7f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${signal.confidencePercent}%",
                        style = MaterialTheme.typography.titleLarge,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "confidence",
                        style = MaterialTheme.typography.bodySmall,
                        color = color.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun ConfidenceBar(
    confidence: Float,
    modifier: Modifier = Modifier
) {
    val animatedWidth by animateFloatAsState(
        targetValue = confidence,
        animationSpec = tween(durationMillis = 800),
        label = "confidence"
    )
    val barColor = when {
        confidence >= 0.7f -> QuantGreen
        confidence >= 0.5f -> QuantOrange
        else -> QuantRed
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Signal Confidence",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                text = "${(confidence * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = barColor,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(SurfaceVariantDark)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(barColor.copy(alpha = 0.7f), barColor)
                        )
                    )
            )
        }
    }
}

@Composable
fun SentimentChip(
    label: SentimentLabel,
    modifier: Modifier = Modifier
) {
    val (color, bg, text) = when (label) {
        SentimentLabel.VERY_BULLISH -> Triple(QuantGreen, QuantGreenSubtle, "Very Bullish 🚀")
        SentimentLabel.BULLISH -> Triple(QuantGreen.copy(alpha = 0.8f), QuantGreenSubtle, "Bullish 📈")
        SentimentLabel.NEUTRAL -> Triple(QuantOrange, QuantOrangeSubtle, "Neutral ➡️")
        SentimentLabel.BEARISH -> Triple(QuantRed.copy(alpha = 0.8f), QuantRedSubtle, "Bearish 📉")
        SentimentLabel.VERY_BEARISH -> Triple(QuantRed, QuantRedSubtle, "Very Bearish 🔻")
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bg,
        modifier = modifier
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun MarketIndexCard(
    index: MarketIndex,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = index.name,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = String.format("%,.0f", index.value),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            val changeColor = if (index.isPositive) QuantGreen else QuantRed
            val arrow = if (index.isPositive) "▲" else "▼"
            Text(
                text = "$arrow ${String.format("%.2f", index.changePercent)}%",
                style = MaterialTheme.typography.bodySmall,
                color = changeColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun FactorCheckbox(
    factor: AnalysisFactor,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) QuantPurpleSubtle else SurfaceVariantDark)
            .clickable { onToggle(!enabled) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = CheckboxDefaults.colors(
                checkedColor = QuantPurple,
                checkmarkColor = TextPrimary,
                uncheckedColor = TextMuted
            )
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = factor.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) TextPrimary else TextSecondary
            )
            Text(
                text = factor.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun QuantEdgeTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        color = BackgroundDark,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
                color = TextPrimary
            )
            actions()
        }
    }
}

@Composable
fun PriceTargetRow(
    label: String,
    price: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Text(
            text = "₹${String.format("%,.2f", price)}",
            style = MaterialTheme.typography.titleSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun LoadingShimmer(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        SurfaceVariantDark,
                        CardElevated,
                        SurfaceVariantDark
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
    )
}

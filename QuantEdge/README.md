# QuantEdge — Smart Stock Intelligence

<p align="center">
  <strong>AI-powered BSE &amp; NSE stock analysis for Android</strong><br>
  Real-time data · Multi-factor analysis · Swing &amp; Equity signals
</p>

---

## Features

| Feature | Description |
|---|---|
| 📈 Real-time prices | Live BSE &amp; NSE quotes via Yahoo Finance API (free, no key required) |
| 🧠 Analysis Engine | On-device scoring across 14 configurable factors |
| ✅ Factor Checkboxes | Toggle each factor (RSI, MACD, P/E, ROE, sentiment…) per analysis |
| 🎯 Trading Signals | STRONG BUY / BUY / HOLD / SELL / STRONG SELL with confidence % |
| 📰 News Sentiment | On-device NLP sentiment scoring of financial news |
| 🔁 Swing Trading | Entry, target, stop-loss levels with risk/reward ratio |
| 📊 Equity Long-Term | Fundamental-heavy analysis for long-term investors |
| 🔖 Watchlist | Swipe-to-remove saved stocks with live prices |
| 🌙 Dark Theme | Deep space dark UI with glow accents |

---

## Screenshots (Conceptual)

```
[Dashboard]         [Stock Detail]       [Analysis Factors]
┌────────────────┐  ┌────────────────┐   ┌────────────────┐
│ QuantEdge      │  │ ← RELIANCE     │   │ ⚡ TECHNICAL   │
│ Smart Stock AI │  │ NSE            │   │ ✅ RSI         │
│ ● MARKET OPEN  │  │ ₹2,987.45      │   │ ✅ MACD        │
│                │  │ ▲ +1.23%       │   │ ☐ Bollinger    │
│ [SENSEX ▲]     │  │────────────────│   │ ✅ MA Cross    │
│ [NIFTY  ▲]     │  │   ▁▂▄▃▅▆▅▇    │   │ ☐ Volume       │
│                │  │                │   │                │
│ Top Gainers 🚀 │  │ STRONG BUY ██  │   │ 📊 FUNDAMENTAL │
│ TCS    +3.2%   │  │ Confidence: 78%│   │ ✅ P/E Ratio   │
│ INFY   +2.1%   │  │                │   │ ✅ ROE         │
│                │  │ Entry: ₹2987   │   │ ✅ Debt/Equity │
│ Top Losers 📉  │  │ Target:₹3210   │   │ ✅ Rev Growth  │
│ ONGC   -1.8%   │  │ SL:    ₹2850   │   │ ✅ Margins     │
└────────────────┘  └────────────────┘   └────────────────┘
```

---

## Tech Stack

- **Language**: Kotlin 2.1.0
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Clean Architecture + Repository Pattern
- **DI**: Hilt 2.54
- **Network**: Retrofit 2.11 + OkHttp 4.12 (TLS enforced, HTTPS only)
- **Database**: Room 2.6.1 (local watchlist + cache)
- **Preferences**: DataStore
- **Target**: Android 16 (API 36), minSdk: API 35 (Android 15+)

---

## Build Instructions

### Prerequisites
- **Android Studio Ladybug** (2024.2.1) or newer
- **JDK 17**
- Android SDK with API 36 installed

### Steps

```bash
# 1. Clone / download this project
cd QuantEdge

# 2. Open in Android Studio
#    File → Open → select the QuantEdge folder

# 3. Let Gradle sync complete (downloads ~200MB of dependencies)

# 4. Build debug APK
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# 5. Build release APK (unsigned)
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release-unsigned.apk

# 6. Install on connected device
./gradlew installDebug
```

### On Windows
```cmd
gradlew.bat assembleDebug
```

---

## Configuration

### News API (Optional but recommended)
For real-time news sentiment, get a **free GNews API key** at https://gnews.io
- Free tier: 100 requests/day
- Enter key in app: Settings → News API → Enter key → Save

### No other API keys needed
- Yahoo Finance API is used anonymously (no key)
- All analysis runs on-device

---

## Analysis Factors

### Technical (5 factors)
| Factor | What it measures |
|---|---|
| RSI | Overbought/oversold momentum (period 14) |
| MACD | Trend direction & strength (12/26/9) |
| Bollinger Bands | Volatility & mean reversion (20-period) |
| Moving Averages | SMA 20/50/200 crossovers + Golden/Death cross |
| Volume | Volume anomaly vs 20-day average |

### Fundamental (5 factors, uses 2–5 year data)
| Factor | What it measures |
|---|---|
| P/E Ratio | Sector-adjusted valuation (benchmarked per sector) |
| ROE | Return on equity (quality indicator) |
| Debt/Equity | Balance sheet health |
| Revenue Growth | YoY revenue trend |
| Profit Margins | Net margin trend |

### Market & Sentiment (4 factors)
| Factor | What it measures |
|---|---|
| News Sentiment | On-device NLP across recent 10 articles |
| 52-Week Position | Price position within annual range |
| Sector Momentum | Contextual sector strength |
| FII/DII Activity | Institutional flow context |

---

## Signal Calculation

```
1. Each enabled factor scores from -1.0 (very bearish) to +1.0 (very bullish)
2. Weighted average → normalized score
3. Swing mode: equal tech + fundamental weighting
   Equity mode: 65% fundamental + 35% technical
4. Risk multiplier applied (Low: ×0.8, Moderate: ×1.0, High: ×1.2)
5. Final score → Signal type:
   ≥ 0.60 → STRONG BUY
   ≥ 0.25 → BUY
   ≤ -0.60 → STRONG SELL
   ≤ -0.25 → SELL
   else   → HOLD
6. ATR-based price targets:
   Swing: 3× ATR target, 1.5× ATR stop-loss
   Equity: 6× ATR target, 1.5× ATR stop-loss
```

---

## Security Notes

- All network calls use **HTTPS/TLS only** (enforced via `network_security_config.xml`)
- No API keys hardcoded — keys stored in DataStore (encrypted preferences)
- No data sent to third parties (analysis is fully on-device)
- No root privilege required

---

## Disclaimer

> QuantEdge is for **informational and educational purposes only**.
> It does **not** constitute financial advice, investment advice, or any recommendation to buy or sell securities.
> Always consult a SEBI-registered financial advisor before making investment decisions.
> Past performance of signals does not guarantee future results.

---

## License

MIT License — free to use, modify, distribute with attribution.

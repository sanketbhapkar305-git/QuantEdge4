package com.quantedge.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val QuantEdgeDarkColorScheme = darkColorScheme(
    primary = QuantBlue,
    onPrimary = BackgroundDark,
    primaryContainer = QuantPurpleSubtle,
    onPrimaryContainer = QuantBlueLight,
    secondary = QuantGreen,
    onSecondary = BackgroundDark,
    secondaryContainer = QuantGreenSubtle,
    onSecondaryContainer = QuantGreen,
    tertiary = QuantPurple,
    onTertiary = TextPrimary,
    tertiaryContainer = QuantPurpleSubtle,
    onTertiaryContainer = QuantPurple,
    error = QuantRed,
    onError = TextPrimary,
    errorContainer = QuantRedSubtle,
    onErrorContainer = QuantRed,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark,
    outlineVariant = DividerDark,
    inverseSurface = TextPrimary,
    inverseOnSurface = BackgroundDark,
    inversePrimary = QuantBlueDark,
    surfaceTint = QuantBlue,
    scrim = BackgroundDark
)

@Composable
fun QuantEdgeTheme(content: @Composable () -> Unit) {
    val colorScheme = QuantEdgeDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BackgroundDark.toArgb()
            window.navigationBarColor = BackgroundDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = QuantEdgeTypography,
        content = content
    )
}

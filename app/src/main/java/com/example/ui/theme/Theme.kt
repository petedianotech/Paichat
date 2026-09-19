package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Solid background brush provider strictly avoiding gradients.
 */
val LocalThemeGradient = staticCompositionLocalOf<Brush> {
    Brush.verticalGradient(colors = listOf(DarkBackground, DarkBackground))
}

/**
 * Matching Light Theme:
 * - Background: #F6F8FC
 * - Cards/containers: #FFFFFF
 * - Primary blue: #1688F5
 * - Secondary blue: #E8F1FF
 * - Primary text: #111827
 * - Secondary text: #5F6B7A
 * - Borders/dividers: #D9E1EC
 * - Selected/active: #DCEBFF
 * - Avatar blue: #087FF0
 * - Disabled text: #9AA5B1
 */
private val LightColorScheme = lightColorScheme(
    primary = LightPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = LightSelectedActive,
    onPrimaryContainer = LightTextPrimary,
    secondary = LightSecondaryBlue,
    onSecondary = LightTextPrimary,
    secondaryContainer = LightSelectedActive,
    onSecondaryContainer = LightTextPrimary,
    tertiary = LightAvatarBlue,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightCardContainer,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSecondaryBlue,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorderDivider,
    outlineVariant = LightBorderDivider,
    error = SemanticError,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight
)

/**
 * Dark Theme:
 * - Background: #050B18
 * - Cards/containers: #0B1424
 * - Primary/active blue: #1688F5
 * - Secondary blue: #29466F
 * - Primary text: #F5F7FF
 * - Secondary text: #A9B9E0
 * - Borders/dividers: #263753
 * - Avatar blue: #087FF0
 */
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = DarkSecondaryBlue,
    onPrimaryContainer = DarkTextPrimary,
    secondary = DarkSecondaryBlue,
    onSecondary = DarkTextPrimary,
    secondaryContainer = DarkCardContainer,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = PaiAvatarBlue,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkCardContainer,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCardContainer,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorderDivider,
    outlineVariant = DarkBorderDivider,
    error = SemanticError,
    onError = Color.White,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark
)

/**
 * Pure AMOLED Dark Scheme:
 * Pitch black background with exact Dark palette cards (#0B1424) and borders (#263753).
 */
private val AmoledDarkColorScheme = darkColorScheme(
    primary = DarkPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = DarkSecondaryBlue,
    onPrimaryContainer = DarkTextPrimary,
    secondary = DarkSecondaryBlue,
    onSecondary = DarkTextPrimary,
    secondaryContainer = DarkCardContainer,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = PaiAvatarBlue,
    onTertiary = Color.White,
    background = Color(0xFF000000),
    onBackground = DarkTextPrimary,
    surface = DarkCardContainer,
    onSurface = DarkTextPrimary,
    surfaceVariant = Color(0xFF000000),
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorderDivider,
    outlineVariant = DarkBorderDivider,
    error = SemanticError,
    onError = Color.White,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark
)

@Composable
fun PaiChatTheme(
    themeMode: String = "SYSTEM",
    colorTheme: String = "BLUE",
    fontFamily: String = "DEFAULT",
    fontSize: String = "NORMAL",
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "LIGHT" -> false
        "DARK", "AMOLED" -> true
        else -> isSystemDark
    }
    val isAmoled = themeMode == "AMOLED"

    val baseScheme = if (isAmoled) AmoledDarkColorScheme else if (isDark) DarkColorScheme else LightColorScheme

    // Solid single-color brush strictly avoiding any gradient
    val solidBackgroundColor = if (isAmoled) Color(0xFF000000) else if (isDark) DarkBackground else LightBackground
    val solidBrush = Brush.verticalGradient(colors = listOf(solidBackgroundColor, solidBackgroundColor))

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = baseScheme.background.toArgb()
                window.navigationBarColor = baseScheme.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalThemeGradient provides solidBrush
    ) {
        MaterialTheme(
            colorScheme = baseScheme,
            typography = AppFonts.getDynamicTypography(fontFamily, fontSize),
            content = content
        )
    }
}

/**
 * Backward compatibility alias matching PaichatTheme (lowercase 'c').
 */
@Composable
fun PaichatTheme(
    themeMode: String = "SYSTEM",
    colorTheme: String = "BLUE",
    fontFamily: String = "DEFAULT",
    fontSize: String = "NORMAL",
    content: @Composable () -> Unit
) {
    PaiChatTheme(
        themeMode = themeMode,
        colorTheme = colorTheme,
        fontFamily = fontFamily,
        fontSize = fontSize,
        content = content
    )
}

/**
 * Template compatibility alias for MyApplicationTheme.
 */
@Composable
fun MyApplicationTheme(
    themeMode: String = "SYSTEM",
    colorTheme: String = "BLUE",
    fontFamily: String = "DEFAULT",
    fontSize: String = "NORMAL",
    content: @Composable () -> Unit
) {
    PaiChatTheme(
        themeMode = themeMode,
        colorTheme = colorTheme,
        fontFamily = fontFamily,
        fontSize = fontSize,
        content = content
    )
}

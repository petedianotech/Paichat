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
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalThemeGradient = staticCompositionLocalOf<Brush> {
    Brush.verticalGradient(colors = listOf(BackgroundDark, BackgroundDark))
}

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

private val AmoledDarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = Color(0xFF131A26),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = Color(0xFF161B22),
    onSecondaryContainer = Color(0xFFE2E8F0),
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = Color(0xFF3B0B0C),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF0E1217),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF262D37),
    outlineVariant = Color(0xFF181E27)
)

@Composable
fun PaiChatTheme(
    themeMode: String = "SYSTEM",
    colorTheme: String = "BLUE",
    fontFamily: String = "DEFAULT",
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

    val colorScheme = when (colorTheme) {
        "INDIGO" -> if (isDark) baseScheme.copy(
            primary = PrimaryIndigoDark,
            primaryContainer = if (isAmoled) Color(0xFF141738) else PrimaryIndigoContainerDark,
            onPrimaryContainer = OnPrimaryIndigoContainerDark
        ) else baseScheme.copy(
            primary = PrimaryIndigoLight,
            primaryContainer = PrimaryIndigoContainerLight,
            onPrimaryContainer = OnPrimaryIndigoContainerLight
        )
        "PURPLE" -> if (isDark) baseScheme.copy(
            primary = PrimaryPurpleDark,
            primaryContainer = if (isAmoled) Color(0xFF1F1238) else PrimaryPurpleContainerDark,
            onPrimaryContainer = OnPrimaryPurpleContainerDark
        ) else baseScheme.copy(
            primary = PrimaryPurpleLight,
            primaryContainer = PrimaryPurpleContainerLight,
            onPrimaryContainer = OnPrimaryPurpleContainerLight
        )
        "ROSE" -> if (isDark) baseScheme.copy(
            primary = PrimaryRoseDark,
            primaryContainer = if (isAmoled) Color(0xFF2E101B) else PrimaryRoseContainerDark,
            onPrimaryContainer = OnPrimaryRoseContainerDark
        ) else baseScheme.copy(
            primary = PrimaryRoseLight,
            primaryContainer = PrimaryRoseContainerLight,
            onPrimaryContainer = OnPrimaryRoseContainerLight
        )
        "TEAL" -> if (isDark) baseScheme.copy(
            primary = Color(0xFF2DD4BF),
            primaryContainer = if (isAmoled) Color(0xFF062826) else Color(0xFF134E4A),
            onPrimaryContainer = Color(0xFFCCFBF1)
        ) else baseScheme.copy(
            primary = Color(0xFF0D9488),
            primaryContainer = Color(0xFFCCFBF1),
            onPrimaryContainer = Color(0xFF115E59)
        )
        "AMBER" -> if (isDark) baseScheme.copy(
            primary = Color(0xFFFBBF24),
            primaryContainer = if (isAmoled) Color(0xFF332005) else Color(0xFF78350F),
            onPrimaryContainer = Color(0xFFFEF3C7)
        ) else baseScheme.copy(
            primary = Color(0xFFD97706),
            primaryContainer = Color(0xFFFEF3C7),
            onPrimaryContainer = Color(0xFF92400E)
        )
        else -> baseScheme
    }

    // Flat solid background brush for compatibility with components referencing LocalThemeGradient
    val solidColor = if (isAmoled) Color(0xFF000000) else if (isDark) BackgroundDark else BackgroundLight
    val gradientBrush = Brush.verticalGradient(colors = listOf(solidColor, solidColor))

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalThemeGradient provides gradientBrush
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppFonts.getDynamicTypography(fontFamily),
            content = content
        )
    }
}

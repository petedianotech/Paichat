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
    Brush.verticalGradient(colors = listOf(Color(0xFFF8F9FF), Color(0xFFF8F9FF)))
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
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight
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
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark
)

private val AmoledDarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = Color(0xFF151922),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = Color(0xFF1E2129),
    onSecondaryContainer = Color(0xFFDDE1EE),
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = Color(0xFF281C26),
    onTertiaryContainer = Color(0xFFFFD7F5),
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = Color(0xFF3B0B0C),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF12141A),
    onSurfaceVariant = Color(0xFFC4C6D0)
)

@Composable
fun PaiChatTheme(
    themeMode: String = "SYSTEM",
    colorTheme: String = "BLUE",
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
        "INDIGO" -> if (isDark) baseScheme.copy(primary = PrimaryIndigoDark, primaryContainer = if (isAmoled) Color(0xFF121528) else PrimaryIndigoContainerDark, onPrimaryContainer = OnPrimaryIndigoContainerDark)
                    else baseScheme.copy(primary = PrimaryIndigoLight, primaryContainer = PrimaryIndigoContainerLight, onPrimaryContainer = OnPrimaryIndigoContainerLight)
        "PURPLE" -> if (isDark) baseScheme.copy(primary = PrimaryPurpleDark, primaryContainer = if (isAmoled) Color(0xFF1D0E2B) else PrimaryPurpleContainerDark, onPrimaryContainer = OnPrimaryPurpleContainerDark)
                    else baseScheme.copy(primary = PrimaryPurpleLight, primaryContainer = PrimaryPurpleContainerLight, onPrimaryContainer = OnPrimaryPurpleContainerLight)
        "ROSE" -> if (isDark) baseScheme.copy(primary = PrimaryRoseDark, primaryContainer = if (isAmoled) Color(0xFF2B0E17) else PrimaryRoseContainerDark, onPrimaryContainer = OnPrimaryRoseContainerDark)
                  else baseScheme.copy(primary = PrimaryRoseLight, primaryContainer = PrimaryRoseContainerLight, onPrimaryContainer = OnPrimaryRoseContainerLight)
        "TEAL" -> if (isDark) baseScheme.copy(primary = Color(0xFF4DD0E1), primaryContainer = if (isAmoled) Color(0xFF07242B) else Color(0xFF004F58), onPrimaryContainer = Color(0xFFB1ECF5))
                  else baseScheme.copy(primary = Color(0xFF006874), primaryContainer = Color(0xFF97F0FF), onPrimaryContainer = Color(0xFF001F24))
        "AMBER" -> if (isDark) baseScheme.copy(primary = Color(0xFFFFB74D), primaryContainer = if (isAmoled) Color(0xFF2C1900) else Color(0xFF5A3600), onPrimaryContainer = Color(0xFFFFDDB3))
                   else baseScheme.copy(primary = Color(0xFF855300), primaryContainer = Color(0xFFFFDDB3), onPrimaryContainer = Color(0xFF2A1700))
        else -> baseScheme
    }

    val gradientBrush = if (isAmoled) {
        Brush.verticalGradient(colors = listOf(Color(0xFF000000), Color(0xFF000000)))
    } else {
        when (colorTheme) {
            "INDIGO" -> if (isDark) Brush.verticalGradient(colors = listOf(Color(0xFF0D1424), Color(0xFF060A13)))
                        else Brush.verticalGradient(colors = listOf(Color(0xFFEEF2FF), Color(0xFFFAFBFF)))
            "PURPLE" -> if (isDark) Brush.verticalGradient(colors = listOf(Color(0xFF130922), Color(0xFF0A0511)))
                        else Brush.verticalGradient(colors = listOf(Color(0xFFF4E9FF), Color(0xFFFBF6FF)))
            "ROSE" -> if (isDark) Brush.verticalGradient(colors = listOf(Color(0xFF1D0B12), Color(0xFF0D0408)))
                      else Brush.verticalGradient(colors = listOf(Color(0xFFFFF0F3), Color(0xFFFFF9FA)))
            "TEAL" -> if (isDark) Brush.verticalGradient(colors = listOf(Color(0xFF061A1E), Color(0xFF020C0E)))
                      else Brush.verticalGradient(colors = listOf(Color(0xFFE5FAFC), Color(0xFFF6FDFF)))
            "AMBER" -> if (isDark) Brush.verticalGradient(colors = listOf(Color(0xFF1D1204), Color(0xFF0D0802)))
                       else Brush.verticalGradient(colors = listOf(Color(0xFFFFF8EC), Color(0xFFFFFCF5)))
            else -> if (isDark) Brush.verticalGradient(colors = listOf(Color(0xFF081225), Color(0xFF040810)))
                    else Brush.verticalGradient(colors = listOf(Color(0xFFF0F5FF), Color(0xFFFAFCFF)))
        }
    }

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
            typography = Typography,
            content = content
        )
    }
}

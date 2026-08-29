package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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

@Composable
fun PaiChatTheme(
    themeMode: String = "SYSTEM",
    colorTheme: String = "BLUE",
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemDark
    }

    val baseScheme = if (isDark) DarkColorScheme else LightColorScheme

    val colorScheme = when (colorTheme) {
        "TEAL" -> if (isDark) baseScheme.copy(primary = PrimaryTealDark, primaryContainer = PrimaryTealContainerDark, onPrimaryContainer = OnPrimaryTealContainerDark)
                  else baseScheme.copy(primary = PrimaryTealLight, primaryContainer = PrimaryTealContainerLight, onPrimaryContainer = OnPrimaryTealContainerLight)
        "PURPLE" -> if (isDark) baseScheme.copy(primary = PrimaryPurpleDark, primaryContainer = PrimaryPurpleContainerDark, onPrimaryContainer = OnPrimaryPurpleContainerDark)
                    else baseScheme.copy(primary = PrimaryPurpleLight, primaryContainer = PrimaryPurpleContainerLight, onPrimaryContainer = OnPrimaryPurpleContainerLight)
        "EMERALD" -> if (isDark) baseScheme.copy(primary = PrimaryEmeraldDark, primaryContainer = PrimaryEmeraldContainerDark, onPrimaryContainer = OnPrimaryEmeraldContainerDark)
                     else baseScheme.copy(primary = PrimaryEmeraldLight, primaryContainer = PrimaryEmeraldContainerLight, onPrimaryContainer = OnPrimaryEmeraldContainerLight)
        else -> baseScheme
    }

    val gradientBrush = when (colorTheme) {
        "TEAL" -> if (isDark) {
            Brush.verticalGradient(colors = listOf(Color(0xFF041817), Color(0xFF0C0E0E)))
        } else {
            Brush.verticalGradient(colors = listOf(Color(0xFFE2F9F8), Color(0xFFF3FEFE)))
        }
        "PURPLE" -> if (isDark) {
            Brush.verticalGradient(colors = listOf(Color(0xFF130922), Color(0xFF0A0511)))
        } else {
            Brush.verticalGradient(colors = listOf(Color(0xFFF4E9FF), Color(0xFFFBF6FF)))
        }
        "EMERALD" -> if (isDark) {
            Brush.verticalGradient(colors = listOf(Color(0xFF06180E), Color(0xFF030C07)))
        } else {
            Brush.verticalGradient(colors = listOf(Color(0xFFE8F8EE), Color(0xFFF4FBF7)))
        }
        else -> if (isDark) { // BLUE
            Brush.verticalGradient(colors = listOf(Color(0xFF081225), Color(0xFF040810)))
        } else {
            Brush.verticalGradient(colors = listOf(Color(0xFFF0F5FF), Color(0xFFFAFCFF)))
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

@Composable
fun PulseChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    PaiChatTheme(
        themeMode = if (darkTheme) "DARK" else "LIGHT",
        colorTheme = "BLUE",
        content = content
    )
}

package com.example.ui.theme

import android.graphics.Typeface as AndroidTypeface
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily

object AppFonts {
    val fontsList = listOf(
        FontOption("DEFAULT", "System Default"),
        FontOption("SERIF", "Classic Serif"),
        FontOption("MONOSPACE", "Modern Monospace"),
        FontOption("ROUNDED", "Elegant Rounded"),
        FontOption("CONDENSED", "Condensed Bold"),
        FontOption("CASUAL", "Casual Type"),
        FontOption("CURSIVE", "Creative Cursive"),
        FontOption("THIN", "Minimalist Thin"),
        FontOption("LIGHT", "Sophisticated Light"),
        FontOption("MEDIUM", "Bold Medium"),
        FontOption("BLACK", "Ultra Black")
    )

    fun getFontFamily(key: String): FontFamily {
        return when (key.uppercase()) {
            "SERIF" -> FontFamily.Serif
            "MONOSPACE" -> FontFamily.Monospace
            "ROUNDED" -> try { FontFamily(AndroidTypeface.create("sans-serif-rounded", AndroidTypeface.NORMAL)) } catch (_: Exception) { FontFamily.Default }
            "CONDENSED" -> try { FontFamily(AndroidTypeface.create("sans-serif-condensed", AndroidTypeface.NORMAL)) } catch (_: Exception) { FontFamily.Default }
            "CASUAL" -> try { FontFamily(AndroidTypeface.create("casual", AndroidTypeface.NORMAL)) } catch (_: Exception) { FontFamily.Default }
            "CURSIVE" -> try { FontFamily(AndroidTypeface.create("cursive", AndroidTypeface.NORMAL)) } catch (_: Exception) { FontFamily.Default }
            "THIN" -> try { FontFamily(AndroidTypeface.create("sans-serif-thin", AndroidTypeface.NORMAL)) } catch (_: Exception) { FontFamily.Default }
            "LIGHT" -> try { FontFamily(AndroidTypeface.create("sans-serif-light", AndroidTypeface.NORMAL)) } catch (_: Exception) { FontFamily.Default }
            "MEDIUM" -> try { FontFamily(AndroidTypeface.create("sans-serif-medium", AndroidTypeface.NORMAL)) } catch (_: Exception) { FontFamily.Default }
            "BLACK" -> try { FontFamily(AndroidTypeface.create("sans-serif-black", AndroidTypeface.NORMAL)) } catch (_: Exception) { FontFamily.Default }
            else -> FontFamily.Default
        }
    }

    fun getDynamicTypography(key: String): Typography {
        val fontFamily = getFontFamily(key)
        val base = Typography
        return Typography(
            displayLarge = base.displayLarge.copy(fontFamily = fontFamily),
            displayMedium = base.displayMedium.copy(fontFamily = fontFamily),
            displaySmall = base.displaySmall.copy(fontFamily = fontFamily),
            headlineLarge = base.headlineLarge.copy(fontFamily = fontFamily),
            headlineMedium = base.headlineMedium.copy(fontFamily = fontFamily),
            headlineSmall = base.headlineSmall.copy(fontFamily = fontFamily),
            titleLarge = base.titleLarge.copy(fontFamily = fontFamily),
            titleMedium = base.titleMedium.copy(fontFamily = fontFamily),
            titleSmall = base.titleSmall.copy(fontFamily = fontFamily),
            bodyLarge = base.bodyLarge.copy(fontFamily = fontFamily),
            bodyMedium = base.bodyMedium.copy(fontFamily = fontFamily),
            bodySmall = base.bodySmall.copy(fontFamily = fontFamily),
            labelLarge = base.labelLarge.copy(fontFamily = fontFamily),
            labelMedium = base.labelMedium.copy(fontFamily = fontFamily),
            labelSmall = base.labelSmall.copy(fontFamily = fontFamily)
        )
    }
}

data class FontOption(val key: String, val displayName: String)

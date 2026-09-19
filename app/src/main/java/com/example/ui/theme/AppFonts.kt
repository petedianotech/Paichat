package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.R

object AppFonts {
    private val fontProvider = GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs
    )

    private val interFont = GoogleFont("Inter")
    val InterFontFamily = FontFamily(
        Font(googleFont = interFont, fontProvider = fontProvider, weight = FontWeight.Normal),
        Font(googleFont = interFont, fontProvider = fontProvider, weight = FontWeight.Medium),
        Font(googleFont = interFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
        Font(googleFont = interFont, fontProvider = fontProvider, weight = FontWeight.Bold)
    )

    private val robotoFlexFont = GoogleFont("Roboto Flex")
    val RobotoFlexFontFamily = FontFamily(
        Font(googleFont = robotoFlexFont, fontProvider = fontProvider, weight = FontWeight.Normal),
        Font(googleFont = robotoFlexFont, fontProvider = fontProvider, weight = FontWeight.Medium),
        Font(googleFont = robotoFlexFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
        Font(googleFont = robotoFlexFont, fontProvider = fontProvider, weight = FontWeight.Bold)
    )

    private val manropeFont = GoogleFont("Manrope")
    val ManropeFontFamily = FontFamily(
        Font(googleFont = manropeFont, fontProvider = fontProvider, weight = FontWeight.Normal),
        Font(googleFont = manropeFont, fontProvider = fontProvider, weight = FontWeight.Medium),
        Font(googleFont = manropeFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
        Font(googleFont = manropeFont, fontProvider = fontProvider, weight = FontWeight.Bold)
    )

    private val plusJakartaFont = GoogleFont("Plus Jakarta Sans")
    val PlusJakartaFontFamily = FontFamily(
        Font(googleFont = plusJakartaFont, fontProvider = fontProvider, weight = FontWeight.Normal),
        Font(googleFont = plusJakartaFont, fontProvider = fontProvider, weight = FontWeight.Medium),
        Font(googleFont = plusJakartaFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
        Font(googleFont = plusJakartaFont, fontProvider = fontProvider, weight = FontWeight.Bold)
    )

    private val dmSansFont = GoogleFont("DM Sans")
    val DmSansFontFamily = FontFamily(
        Font(googleFont = dmSansFont, fontProvider = fontProvider, weight = FontWeight.Normal),
        Font(googleFont = dmSansFont, fontProvider = fontProvider, weight = FontWeight.Medium),
        Font(googleFont = dmSansFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
        Font(googleFont = dmSansFont, fontProvider = fontProvider, weight = FontWeight.Bold)
    )

    val fontsList = listOf(
        FontOption("DEFAULT", "Inter (Default)"),
        FontOption("ROBOTO_FLEX", "Roboto Flex"),
        FontOption("MANROPE", "Manrope"),
        FontOption("PLUS_JAKARTA", "Plus Jakarta Sans"),
        FontOption("DM_SANS", "DM Sans"),
        FontOption("SERIF", "Serif"),
        FontOption("MONOSPACE", "Monospace"),
        FontOption("SANS_SERIF", "Sans-Serif")
    )

    fun getFontFamily(key: String): FontFamily {
        return when (key.uppercase()) {
            "ROBOTO_FLEX" -> RobotoFlexFontFamily
            "MANROPE" -> ManropeFontFamily
            "PLUS_JAKARTA" -> PlusJakartaFontFamily
            "DM_SANS" -> DmSansFontFamily
            "SERIF" -> FontFamily.Serif
            "MONOSPACE" -> FontFamily.Monospace
            "SANS_SERIF" -> FontFamily.SansSerif
            "DEFAULT", "INTER" -> InterFontFamily
            else -> InterFontFamily
        }
    }

    fun getFontSizeScale(sizeKey: String): androidx.compose.ui.unit.TextUnit? {
        return null
    }

    fun getScaleFactor(sizeKey: String): Float {
        return when (sizeKey.uppercase()) {
            "SMALL" -> 0.85f
            "LARGE" -> 1.15f
            "EXTRA_LARGE" -> 1.30f
            else -> 1.0f // "NORMAL"
        }
    }

    fun getDynamicTypography(fontKey: String = "DEFAULT", fontSizeKey: String = "NORMAL"): Typography {
        val fontFamily = getFontFamily(fontKey)
        val scale = getScaleFactor(fontSizeKey)
        val base = Typography

        fun scaleStyle(style: TextStyle): TextStyle {
            return style.copy(
                fontFamily = fontFamily,
                fontSize = (style.fontSize.value * scale).sp,
                lineHeight = (style.lineHeight.value * scale).sp
            )
        }

        return Typography(
            displayLarge = scaleStyle(base.displayLarge),
            displayMedium = scaleStyle(base.displayMedium),
            displaySmall = scaleStyle(base.displaySmall),
            headlineLarge = scaleStyle(base.headlineLarge),
            headlineMedium = scaleStyle(base.headlineMedium),
            headlineSmall = scaleStyle(base.headlineSmall),
            titleLarge = scaleStyle(base.titleLarge),
            titleMedium = scaleStyle(base.titleMedium),
            titleSmall = scaleStyle(base.titleSmall),
            bodyLarge = scaleStyle(base.bodyLarge),
            bodyMedium = scaleStyle(base.bodyMedium),
            bodySmall = scaleStyle(base.bodySmall),
            labelLarge = scaleStyle(base.labelLarge),
            labelMedium = scaleStyle(base.labelMedium),
            labelSmall = scaleStyle(base.labelSmall)
        )
    }
}

data class FontOption(val key: String, val displayName: String)

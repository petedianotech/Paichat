package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
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
        FontOption("INTER", "Inter (Default)"),
        FontOption("ROBOTO_FLEX", "Roboto Flex"),
        FontOption("MANROPE", "Manrope"),
        FontOption("PLUS_JAKARTA", "Plus Jakarta Sans"),
        FontOption("DM_SANS", "DM Sans")
    )

    fun getFontFamily(key: String): FontFamily {
        return when (key.uppercase()) {
            "ROBOTO_FLEX" -> RobotoFlexFontFamily
            "MANROPE" -> ManropeFontFamily
            "PLUS_JAKARTA" -> PlusJakartaFontFamily
            "DM_SANS" -> DmSansFontFamily
            else -> InterFontFamily
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

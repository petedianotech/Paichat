package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// PaiChat Refined Color System
// Strictly Human-Coded Flat Design (No Gradients)
// ==========================================

// --- CORE BRAND ACCENTS ---
val PaiPrimaryBlue = Color(0xFF1688F5) // Primary / active blue (#1688F5)
val PaiAvatarBlue = Color(0xFF087FF0)  // Avatar blue (#087FF0)

// --- DARK THEME PALETTE ---
// Background: #050B18
// Cards/containers: #0B1424
// Primary/active blue: #1688F5
// Secondary blue: #29466F
// Primary text: #F5F7FF
// Secondary text: #A9B9E0
// Borders/dividers: #263753
// Avatar blue: #087FF0
val DarkBackground = Color(0xFF050B18)
val DarkCardContainer = Color(0xFF0B1424)
val DarkPrimaryBlue = Color(0xFF1688F5)
val DarkSecondaryBlue = Color(0xFF29466F)
val DarkTextPrimary = Color(0xFFF5F7FF)
val DarkTextSecondary = Color(0xFFA9B9E0)
val DarkBorderDivider = Color(0xFF263753)

// Semantic aliases for Dark Theme
val DarkSurface = DarkCardContainer
val DarkSurfaceElevated = DarkCardContainer
val DarkDivider = DarkBorderDivider
val DarkBorder = DarkBorderDivider

// --- LIGHT THEME PALETTE ---
// Background: #F6F8FC
// Cards/containers: #FFFFFF
// Primary blue: #1688F5
// Secondary blue: #E8F1FF
// Primary text: #111827
// Secondary text: #5F6B7A
// Borders/dividers: #D9E1EC
// Selected/active: #DCEBFF
// Avatar blue: #087FF0
// Disabled text: #9AA5B1
val LightBackground = Color(0xFFF6F8FC)
val LightCardContainer = Color(0xFFFFFFFF)
val LightPrimaryBlue = Color(0xFF1688F5)
val LightSecondaryBlue = Color(0xFFE8F1FF)
val LightTextPrimary = Color(0xFF111827)
val LightTextSecondary = Color(0xFF5F6B7A)
val LightBorderDivider = Color(0xFFD9E1EC)
val LightSelectedActive = Color(0xFFDCEBFF)
val LightAvatarBlue = Color(0xFF087FF0)
val LightDisabledText = Color(0xFF9AA5B1)

// Semantic aliases for Light Theme
val LightSurface = LightCardContainer
val LightSurfaceElevated = LightCardContainer
val LightDivider = LightBorderDivider
val LightBorder = LightBorderDivider

// --- STATUS & SEMANTIC TOKENS ---
val SemanticSuccess = Color(0xFF10B981)
val SemanticWarning = Color(0xFFF59E0B)
val SemanticError = Color(0xFFEF4444)
val SemanticDelivered = Color(0xFF1688F5)
val SemanticSent = Color(0xFF9AA5B1)
val SemanticPending = Color(0xFF64748B)

// --- MESSAGE BUBBLE PALETTE ---
val BubbleSentDark = DarkPrimaryBlue
val BubbleSentTextDark = Color(0xFFFFFFFF)
val BubbleReceivedDark = DarkCardContainer
val BubbleReceivedTextDark = DarkTextPrimary

val BubbleSentLight = LightPrimaryBlue
val BubbleSentTextLight = Color(0xFFFFFFFF)
val BubbleReceivedLight = LightCardContainer
val BubbleReceivedTextLight = LightTextPrimary

// --- DIRECT COMPATIBILITY ALIASES ---
val BrandBlue = PaiPrimaryBlue
val BrandBlueDark = DarkPrimaryBlue
val AvatarBlue = PaiAvatarBlue
val PrimaryLight = LightPrimaryBlue
val PrimaryIndigoLight = Color(0xFF4F46E5)
val PrimaryPurpleLight = Color(0xFF7C3AED)
val PrimaryRoseLight = Color(0xFFE11D48)
val OnPrimaryLight = Color.White
val PrimaryContainerLight = LightSelectedActive
val OnPrimaryContainerLight = LightTextPrimary

val PrimaryDark = DarkPrimaryBlue
val OnPrimaryDark = Color.White
val PrimaryContainerDark = DarkSecondaryBlue
val OnPrimaryContainerDark = DarkTextPrimary

val BackgroundLight = LightBackground
val OnBackgroundLight = LightTextPrimary
val SurfaceLight = LightSurface
val OnSurfaceLight = LightTextPrimary
val SurfaceVariantLight = LightCardContainer
val OnSurfaceVariantLight = LightTextSecondary
val OutlineLight = LightBorderDivider
val OutlineVariantLight = LightBorderDivider

val BackgroundDark = DarkBackground
val OnBackgroundDark = DarkTextPrimary
val SurfaceDark = DarkSurface
val OnSurfaceDark = DarkTextPrimary
val SurfaceVariantDark = DarkCardContainer
val OnSurfaceVariantDark = DarkTextSecondary
val OutlineDark = DarkBorderDivider
val OutlineVariantDark = DarkBorderDivider

val SecondaryLight = LightSecondaryBlue
val OnSecondaryLight = LightTextPrimary
val SecondaryContainerLight = LightSelectedActive
val OnSecondaryContainerLight = LightTextPrimary

val SecondaryDark = DarkSecondaryBlue
val OnSecondaryDark = DarkTextPrimary
val SecondaryContainerDark = DarkCardContainer
val OnSecondaryContainerDark = DarkTextPrimary

val ErrorLight = SemanticError
val OnErrorLight = Color.White
val ErrorContainerLight = Color(0xFFFEE2E2)
val OnErrorContainerLight = Color(0xFF991B1B)

val ErrorDark = SemanticError
val OnErrorDark = Color.White
val ErrorContainerDark = Color(0xFF450A0A)
val OnErrorContainerDark = Color(0xFFFCA5A5)
